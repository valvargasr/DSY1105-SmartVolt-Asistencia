package cl.smartvolt.asistencia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cl.smartvolt.asistencia.ui.theme.SmartVoltAsistenciaTheme

// Punto de entrada de la app Android
class MainActivity : ComponentActivity() {
    // Prepara la ventana y carga la interfaz
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // acá arranca todo, el tema envuelve la navegación completa
        setContent {
            SmartVoltAsistenciaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                }
            }
        }
    }
}
