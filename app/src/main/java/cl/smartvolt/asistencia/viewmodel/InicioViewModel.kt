package cl.smartvolt.asistencia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.modelo.Faena
import cl.smartvolt.asistencia.modelo.faenaPorDefecto
import cl.smartvolt.asistencia.util.Fechas
import cl.smartvolt.asistencia.util.Jornada
import cl.smartvolt.asistencia.util.Sesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// Datos que muestra la pantalla de inicio
data class InicioUiState(
    val cargando: Boolean = true,
    val faena: Faena = faenaPorDefecto(),
    val jornada: Jornada = Jornada(),
    val pendientes: Int = 0
)

// Junta las marcas de hoy y la faena activa
class InicioViewModel(application: Application) : AndroidViewModel(application) {

    private val asistenciaDao = AppDatabase.obtener(application).asistenciaDao()
    private val faenaDao = AppDatabase.obtener(application).faenaDao()

    // Trabajador de la sesion actual
    private val workerId = Sesion.cuenta?.workerId ?: Asistencia.WORKER_ID

    // Estado que observa la pantalla
    private val _uiState = MutableStateFlow(InicioUiState())
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    init {
        // la pantalla de inicio solo mira la jornada de hoy y las marcas pendientes
        viewModelScope.launch {
            combine(
                asistenciaDao.observarPorTrabajador(workerId),
                asistenciaDao.observarPendientes(),
                faenaDao.observarActiva()
            ) { marcas, pendientes, faenaGuardada ->
                InicioUiState(
                    cargando = false,
                    faena = faenaGuardada ?: faenaPorDefecto(),
                    jornada = Fechas.estadoJornada(marcas),
                    pendientes = pendientes.size
                )
            }.collect { estado ->
                _uiState.value = estado
            }
        }
    }
}
