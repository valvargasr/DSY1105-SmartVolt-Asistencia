package cl.smartvolt.asistencia.feature.report

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.smartvolt.asistencia.data.local.AppDatabaseProvider
import cl.smartvolt.asistencia.data.local.AttendanceLocalStore
import cl.smartvolt.asistencia.data.local.OvertimeApprovalDao
import cl.smartvolt.asistencia.data.local.toDomain
import cl.smartvolt.asistencia.domain.report.MonthlyReport
import cl.smartvolt.asistencia.domain.report.MonthlyReportCalculator
import cl.smartvolt.asistencia.session.DemoDirectory
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MonthlyReportUiState(
    val month: YearMonth = YearMonth.now(),
    val report: MonthlyReport? = null,
    val cargando: Boolean = true
)

/**
 * Arma el registro del mes con las marcas y las horas extra de la faena.
 *
 * El cálculo vive en [MonthlyReportCalculator]. El ViewModel solo cambia el
 * mes consultado y le pasa los datos guardados en Room.
 */
class MonthlyReportViewModel(
    private val store: AttendanceLocalStore,
    private val overtimeDao: OvertimeApprovalDao,
    private val siteId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyReportUiState())
    val uiState: StateFlow<MonthlyReportUiState> = _uiState.asStateFlow()

    private var trabajo: Job? = null

    init {
        cargar()
    }

    fun onMesAnterior() {
        _uiState.update { it.copy(month = it.month.minusMonths(1)) }
        cargar()
    }

    fun onMesSiguiente() {
        val actual = YearMonth.now()
        if (_uiState.value.month >= actual) return
        _uiState.update { it.copy(month = it.month.plusMonths(1)) }
        cargar()
    }

    private fun cargar() {
        trabajo?.cancel()
        val mes = _uiState.value.month

        trabajo = viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }

            try {
                val reporte = withContext(Dispatchers.IO) {
                    val eventos = store.all().filter { it.siteId == siteId }
                    val solicitudes = overtimeDao.getAll()
                        .map { it.toDomain() }
                        .filter { it.siteId == siteId }

                    MonthlyReportCalculator.build(
                        events = eventos,
                        approvals = solicitudes,
                        month = mes,
                        nameOf = { workerId ->
                            DemoDirectory.byWorkerId(workerId)?.displayName ?: workerId
                        }
                    )
                }

                if (_uiState.value.month == mes) {
                    _uiState.update { it.copy(report = reporte, cargando = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    companion object {
        fun factory(store: AttendanceLocalStore, siteId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as? Application
                        ?: error("MonthlyReportViewModel requiere el contexto de la aplicación.")
                    MonthlyReportViewModel(
                        store = store,
                        overtimeDao = AppDatabaseProvider.get(application).overtimeApprovalDao(),
                        siteId = siteId
                    )
                }
            }
    }
}
