package com.craxiom.networksurvey.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.craxiom.networksurvey.R

@Composable
fun OldNsTheme(content: @Composable () -> Unit) {
    val darkColorScheme =
        darkColorScheme(
            surface = colorResource(id = R.color.colorCardDark),
            background = Color.Black,
            primary = colorResource(id = R.color.colorAccent),
            tertiary = colorResource(id = R.color.colorAccent),
        )
    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography(),
        content = content,
    )
}

private val DarkColorScheme: ColorScheme
    @Composable
    get() = darkColorScheme(
        primary = colorResource(id = R.color.colorAccent),
        secondary = ColorBlueIceberg,
        tertiary = colorResource(id = R.color.colorAccent),
        background = ColorBlack,
        surface = ColorBlack,
        onPrimary = ColorOffBlack,
        onSecondary = ColorWhiteCultured,
        onTertiary = ColorWhiteCultured,
        onBackground = ColorWhiteCultured,
        onSurface = colorResource(id = R.color.normalText),
        surfaceTint = ColorWhiteCultured
    )

private val LightColorScheme = lightColorScheme(
    primary = ColorWhiteCultured,
    secondary = ColorBlueIceberg,
    tertiary = ColorRedFieryRose,
    background = ColorBlueIceberg,
    surface = ColorBlueIceberg,
    onPrimary = ColorOffBlack,
    onSecondary = ColorWhiteCultured,
    onTertiary = ColorWhiteCultured,
    onBackground = ColorWhiteCultured,
    onSurface = ColorWhiteCultured,
    surfaceTint = ColorBlack
)

@Composable
fun OldNsTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    val currentWindow = (view.context as? Activity)?.window
    if (!view.isInEditMode && currentWindow != null) {
        /*val currentWindow = (view.context as? Activity)?.window
             ?: throw Exception("Not in an activity - unable to get Window reference")*/

        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.background.toArgb()
            (view.context as Activity).window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(currentWindow, view).isAppearanceLightStatusBars =
                false
            /*WindowCompat.setDecorFitsSystemWindows(currentWindow, false)
            val insetsController = WindowCompat.getInsetsController(currentWindow, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.hide(WindowInsetsCompat.Type.statusBars())*/
        }
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography(), content = content
    )
}
