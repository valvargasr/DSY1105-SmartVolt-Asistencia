package cl.smartvolt.asistencia.ui.theme

import android.os.Build
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

// Esquema claro que usa la app por defecto
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
    onErrorContainer = ColorTokens.OnErrorContainerLight
)

// Esquema oscuro listo para futuras preferencias
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

// Esquinas redondeadas de botones, tarjetas y campos
private val SmartVoltShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
// Aplica colores, tipografia y formas a toda la interfaz
fun SmartVoltAsistenciaTheme(
    // La interfaz clara se mantiene constante para que los colores de estado
    // sean siempre reconocibles durante una demostración en terreno.
    darkTheme: Boolean = false,
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

// Colores sueltos que no estan en la paleta principal
private object ColorTokens {
    val OutlineVariantLight = androidx.compose.ui.graphics.Color(0xFFD5DFEC)
    val OutlineVariantDark = androidx.compose.ui.graphics.Color(0xFF40536C)
    val OnErrorContainerLight = androidx.compose.ui.graphics.Color(0xFF7A271A)
}
