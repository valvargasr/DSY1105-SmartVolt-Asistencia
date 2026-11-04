package cl.smartvolt.asistencia.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.smartvolt.asistencia.data.local.AttendanceLocalStore
import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.domain.validation.AttendanceValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/**
 * Resumen del día para la pantalla de inicio.
 *
 * Para el obrero se muestra su propia jornada. Para capataz y administrador se
 * agrega la faena completa. La lógica de fechas queda fuera del composable.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val dayState: AttendanceValidator.DayAttendanceState =
        AttendanceValidator.DayAttendanceState(),
    val entryMillis: Long? = null,
    val exitMillis: Long? = null,
    val pendingSync: Int = 0,
    val teamPresent: Int = 0,
    val teamCheckedOut: Int = 0,
    val teamMarks: Int = 0,
    val lastEvent: LocalAttendanceEvent? = null
)

class HomeViewModel(
    private val store: AttendanceLocalStore,
    private val workerId: String,
    private val siteId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refrescar()
    }

    fun refrescar() {
        viewModelScope.launch {
            val estado = withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val todos = store.all()
                val zone = ZoneId.systemDefault()
                val hoy = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

                fun delDia(event: LocalAttendanceEvent): Boolean =
                    Instant.ofEpochMilli(event.timestampMillis).atZone(zone).toLocalDate() == hoy

                val propios = todos.filter { it.workerId == workerId && delDia(it) }
                    .sortedBy { it.timestampMillis }
                val faena = todos.filter { it.siteId == siteId && delDia(it) }
                val entradas = faena.filter { it.type == LocalAttendanceEvent.TYPE_CHECK_IN }
                val salidas = faena.filter { it.type == LocalAttendanceEvent.TYPE_CHECK_OUT }

                HomeUiState(
                    isLoading = false,
                    dayState = AttendanceValidator.dayState(
                        events = todos,
                        nowMillis = now,
                        workerId = workerId
                    ),
                    entryMillis = propios.firstOrNull {
                        it.type == LocalAttendanceEvent.TYPE_CHECK_IN
                    }?.timestampMillis,
                    exitMillis = propios.lastOrNull {
                        it.type == LocalAttendanceEvent.TYPE_CHECK_OUT
                    }?.timestampMillis,
                    pendingSync = todos.count {
                        it.syncState == LocalAttendanceEvent.PENDING_SYNC
                    },
                    teamPresent = entradas.map { it.workerId }.distinct().size,
                    teamCheckedOut = salidas.map { it.workerId }.distinct().size,
                    teamMarks = faena.size,
                    lastEvent = propios.lastOrNull()
                )
            }
            _uiState.value = estado
        }
    }

    companion object {
        fun factory(
            store: AttendanceLocalStore,
            workerId: String,
            siteId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(store, workerId, siteId) }
        }
    }
}
