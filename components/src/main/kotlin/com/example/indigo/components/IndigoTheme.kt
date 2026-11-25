package com.example.indigo.components

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF4F46A5),
        onPrimary = Color.White,
        secondary = Color(0xFF5C5D72),
    )
private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFC7BFFF),
        onPrimary = Color(0xFF282060),
        secondary = Color(0xFFC5C4DD),
    )

@Composable
fun IndigoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        } else if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
