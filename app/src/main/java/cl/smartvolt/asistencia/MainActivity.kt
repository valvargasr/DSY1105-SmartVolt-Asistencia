package cl.smartvolt.asistencia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.smartvolt.asistencia.ui.theme.SmartVoltAsistenciaTheme
import cl.smartvolt.asistencia.ui.view.LoginView
import cl.smartvolt.asistencia.ui.view.MainContainerView
import cl.smartvolt.asistencia.ui.view.SplashView
import cl.smartvolt.asistencia.util.Sesion

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartVoltAsistenciaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavegacionPrincipal()
                }
            }
        }
    }
}

private const val RUTA_SPLASH = "splash"
private const val RUTA_LOGIN = "login"
private const val RUTA_PRINCIPAL = "principal"

@androidx.compose.runtime.Composable
private fun NavegacionPrincipal() {
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
