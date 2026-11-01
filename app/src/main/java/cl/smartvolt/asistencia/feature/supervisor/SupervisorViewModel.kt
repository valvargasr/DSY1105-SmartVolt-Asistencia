package cl.smartvolt.asistencia.feature.supervisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.local.AttendanceLocalStore
import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Resumen operativo de asistencia para el panel de supervisor.
 *
 * No calcula remuneraciones ni genera reportes de renta (RB-09).
 * Solo informa estados operacionales de asistencia.
 */
data class SupervisorSummary(
    val siteId: String,
    val totalWorkers: Int,
    val presentOnTime: Int,
    val presentLate: Int,
    val absent: Int,
    val pending: Int,
    val checkedOut: Int,
    val pendingSyncCount: Int
)

/**
 * ViewModel para el panel de supervisor.
 *
 * Agrega las marcas locales por faena para mostrar el estado del equipo.
 * La UI (Área A) consume este estado para renderizar el panel.
 */
class SupervisorViewModel(
    private val store: AttendanceLocalStore,
    private val siteId: String
) : ViewModel() {

    private val _summary = MutableStateFlow(
        SupervisorSummary(
            siteId = siteId,
            totalWorkers = 0,
            presentOnTime = 0,
            presentLate = 0,
            absent = 0,
            pending = 0,
            checkedOut = 0,
            pendingSyncCount = 0
        )
    )
    val summary: StateFlow<SupervisorSummary> = _summary.asStateFlow()

    private val _events = MutableStateFlow<List<LocalAttendanceEvent>>(emptyList())
    val events: StateFlow<List<LocalAttendanceEvent>> = _events.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            val events = withContext(Dispatchers.IO) {
                store.all().filter { it.siteId == siteId }
            }
            _events.value = events

            val checkIns = events.filter { it.type == LocalAttendanceEvent.TYPE_CHECK_IN }
            val checkOuts = events.filter { it.type == LocalAttendanceEvent.TYPE_CHECK_OUT }

            _summary.value = SupervisorSummary(
                siteId = siteId,
                totalWorkers = checkIns.map { it.workerId }.distinct().size,
                presentOnTime = 0, // Se clasifica cuando se definan las reglas de turno
                presentLate = 0,
                absent = 0,
                pending = events.count { it.syncState == LocalAttendanceEvent.PENDING_SYNC },
                checkedOut = checkOuts.map { it.workerId }.distinct().size,
                pendingSyncCount = store.count()
            )
        }
    }

    fun refrescar() {
        cargarDatos()
    }
}
