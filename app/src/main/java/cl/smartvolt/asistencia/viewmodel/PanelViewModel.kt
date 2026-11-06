package cl.smartvolt.asistencia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.modelo.Faena
import cl.smartvolt.asistencia.modelo.faenaPorDefecto
import cl.smartvolt.asistencia.util.Fechas
import cl.smartvolt.asistencia.util.Sesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PanelUiState(
    val faena: Faena = faenaPorDefecto(),
    val marcasHoy: List<Asistencia> = emptyList(),
    val totalTrabajadores: Int = 0,
    val presentes: Int = 0,
    val conSalida: Int = 0,
    val pendientes: Int = 0
)

class PanelViewModel(application: Application) : AndroidViewModel(application) {

    private val asistenciaDao = AppDatabase.obtener(application).asistenciaDao()
    private val faenaDao = AppDatabase.obtener(application).faenaDao()

    private val siteId = Sesion.cuenta?.siteId ?: Asistencia.SITE_ID

    private val _uiState = MutableStateFlow(PanelUiState())
    val uiState: StateFlow<PanelUiState> = _uiState.asStateFlow()

    init {
        // nos quedamos con la última marca de cada trabajador y de ahí salen los contadores
        viewModelScope.launch {
            combine(
                asistenciaDao.observarPorFaena(siteId),
                faenaDao.observarActiva()
            ) { marcas, faenaGuardada ->
                val hoy = marcas.filter { Fechas.esHoy(it.timestampMillis) }
                val ultimas = hoy.groupBy { it.workerId }
                    .mapValues { (_, lista) -> lista.maxByOrNull { it.timestampMillis } }

                PanelUiState(
                    faena = faenaGuardada ?: faenaPorDefecto(),
                    marcasHoy = hoy.sortedBy { it.timestampMillis },
                    totalTrabajadores = ultimas.size,
                    presentes = ultimas.values.count {
                        it?.type == Asistencia.TYPE_CHECK_IN
                    },
                    conSalida = ultimas.values.count {
                        it?.type == Asistencia.TYPE_CHECK_OUT
                    },
                    pendientes = hoy.count { it.syncState == Asistencia.PENDING_SYNC }
                )
            }.collect { _uiState.value = it }
        }
    }
}
