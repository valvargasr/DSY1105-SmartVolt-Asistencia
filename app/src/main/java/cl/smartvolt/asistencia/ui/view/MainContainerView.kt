package cl.smartvolt.asistencia.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.smartvolt.asistencia.modelo.DemoAccount
import cl.smartvolt.asistencia.modelo.WorkerRole
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar

private const val RUTA_INICIO = "inicio"
private const val RUTA_ASISTENCIA = "asistencia"
private const val RUTA_HISTORIAL = "historial"
private const val RUTA_GESTION = "gestion"
private const val RUTA_PANEL = "panel"
private const val RUTA_FAENA = "faena"
private const val RUTA_REPORTE = "reporte"
private const val RUTA_HORAS_EXTRA = "horas_extra"

private data class Pestana(val ruta: String, val etiqueta: String, val icono: ImageVector)

private val pestanas = listOf(
    Pestana(RUTA_INICIO, "Inicio", Icons.Outlined.Home),
    Pestana(RUTA_ASISTENCIA, "Asistencia", Icons.Outlined.MyLocation),
    Pestana(RUTA_HISTORIAL, "Historial", Icons.Outlined.History),
    Pestana(RUTA_GESTION, "Gestión", Icons.Outlined.Settings)
)

@Composable
fun MainContainerView(
    cuenta: DemoAccount,
    onCerrarSesion: () -> Unit
) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route ?: RUTA_INICIO
    val mostrarBarra = pestanas.any { it.ruta == rutaActual }

    // la barra de abajo solo aparece en las pestañas, no en las pantallas internas
    Scaffold(
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    pestanas.forEach { pestana ->
                        NavigationBarItem(
                            selected = rutaActual == pestana.ruta,
                            onClick = { nav.irAPestana(pestana.ruta) },
                            icon = {
                                Icon(pestana.icono, contentDescription = pestana.etiqueta)
                            },
                            label = { Text(pestana.etiqueta) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = RUTA_INICIO,
            modifier = Modifier.padding(padding)
        ) {
            composable(RUTA_INICIO) {
                InicioView(
                    cuenta = cuenta,
                    onIrAsistencia = { nav.irAPestana(RUTA_ASISTENCIA) },
                    onIrHistorial = { nav.irAPestana(RUTA_HISTORIAL) },
                    onAbrirPanel = { nav.navigate(RUTA_PANEL) { launchSingleTop = true } },
                    onAbrirFaena = { nav.navigate(RUTA_FAENA) { launchSingleTop = true } },
                    onAbrirReporte = { nav.navigate(RUTA_REPORTE) { launchSingleTop = true } },
                    onAbrirHorasExtra = {
                        nav.navigate(RUTA_HORAS_EXTRA) { launchSingleTop = true }
                    }
                )
            }

            composable(RUTA_ASISTENCIA) {
                AsistenciaView()
            }

            composable(RUTA_HISTORIAL) {
                HistorialView()
            }

            composable(RUTA_GESTION) {
                GestionView(
                    cuenta = cuenta,
                    onAbrirPanel = { nav.navigate(RUTA_PANEL) { launchSingleTop = true } },
                    onAbrirFaena = { nav.navigate(RUTA_FAENA) { launchSingleTop = true } },
                    onAbrirReporte = { nav.navigate(RUTA_REPORTE) { launchSingleTop = true } },
                    onAbrirHorasExtra = {
                        nav.navigate(RUTA_HORAS_EXTRA) { launchSingleTop = true }
                    },
                    onCerrarSesion = onCerrarSesion
                )
            }

            composable(RUTA_PANEL) {
                PanelView(onVolver = { nav.popBackStack() })
            }

            // el admin es el único que puede entrar a faena y al reporte
            composable(RUTA_FAENA) {
                if (cuenta.role != WorkerRole.ADMIN) {
                    AccesoDenegado(
                        titulo = "Faena",
                        detalle = "Solo el administrador puede cambiar el lugar de la faena.",
                        onVolver = { nav.popBackStack() }
                    )
                } else {
                    FaenaView(
                        onVolver = { nav.popBackStack() },
                        onGuardado = { nav.popBackStack() }
                    )
                }
            }

            composable(RUTA_REPORTE) {
                if (cuenta.role != WorkerRole.ADMIN) {
                    AccesoDenegado(
                        titulo = "Registro del mes",
                        detalle = "Solo el administrador puede ver el registro del mes.",
                        onVolver = { nav.popBackStack() }
                    )
                } else {
                    ReporteView(
                        siteName = cuenta.siteName,
                        onVolver = { nav.popBackStack() }
                    )
                }
            }

            composable(RUTA_HORAS_EXTRA) {
                when (cuenta.role) {
                    WorkerRole.WORKER, WorkerRole.ADMIN -> HorasExtraView(
                        cuenta = cuenta,
                        onVolver = { nav.popBackStack() }
                    )

                    else -> AccesoDenegado(
                        titulo = "Horas extra",
                        detalle = "Esta sección está disponible para obreros y administradores.",
                        onVolver = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}

private fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(RUTA_INICIO) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AccesoDenegado(
    titulo: String,
    detalle: String,
    onVolver: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SmartVoltTopBar(
                title = titulo,
                subtitle = "Acceso restringido",
                onBack = onVolver
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = detalle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Vuelve atrás para continuar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
