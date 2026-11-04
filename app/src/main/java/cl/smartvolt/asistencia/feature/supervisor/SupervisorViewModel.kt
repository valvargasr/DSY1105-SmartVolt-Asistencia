package cl.smartvolt.asistencia.feature.supervisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.smartvolt.asistencia.data.local.AttendanceLocalStore
import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/** Resumen del día calculado desde las marcas locales de la faena. */
data class SupervisorSummary(
    val siteId: String,
    val totalWorkers: Int,
    val presentNow: Int,
    /** Nulo mientras no exista una configuración aprobada de turnos y tolerancia. */
    val presentLate: Int?,
    /** Nulo mientras no exista una nómina de turnos y una hora de corte aprobadas. */
    val absent: Int?,
    val pending: Int,
    val checkedOut: Int,
    val pendingSyncCount: Int
)

/** Agrega las marcas del día actual para la faena asignada al supervisor. */
class SupervisorViewModel(
    private val store: AttendanceLocalStore,
    private val siteId: String
) : ViewModel() {

    private val _summary = MutableStateFlow(
        SupervisorSummary(
            siteId = siteId,
            totalWorkers = 0,
            presentNow = 0,
            presentLate = null,
            absent = null,
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
            val zone = ZoneId.systemDefault()
            val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
            val todayEvents = withContext(Dispatchers.IO) {
                store.all().filter { event ->
                    event.siteId == siteId &&
                        Instant.ofEpochMilli(event.timestampMillis).atZone(zone).toLocalDate() == today
                }.sortedBy { it.timestampMillis }
            }
            _events.value = todayEvents

            val byWorker = todayEvents.groupBy { it.workerId }
            val lastByWorker = byWorker.mapValues { (_, marks) -> marks.maxByOrNull { it.timestampMillis } }
            val presentNow = lastByWorker.values.count {
                it?.type == LocalAttendanceEvent.TYPE_CHECK_IN
            }
            val checkedOut = lastByWorker.values.count {
                it?.type == LocalAttendanceEvent.TYPE_CHECK_OUT
            }

            _summary.value = SupervisorSummary(
                siteId = siteId,
                totalWorkers = byWorker.size,
                presentNow = presentNow,
                // El proyecto aún no tiene horarios/tolerancias ni nómina de turnos aprobados.
                // Mostrar 0 sería inventar un resultado; null se presenta como "Por configurar".
                presentLate = null,
                absent = null,
                pending = todayEvents.count { it.syncState == LocalAttendanceEvent.PENDING_SYNC },
                checkedOut = checkedOut,
                pendingSyncCount = todayEvents.count { it.syncState == LocalAttendanceEvent.PENDING_SYNC }
            )
        }
    }

    fun refrescar() = cargarDatos()

    companion object {
        fun factory(
            store: AttendanceLocalStore,
            siteId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SupervisorViewModel(store, siteId) }
        }
    }
}
