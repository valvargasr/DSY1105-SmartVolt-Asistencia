package cl.smartvolt.asistencia.feature.overtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.smartvolt.asistencia.data.local.OvertimeApprovalDao
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
import java.util.UUID

/**
 * ViewModel de solicitudes de horas extra.
 * El trabajador crea y consulta sus solicitudes; solo el administrador decide.
 */
class OvertimeViewModel(
    private val dao: OvertimeApprovalDao
) : ViewModel() {

    private val _requests = MutableStateFlow<List<OvertimeApproval>>(emptyList())
    val requests: StateFlow<List<OvertimeApproval>> = _requests.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        cargarSolicitudes()
    }

    private fun cargarSolicitudes() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { dao.getAll().map { it.toDomain() } }
            }.onSuccess { _requests.value = it }
                .onFailure { _operationMessage.value = "No se pudieron cargar las solicitudes." }
        }
    }

    fun limpiarMensaje() {
        _operationMessage.value = null
    }

    /** Solo un obrero puede crear solicitudes. */
    fun solicitar(
        requesterRole: WorkerRole,
        workerId: String,
        siteId: String,
        date: LocalDate,
        startMillis: Long,
        endMillis: Long,
        reason: String
    ) {
        if (requesterRole != WorkerRole.WORKER) {
            _operationMessage.value = "Solo el obrero puede crear una solicitud."
            return
        }
        if (workerId.isBlank() || siteId.isBlank() || reason.isBlank() || endMillis <= startMillis) {
            _operationMessage.value = "Revisa los datos de la solicitud antes de enviarla."
            return
        }

        val approval = OvertimeApproval(
            approvalId = "OT-${UUID.randomUUID()}",
            workerId = workerId,
            siteId = siteId,
            date = date,
            authorizedStartMillis = startMillis,
            authorizedEndMillis = endMillis,
            approverRole = WorkerRole.ADMIN,
            approverId = "",
            status = OvertimeStatus.PENDING,
            reason = reason.trim()
        )

        viewModelScope.launch {
            _isSaving.value = true
            _operationMessage.value = null
            try {
                withContext(Dispatchers.IO) { dao.insert(approval.toEntity()) }
                _requests.value = (_requests.value + approval)
                    .sortedByDescending { it.createdAtMillis }
                _operationMessage.value = "Solicitud enviada. Puedes revisar su estado aquí."
            } catch (error: Exception) {
                _operationMessage.value = "No se pudo guardar la solicitud. Intenta nuevamente."
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** Solo un administrador puede aprobar. */
    fun aprobar(approvalId: String, approverId: String, approverRole: WorkerRole) {
        if (approverRole != WorkerRole.ADMIN) return
        val solicitud = _requests.value.firstOrNull {
            it.approvalId == approvalId && it.status == OvertimeStatus.PENDING
        } ?: return

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    dao.updateDecision(
                        approvalId,
                        OvertimeStatus.APPROVED.name,
                        approverId,
                        solicitud.reason
                    )
                }
            }.onSuccess {
                _requests.value = _requests.value.map { req ->
                    if (req.approvalId == approvalId && req.status == OvertimeStatus.PENDING) {
                        req.copy(status = OvertimeStatus.APPROVED, approverId = approverId)
                    } else req
                }
                _operationMessage.value = "Solicitud aprobada."
            }.onFailure {
                _operationMessage.value = "No se pudo aprobar la solicitud."
            }
        }
    }

    /** Solo un administrador puede rechazar y el motivo es obligatorio. */
    fun rechazar(
        approvalId: String,
        approverId: String,
        approverRole: WorkerRole,
        motivo: String
    ) {
        if (approverRole != WorkerRole.ADMIN || motivo.isBlank()) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    dao.updateDecision(
                        approvalId,
                        OvertimeStatus.REJECTED.name,
                        approverId,
                        motivo.trim()
                    )
                }
            }.onSuccess {
                _requests.value = _requests.value.map { req ->
                    if (req.approvalId == approvalId && req.status == OvertimeStatus.PENDING) {
                        req.copy(
                            status = OvertimeStatus.REJECTED,
                            approverId = approverId,
                            reason = motivo.trim()
                        )
                    } else req
                }
                _operationMessage.value = "Solicitud rechazada."
            }.onFailure {
                _operationMessage.value = "No se pudo rechazar la solicitud."
            }
        }
    }

    fun porTrabajador(workerId: String): List<OvertimeApproval> =
        _requests.value.filter { it.workerId == workerId }

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
