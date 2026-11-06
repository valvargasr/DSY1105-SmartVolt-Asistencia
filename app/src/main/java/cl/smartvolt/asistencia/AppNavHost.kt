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

private const val RUTA_SPLASH = "splash"
private const val RUTA_LOGIN = "login"
private const val RUTA_PRINCIPAL = "principal"

@Composable
fun AppNavHost() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = RUTA_SPLASH) {
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

        composable(RUTA_PRINCIPAL) {
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
