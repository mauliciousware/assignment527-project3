/*
 * Copyright (C) 2008-2021 The Android Open Source Project,
 * Sean J. Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.craxiom.networksurvey.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.craxiom.networksurvey.Application
import com.craxiom.networksurvey.R
import com.craxiom.networksurvey.databinding.GpsSkyBinding
import com.craxiom.networksurvey.databinding.GpsSkyLegendCardBinding
import com.craxiom.networksurvey.databinding.GpsSkySignalMeterBinding
import com.craxiom.networksurvey.model.SatelliteStatus
import com.craxiom.networksurvey.ui.gnss.Filter
import com.craxiom.networksurvey.ui.gnss.data.FixState
import com.craxiom.networksurvey.ui.gnss.data.LocationRepository
import com.craxiom.networksurvey.ui.gnss.model.SatelliteMetadata
import com.craxiom.networksurvey.ui.gnss.model.SignalInfoViewModel
import com.craxiom.networksurvey.ui.theme.NsTheme
import com.craxiom.networksurvey.util.LibUIUtils
import com.craxiom.networksurvey.util.MathUtils
import com.craxiom.networksurvey.util.PreferenceUtils.clearGnssFilter
import com.craxiom.networksurvey.util.PreferenceUtils.gnssFilter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class SkyFragment : Fragment() {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val viewModel: SignalInfoViewModel by activityViewModels()

    // Binding variables
    private var _binding: GpsSkyBinding? = null
    private val binding get() = _binding
    private lateinit var legendLines: List<View>
    private lateinit var legendShapes: List<ImageView>
    private lateinit var meter: GpsSkySignalMeterBinding
    private lateinit var legend: GpsSkyLegendCardBinding

    // Animations for cn0 indicators
    private var cn0InViewAvgAnimation: Animation? = null
    var cn0UsedAvgAnimation: Animation? = null
    var cn0InViewAvgAnimationTextView: Animation? = null
    var cn0UsedAvgAnimationTextView: Animation? = null

    // Default light theme values
    private var usedCn0Background = R.drawable.cn0_round_corner_background_used_dark
    private var usedCn0IndicatorColor = Color.BLACK

    // Repository of location data that the service will observe, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository

    // Get a reference to the Job from the Flow so we can stop it from UI events
    private var gnssFlow: Job? = null
    private var sensorFlow: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GpsSkyBinding.inflate(inflater, container, false)
        val v = binding!!.root
        meter = binding!!.skyCn0IndicatorCard.gpsSkySignalMeter
        legend = binding!!.skyLegendCard

        initFilterView(viewModel)

        initLegendViews()

        observeLocationUpdateStates()

        return v
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onResume() {
        super.onResume()

        val color: Int =
            ContextCompat.getColor(requireContext(), android.R.color.secondary_text_dark)
        legend.skyLegendUsedInFix.setImageResource(R.drawable.circle_used_in_fix_dark)
        usedCn0Background = R.drawable.cn0_round_corner_background_used_dark
        usedCn0IndicatorColor = resources.getColor(android.R.color.darker_gray, null)

        for (v in legendLines) {
            v.setBackgroundColor(color)
        }
        for (v in legendShapes) {
            v.setColorFilter(color)
        }

        onGnssStarted()
    }

    override fun onPause() {
        onGnssStopped()

        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationUpdateStates() {
        repository.receivingLocationUpdates
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                when (it) {
                    true -> onGnssStarted()
                    false -> onGnssStopped()
                }
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeGnssStatus() {
        val gnssStatusObserver = Observer<List<SatelliteStatus>> { statuses ->
            updateGnssStatus(statuses)
        }

        viewModel.filteredStatuses.observe(
            viewLifecycleOwner, gnssStatusObserver
        )
    }

    private fun observeGnssStates() {
        repository.fixState
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                when (it) {
                    is FixState.Acquired -> onGnssFixAcquired()
                    is FixState.NotAcquired -> onGnssFixLost()
                }
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeSensorFlow() {
        if (sensorFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        sensorFlow = repository.getSensorUpdates()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(TAG, "Sky sensor: orientation ${it[0]}, tilt ${it[1]}")
                onOrientationChanged(it.values[0], it.values[1])
            }
            .launchIn(lifecycleScope)
    }

    private fun onGnssFixAcquired() {
        showHaveFix()
    }

    private fun onGnssFixLost() {
        showLostFix()
    }

    private fun updateGnssStatus(statuses: List<SatelliteStatus>) {
        binding?.skyView?.setStatus(statuses)
        updateCn0AvgMeterText()
        updateCn0Avgs()
    }

    @ExperimentalCoroutinesApi
    private fun onGnssStarted() {
        binding?.skyView?.setStarted()
        // Activity or service is observing updates, so observe here too
        observeGnssStatus()
        observeGnssStates()
        observeSensorFlow()
    }

    private fun onGnssStopped() {
        // Cancel updates (Note that these are canceled via trackingListener preference listener
        // in the case where updates are stopped from the Activity UI switch).
        sensorFlow?.cancel()
        gnssFlow?.cancel()

        binding?.skyView?.setStopped()
        binding?.skyLock?.visibility = View.GONE
    }

    private fun onOrientationChanged(orientation: Double, tilt: Double) {
        // For performance reasons, only proceed if this fragment is visible
        // TODO - this is now deprecated and a no-op, update to newest code
        if (!userVisibleHint) {
            return
        }
        binding?.skyView?.onOrientationChanged(orientation, tilt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initFilterView(viewModel: SignalInfoViewModel) {
        binding!!.filterView.apply {
            // Dispose the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NsTheme {
                    val allStatuses: List<SatelliteStatus> by viewModel.allStatuses.observeAsState(
                        emptyList()
                    )
                    val satelliteMetadata: SatelliteMetadata by viewModel.filteredSatelliteMetadata.observeAsState(
                        SatelliteMetadata()
                    )
                    // Order of arguments seems to matter in below IF statement - it doesn't seem
                    // to recompose if gnssFilter().isNotEmpty() is first
                    if (allStatuses.isNotEmpty() && gnssFilter(
                            Application.get(),
                            Application.getPrefs()
                        ).isNotEmpty()
                    ) {
                        Filter(
                            allStatuses.size,
                            satelliteMetadata
                        ) { clearGnssFilter(Application.get(), Application.getPrefs()) }
                    }
                }
            }
            id = R.id.filter_view
        }
    }

    /**
     * Initialize the views in the C/N0 and Shape legends
     */
    private fun initLegendViews() {
        // Avg C/N0 indicator lines
        val cn0 = meter.signalMeterTicksAndText
        legendLines = listOf(
            cn0.skyLegendCn0LeftLine4,
            cn0.skyLegendCn0LeftLine3,
            cn0.skyLegendCn0LeftLine2,
            cn0.skyLegendCn0LeftLine1,
            cn0.skyLegendCn0CenterLine,
            cn0.skyLegendCn0RightLine1,
            cn0.skyLegendCn0RightLine2,
            cn0.skyLegendCn0RightLine3,
            cn0.skyLegendCn0RightLine4,
            legend.skyLegendShapeLine1a,
            legend.skyLegendShapeLine1b,
            legend.skyLegendShapeLine2a,
            legend.skyLegendShapeLine2b,
            legend.skyLegendShapeLine3a,
            legend.skyLegendShapeLine3b,
            legend.skyLegendShapeLine4a,
            legend.skyLegendShapeLine4b,
            legend.skyLegendShapeLine5a,
            legend.skyLegendShapeLine5b,
            legend.skyLegendShapeLine6a,
            legend.skyLegendShapeLine6b,
            legend.skyLegendShapeLine7a,
            legend.skyLegendShapeLine7b,
            legend.skyLegendShapeLine8a,
            legend.skyLegendShapeLine8b,
            legend.skyLegendShapeLine9a,
            legend.skyLegendShapeLine9b,
            legend.skyLegendShapeLine10a,
            legend.skyLegendShapeLine10b,
            legend.skyLegendShapeLine11a,
            legend.skyLegendShapeLine12a,
            legend.skyLegendShapeLine13a,
            legend.skyLegendShapeLine14a,
            legend.skyLegendShapeLine14b,
            legend.skyLegendShapeLine15a,
            legend.skyLegendShapeLine15b,
            legend.skyLegendShapeLine16a,
            legend.skyLegendShapeLine16b,
            legend.skyLegendShapeLine17a,
            legend.skyLegendShapeLine17b
        )

        // Shape Legend shapes
        legendShapes = listOf(
            legend.skyLegendCircle,
            legend.skyLegendSquare,
            legend.skyLegendPentagon,
            legend.skyLegendTriangle,
            legend.skyLegendHexagon1,
            legend.skyLegendOval,
            legend.skyLegendDiamond1,
            legend.skyLegendDiamond2,
            legend.skyLegendDiamond3,
            legend.skyLegendDiamond4,
            legend.skyLegendDiamond5,
            legend.skyLegendDiamond6,
            legend.skyLegendDiamond7,
            legend.skyLegendDiamond8
        )
    }

    private fun updateCn0AvgMeterText() {
        binding?.skyCn0IndicatorCard?.gpsSkySignalTitle?.apply {
            skyLegendCn0Title.setText(R.string.gps_cn0_column_label)
            skyLegendCn0Units.setText(R.string.sky_legend_cn0_units)
        }
        meter.signalMeterTicksAndText.apply {
            skyLegendCn0LeftText.setText(R.string.sky_legend_cn0_low)
            skyLegendCn0LeftCenterText.setText(R.string.sky_legend_cn0_low_middle)
            skyLegendCn0CenterText.setText(R.string.sky_legend_cn0_middle)
            skyLegendCn0RightCenterText.setText(R.string.sky_legend_cn0_middle_high)
            skyLegendCn0RightText.setText(R.string.sky_legend_cn0_high)
        }
    }

    private fun updateCn0Avgs() {
        if (binding == null) {
            return
        }
        // Based on the avg C/N0 for "in view" and "used" satellites the left margins need to be adjusted accordingly
        val meterWidthPx = (Application.get().resources.getDimension(R.dimen.cn0_meter_width)
            .toInt()
                - LibUIUtils.dpToPixels(Application.get(), 7.0f)) // Reduce width for padding
        val minIndicatorMarginPx = Application.get().resources
            .getDimension(R.dimen.cn0_indicator_min_left_margin).toInt()
        val maxIndicatorMarginPx = meterWidthPx + minIndicatorMarginPx
        val minTextViewMarginPx = Application.get().resources
            .getDimension(R.dimen.cn0_textview_min_left_margin).toInt()
        val maxTextViewMarginPx = meterWidthPx + minTextViewMarginPx

        // When both "in view" and "used" indicators and TextViews are shown, slide the "in view" TextView by this amount to the left to avoid overlap
        val TEXTVIEW_NON_OVERLAP_OFFSET_DP = -14.0f

        // Calculate normal offsets for avg in view satellite C/N0 value TextViews
        var leftInViewTextViewMarginPx: Int? = null
        if (MathUtils.isValidFloat(binding!!.skyView.cn0InViewAvg)) {
            leftInViewTextViewMarginPx = LibUIUtils.cn0ToTextViewLeftMarginPx(
                binding!!.skyView.cn0InViewAvg,
                minTextViewMarginPx, maxTextViewMarginPx
            )
        }

        // Calculate normal offsets for avg used satellite C/N0 value TextViews
        var leftUsedTextViewMarginPx: Int? = null
        if (MathUtils.isValidFloat(binding!!.skyView.cn0UsedAvg)) {
            leftUsedTextViewMarginPx = LibUIUtils.cn0ToTextViewLeftMarginPx(
                binding!!.skyView.cn0UsedAvg,
                minTextViewMarginPx, maxTextViewMarginPx
            )
        }

        // See if we need to apply the offset margin to try and keep the two TextViews from overlapping by shifting one of the two left
        if (leftInViewTextViewMarginPx != null && leftUsedTextViewMarginPx != null) {
            val offset = LibUIUtils.dpToPixels(Application.get(), TEXTVIEW_NON_OVERLAP_OFFSET_DP)
            if (leftInViewTextViewMarginPx <= leftUsedTextViewMarginPx) {
                leftInViewTextViewMarginPx += offset
            } else {
                leftUsedTextViewMarginPx += offset
            }
        }

        // Define paddings used for TextViews
        val pSides = LibUIUtils.dpToPixels(Application.get(), 7f)
        val pTopBottom = LibUIUtils.dpToPixels(Application.get(), 4f)

        // Set avg C/N0 of satellites in view of device
        if (MathUtils.isValidFloat(binding!!.skyView.cn0InViewAvg)) {
            meter.cn0TextInView.cn0TextInView.text =
                String.format("%.1f", binding!!.skyView.cn0InViewAvg)

            // Set color of TextView
            val color = binding!!.skyView.getSatelliteColor(binding!!.skyView.cn0InViewAvg)
            val background = ContextCompat.getDrawable(
                Application.get(),
                R.drawable.cn0_round_corner_background_in_view
            ) as LayerDrawable?

            // Fill
            val backgroundGradient =
                background!!.findDrawableByLayerId(R.id.cn0_avg_in_view_fill) as GradientDrawable
            backgroundGradient.setColor(color)

            // Stroke
            val borderGradient =
                background.findDrawableByLayerId(R.id.cn0_avg_in_view_border) as GradientDrawable
            borderGradient.setColor(color)
            meter.cn0TextInView.cn0TextInView.background = background

            // Set padding
            meter.cn0TextInView.cn0TextInView.setPadding(pSides, pTopBottom, pSides, pTopBottom)

            // Set color of indicator
            meter.cn0IndicatorInView.setColorFilter(color)

            // Set position and visibility of TextView
            if (meter.cn0TextInView.cn0TextInView.visibility == View.VISIBLE) {
                animateCn0Indicator(
                    meter.cn0TextInView.cn0TextInView,
                    leftInViewTextViewMarginPx!!,
                    cn0InViewAvgAnimationTextView
                )
            } else {
                LibUIUtils.updateLeftMarginPx(
                    leftInViewTextViewMarginPx!!,
                    meter.cn0TextInView.cn0TextInView
                )
                meter.cn0TextInView.cn0TextInView.visibility = View.VISIBLE
            }

            // Set position and visibility of indicator
            val leftIndicatorMarginPx = LibUIUtils.cn0ToIndicatorLeftMarginPx(
                binding!!.skyView.cn0InViewAvg,
                minIndicatorMarginPx, maxIndicatorMarginPx
            )

            // If the view is already visible, animate to the new position.  Otherwise just set the position and make it visible
            if (meter.cn0IndicatorInView.visibility == View.VISIBLE) {
                animateCn0Indicator(
                    meter.cn0IndicatorInView,
                    leftIndicatorMarginPx,
                    cn0InViewAvgAnimation
                )
            } else {
                LibUIUtils.updateLeftMarginPx(leftIndicatorMarginPx, meter.cn0IndicatorInView)
                meter.cn0IndicatorInView.visibility = View.VISIBLE
            }
        } else {
            meter.cn0TextInView.cn0TextInView.text = ""
            meter.cn0TextInView.cn0TextInView.visibility = View.INVISIBLE
            meter.cn0IndicatorInView.visibility = View.INVISIBLE
        }

        // Set avg C/N0 of satellites used in fix
        if (MathUtils.isValidFloat(binding!!.skyView.cn0UsedAvg)) {
            meter.cn0TextUsed.cn0TextUsed.text = String.format("%.1f", binding!!.skyView.cn0UsedAvg)
            // Set color of TextView
            val color = binding!!.skyView.getSatelliteColor(binding!!.skyView.cn0UsedAvg)
            val background =
                ContextCompat.getDrawable(Application.get(), usedCn0Background) as LayerDrawable?

            // Fill
            val backgroundGradient =
                background!!.findDrawableByLayerId(R.id.cn0_avg_used_fill) as GradientDrawable
            backgroundGradient.setColor(color)
            meter.cn0TextUsed.cn0TextUsed.background = background

            // Set padding
            meter.cn0TextUsed.cn0TextUsed.setPadding(pSides, pTopBottom, pSides, pTopBottom)

            // Set color of indicator
            meter.cn0IndicatorUsed.setColorFilter(usedCn0IndicatorColor)

            // Set position and visibility of TextView
            if (meter.cn0TextUsed.cn0TextUsed.visibility == View.VISIBLE) {
                animateCn0Indicator(
                    meter.cn0TextUsed.cn0TextUsed,
                    leftUsedTextViewMarginPx!!,
                    cn0UsedAvgAnimationTextView
                )
            } else {
                LibUIUtils.updateLeftMarginPx(
                    leftUsedTextViewMarginPx!!,
                    meter.cn0TextUsed.cn0TextUsed
                )
                meter.cn0TextUsed.cn0TextUsed.visibility = View.VISIBLE
            }

            // Set position and visibility of indicator
            val leftMarginPx = LibUIUtils.cn0ToIndicatorLeftMarginPx(
                binding!!.skyView.cn0UsedAvg,
                minIndicatorMarginPx, maxIndicatorMarginPx
            )

            // If the view is already visible, animate to the new position.  Otherwise just set the position and make it visible
            if (meter.cn0IndicatorUsed.visibility == View.VISIBLE) {
                animateCn0Indicator(meter.cn0IndicatorUsed, leftMarginPx, cn0UsedAvgAnimation)
            } else {
                LibUIUtils.updateLeftMarginPx(leftMarginPx, meter.cn0IndicatorUsed)
                meter.cn0IndicatorUsed.visibility = View.VISIBLE
            }
        } else {
            meter.cn0TextUsed.cn0TextUsed.text = ""
            meter.cn0TextUsed.cn0TextUsed.visibility = View.INVISIBLE
            meter.cn0IndicatorUsed.visibility = View.INVISIBLE
        }
    }

    /**
     * Animates a C/N0 indicator view from it's current location to the provided left margin location (in pixels)
     * @param v view to animate
     * @param goalLeftMarginPx the new left margin for the view that the view should animate to in pixels
     * @param animation Animation to use for the animation
     */
    private fun animateCn0Indicator(v: View?, goalLeftMarginPx: Int, animation: Animation?) {
        if (v == null) {
            return
        }
        var mutableAnimation = animation
        mutableAnimation?.reset()
        val p = v.layoutParams as MarginLayoutParams
        val currentMargin = p.leftMargin
        mutableAnimation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                val newLeft: Int = if (goalLeftMarginPx > currentMargin) {
                    currentMargin + (abs(currentMargin - goalLeftMarginPx)
                            * interpolatedTime).toInt()
                } else {
                    currentMargin - (abs(currentMargin - goalLeftMarginPx)
                            * interpolatedTime).toInt()
                }
                LibUIUtils.updateLeftMarginPx(newLeft, v)
            }
        }
        // C/N0 updates every second, so animation of 300ms (https://material.io/guidelines/motion/duration-easing.html#duration-easing-common-durations)
        // wit FastOutSlowInInterpolator recommended by Material Design spec easily finishes in time for next C/N0 update
        mutableAnimation.setDuration(300)
        mutableAnimation.setInterpolator(FastOutSlowInInterpolator())
        v.startAnimation(mutableAnimation)
    }

    private fun showHaveFix() {
        binding?.let {
            LibUIUtils.showViewWithAnimation(
                it.skyLock,
                LibUIUtils.ANIMATION_DURATION_SHORT_MS
            )
        }
    }

    private fun showLostFix() {
        binding?.let {
            LibUIUtils.hideViewWithAnimation(
                it.skyLock,
                LibUIUtils.ANIMATION_DURATION_SHORT_MS
            )
        }
    }

    companion object {
        const val TAG = "GpsSkyFragment"
    }
}