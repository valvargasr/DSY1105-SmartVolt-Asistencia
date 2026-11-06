package cl.smartvolt.asistencia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.smartvolt.asistencia.ui.view.LoginView
import cl.smartvolt.asistencia.ui.view.MainContainerView
import cl.smartvolt.asistencia.ui.view.SplashView
import cl.smartvolt.asistencia.util.Sesion

// Nombres de las tres pantallas principales
private const val RUTA_SPLASH = "splash"
private const val RUTA_LOGIN = "login"
private const val RUTA_PRINCIPAL = "principal"

@Composable
// Controla el paso de splash a login y luego a la app
fun AppNavHost() {
    val nav = rememberNavController()

    // son tres pantallas nomás: splash, login y el contenedor con las pestañas
    NavHost(navController = nav, startDestination = RUTA_SPLASH) {
        // SPLASH
        composable(RUTA_SPLASH) {
            SplashView(
                onContinuar = {
                    nav.navigate(RUTA_LOGIN) {
                        popUpTo(RUTA_SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // LOGIN
        composable(RUTA_LOGIN) {
            LoginView(
                onSesionIniciada = {
                    nav.navigate(RUTA_PRINCIPAL) {
                        popUpTo(RUTA_LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // CONTENEDOR CON PESTANAS
        composable(RUTA_PRINCIPAL) {
            // si no hay sesión guardada lo devolvemos al login
            val cuenta = Sesion.cuenta
            if (cuenta == null) {
                LaunchedEffect(Unit) {
                    nav.navigate(RUTA_LOGIN) {
                        popUpTo(RUTA_PRINCIPAL) { inclusive = true }
                    }
                }
            } else {
                MainContainerView(
                    cuenta = cuenta,
                    onCerrarSesion = {
                        // al cerrar sesión limpiamos la cuenta y volvemos al login
                        Sesion.cuenta = null
                        nav.navigate(RUTA_LOGIN) {
                            popUpTo(RUTA_PRINCIPAL) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
