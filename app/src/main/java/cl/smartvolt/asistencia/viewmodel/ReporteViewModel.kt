package cl.smartvolt.asistencia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.modelo.DemoDirectory
import cl.smartvolt.asistencia.modelo.MonthlyReport
import cl.smartvolt.asistencia.modelo.MonthlyReportCalculator
import cl.smartvolt.asistencia.modelo.faenaPorDefecto
import cl.smartvolt.asistencia.util.Sesion
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReporteUiState(
    val mes: YearMonth = YearMonth.now(),
    val reporte: MonthlyReport? = null,
    val cargando: Boolean = true
)

class ReporteViewModel(application: Application) : AndroidViewModel(application) {

    private val asistenciaDao = AppDatabase.obtener(application).asistenciaDao()
    private val solicitudDao = AppDatabase.obtener(application).solicitudHoraExtraDao()
    private val faenaDao = AppDatabase.obtener(application).faenaDao()

    private val siteId = Sesion.cuenta?.siteId ?: Asistencia.SITE_ID

    private val _uiState = MutableStateFlow(ReporteUiState())
    val uiState: StateFlow<ReporteUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun mesAnterior() {
        _uiState.update { it.copy(mes = it.mes.minusMonths(1)) }
        cargar()
    }

    fun mesSiguiente() {
        if (_uiState.value.mes >= YearMonth.now()) return
        _uiState.update { it.copy(mes = it.mes.plusMonths(1)) }
        cargar()
    }

    private fun cargar() {
        val mes = _uiState.value.mes
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            try {
                val asistencias = asistenciaDao.observarPorFaena(siteId).first()
                val solicitudes = solicitudDao.listarPorFaena(siteId)
                val faena = faenaDao.obtener(siteId) ?: faenaPorDefecto()

                val reporte = MonthlyReportCalculator.build(
                    events = asistencias,
                    approvals = solicitudes,
                    month = mes,
                    hourlyRateClp = faena.hourlyRateClp.toLong(),
                    overtimeSurchargePercent = faena.overtimeSurchargePercent,
                    nameOf = { workerId ->
                        DemoDirectory.byWorkerId(workerId)?.displayName ?: workerId
                    }
                )

                if (_uiState.value.mes == mes) {
                    _uiState.update { it.copy(reporte = reporte, cargando = false) }
                }
            } catch (cancelado: CancellationException) {
                throw cancelado
            } catch (error: Exception) {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }
}
