package cl.smartvolt.asistencia.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = VoltAmber,
    onPrimary = VoltAmberInk,
    primaryContainer = VoltAmberContainer,
    onPrimaryContainer = OnVoltAmberContainer,
    secondary = SteelTeal,
    onSecondary = OnSteelTeal,
    secondaryContainer = SteelTealContainer,
    onSecondaryContainer = OnSteelTealContainer,
    tertiary = SafetyRust,
    onTertiary = OnSafetyRust,
    tertiaryContainer = SafetyRustContainer,
    onTertiaryContainer = OnSafetyRustContainer,
    background = Canvas,
    onBackground = Carbon,
    surface = Paper,
    onSurface = Carbon,
    surfaceVariant = PaperVariant,
    onSurfaceVariant = CarbonMuted,
    outline = OutlineSoft,
    outlineVariant = ColorTokens.OutlineVariantLight,
    error = ErrorRed,
    onError = OnSafetyRust,
    errorContainer = ErrorContainerSoft,
    onErrorContainer = OnSafetyRustContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = VoltAmberNight,
    onPrimary = VoltAmberNightInk,
    primaryContainer = VoltAmberNightContainer,
    onPrimaryContainer = OnVoltAmberNightContainer,
    secondary = SteelTealNight,
    onSecondary = OnSteelTealNight,
    secondaryContainer = SteelTealNightContainer,
    onSecondaryContainer = OnSteelTealNightContainer,
    tertiary = SafetyRustNight,
    onTertiary = OnSafetyRustNight,
    tertiaryContainer = SafetyRustNightContainer,
    onTertiaryContainer = OnSafetyRustNightContainer,
    background = CarbonNight,
    onBackground = CarbonNightMuted,
    surface = CarbonNightSurface,
    onSurface = CarbonNightMuted,
    surfaceVariant = PaperNightVariant,
    onSurfaceVariant = OutlineNight,
    outline = OutlineNight,
    outlineVariant = ColorTokens.OutlineVariantDark,
    error = ErrorNight,
    onError = OnSafetyRustNight,
    errorContainer = ErrorNightContainer,
    onErrorContainer = OnErrorNightContainer
)

private val SmartVoltShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartVoltAsistenciaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // La marca manda: el color dinámico queda disponible, pero apagado por
    // defecto para que la app se vea igual en cualquier teléfono.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SmartVoltShapes,
        content = content
    )
}

/** Colores auxiliares que no tienen un rol directo en Material 3. */
private object ColorTokens {
    val OutlineVariantLight = androidx.compose.ui.graphics.Color(0xFFD8D1C6)
    val OutlineVariantDark = androidx.compose.ui.graphics.Color(0xFF45463E)
}
