package cl.smartvolt.asistencia.feature.overtime

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.smartvolt.asistencia.data.local.OvertimeApprovalDao
import cl.smartvolt.asistencia.data.local.OvertimeApprovalEntity
import cl.smartvolt.asistencia.data.local.toDomain
import cl.smartvolt.asistencia.data.local.toEntity
import cl.smartvolt.asistencia.domain.model.OvertimeApproval
import cl.smartvolt.asistencia.domain.model.OvertimeStatus
import cl.smartvolt.asistencia.domain.model.WorkerRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * ViewModel para la gestión de solicitudes de horas extra.
 *
 * Persiste las solicitudes y decisiones en Room/SQLite para que sobrevivan
 * al cierre de la aplicación. El trabajador solicita; el administrador
 * aprueba o rechaza. No se calculan remuneraciones ni liquidaciones (RB-09).
 */
class OvertimeViewModel(
    private val dao: OvertimeApprovalDao
) : ViewModel() {

    private val _requests = MutableStateFlow<List<OvertimeApproval>>(emptyList())
    val requests: StateFlow<List<OvertimeApproval>> = _requests.asStateFlow()

    init {
        cargarSolicitudes()
    }

    private fun cargarSolicitudes() {
        viewModelScope.launch {
            val lista = withContext(Dispatchers.IO) {
                dao.getAll().map { it.toDomain() }
            }
            _requests.value = lista
        }
    }

    /**
     * El trabajador envía una solicitud de horas extra.
     */
    fun solicitar(
        workerId: String,
        siteId: String,
        date: LocalDate,
        startMillis: Long,
        endMillis: Long,
        reason: String?
    ) {
        val approval = OvertimeApproval(
            approvalId = "OT-${System.currentTimeMillis()}",
            workerId = workerId,
            siteId = siteId,
            date = date,
            authorizedStartMillis = startMillis,
            authorizedEndMillis = endMillis,
            approverRole = WorkerRole.ADMIN,
            approverId = "",
            status = OvertimeStatus.PENDING,
            reason = reason
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.insert(approval.toEntity())
            }
            _requests.value = _requests.value + approval
        }
    }

    /**
     * El administrador aprueba una solicitud.
     */
    fun aprobar(approvalId: String, approverId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.updateDecision(approvalId, OvertimeStatus.APPROVED.name, approverId, null)
            }
            _requests.value = _requests.value.map { req ->
                if (req.approvalId == approvalId && req.status == OvertimeStatus.PENDING) {
                    req.copy(status = OvertimeStatus.APPROVED, approverId = approverId)
                } else req
            }
        }
    }

    /**
     * El administrador rechaza una solicitud (requiere motivo).
     */
    fun rechazar(approvalId: String, approverId: String, motivo: String) {
        if (motivo.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.updateDecision(approvalId, OvertimeStatus.REJECTED.name, approverId, motivo)
            }
            _requests.value = _requests.value.map { req ->
                if (req.approvalId == approvalId && req.status == OvertimeStatus.PENDING) {
                    req.copy(status = OvertimeStatus.REJECTED, approverId = approverId, reason = motivo)
                } else req
            }
        }
    }

    /**
     * Cancela una solicitud pendiente.
     */
    fun cancelar(approvalId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.updateDecision(approvalId, OvertimeStatus.CANCELLED.name, "", "Cancelada por el solicitante")
            }
            _requests.value = _requests.value.map { req ->
                if (req.approvalId == approvalId && req.status == OvertimeStatus.PENDING) {
                    req.copy(status = OvertimeStatus.CANCELLED)
                } else req
            }
        }
    }

    fun porTrabajador(workerId: String): List<OvertimeApproval> =
        _requests.value.filter { it.workerId == workerId }

    fun pendientes(): List<OvertimeApproval> =
        _requests.value.filter { it.status == OvertimeStatus.PENDING }

    fun aprobadas(): List<OvertimeApproval> =
        _requests.value.filter { it.status == OvertimeStatus.APPROVED }

    fun rechazadas(): List<OvertimeApproval> =
        _requests.value.filter { it.status == OvertimeStatus.REJECTED }

    companion object {
        val Factory: (android.app.Application) -> ViewModelProvider.Factory = { application ->
            viewModelFactory {
                initializer {
                    val db = androidx.room.Room.databaseBuilder(
                        application,
                        cl.smartvolt.asistencia.data.local.AppDatabase::class.java,
                        "smartvolt_attendance.db"
                    ).fallbackToDestructiveMigration().build()
                    OvertimeViewModel(db.overtimeApprovalDao())
                }
            }
        }
    }
}
