/*
 * Copyright (C) 2020 Sean J. Barbeau
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
package com.craxiom.networksurvey.ui.gnss.model

import com.craxiom.networksurvey.model.GnssType
import com.craxiom.networksurvey.model.SatelliteStatus
import com.craxiom.networksurvey.model.SbasType

/**
 * A container class that holds metadata and statistics information about a group of satellites.
 * Summary statistics on the constellation family such as the number of signals in view
 * ([numSignalsInView]), number of signals used in the fix ([numSignalsUsed], and the number
 * of satellites used in the fix ([numSatsUsed]), and the number of satellites in view ([numSatsInView]).
 * [unknownCarrierStatuses] is a Map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
 * has been detected with an unknown GNSS frequency.
 * [isDualFrequencyPerSatInView] is true if this device is viewing multiple signals from the same satellite, false if it is not.
 * [isDualFrequencyPerSatInUse] is true if this device is using multiple signals from the same satellite, false if it is not.
 * [isNonPrimaryCarrierFreqInView] is true if a non-primary carrier frequency is in use by at least one satellite, or false if
 * only primary carrier frequencies are in view.
 * [isNonPrimaryCarrierFreqInUse] is true if a non-primary carrier frequency is in use by at least one satellite, or false if
 * only primary carrier frequencies are in use.
 * [duplicateCarrierStatuses] is a Map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
 * has been detected as having duplicate carrier frequency data with another signal.
 */
data class SatelliteMetadata(
    val numSignalsInView: Int = 0,
    val numSignalsUsed: Int = 0,
    val numSignalsTotal: Int = 0,
    val numSatsInView: Int = 0,
    val numSatsUsed: Int = 0,
    val numSatsTotal: Int = 0,
    val supportedGnss: Set<GnssType> = HashSet(),
    val supportedGnssCfs: Set<String> = HashSet(),
    val supportedSbas: Set<SbasType> = HashSet(),
    val supportedSbasCfs: Set<String> = HashSet(),
    val unknownCarrierStatuses: Map<String, SatelliteStatus> = HashMap(),
    val duplicateCarrierStatuses: Map<String, SatelliteStatus> = HashMap(),
    val isDualFrequencyPerSatInView: Boolean = false,
    val isDualFrequencyPerSatInUse: Boolean = false,
    val isNonPrimaryCarrierFreqInView: Boolean = false,
    val isNonPrimaryCarrierFreqInUse: Boolean = false,
)