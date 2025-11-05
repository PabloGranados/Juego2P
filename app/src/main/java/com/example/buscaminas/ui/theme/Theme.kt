package com.example.buscaminas.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.buscaminas.model.AppTheme

// Tema Guinda IPN - Claro
private val GuindaIPNLightScheme = lightColorScheme(
    primary = GuindaIPNPrimary,
    secondary = GuindaIPNSecondary,
    tertiary = GuindaIPNTertiary,
    background = GuindaIPNBackground,
    surface = GuindaIPNSurface,
    error = ErrorColor
)

// Tema Guinda IPN - Oscuro
private val GuindaIPNDarkScheme = darkColorScheme(
    primary = GuindaIPNPrimaryDark,
    secondary = GuindaIPNSecondaryDark,
    tertiary = GuindaIPNTertiaryDark,
    background = GuindaIPNBackgroundDark,
    surface = GuindaIPNSurfaceDark,
    error = ErrorColor
)

// Tema Azul ESCOM - Claro
private val AzulESCOMLightScheme = lightColorScheme(
    primary = AzulESCOMPrimary,
    secondary = AzulESCOMSecondary,
    tertiary = AzulESCOMTertiary,
    background = AzulESCOMBackground,
    surface = AzulESCOMSurface,
    error = ErrorColor
)

// Tema Azul ESCOM - Oscuro
private val AzulESCOMDarkScheme = darkColorScheme(
    primary = AzulESCOMPrimaryDark,
    secondary = AzulESCOMSecondaryDark,
    tertiary = AzulESCOMTertiaryDark,
    background = AzulESCOMBackgroundDark,
    surface = AzulESCOMSurfaceDark,
    error = ErrorColor
)

@Composable
fun BuscaminasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.GUINDA_IPN,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.GUINDA_IPN -> if (darkTheme) GuindaIPNDarkScheme else GuindaIPNLightScheme
        AppTheme.AZUL_ESCOM -> if (darkTheme) AzulESCOMDarkScheme else AzulESCOMLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}