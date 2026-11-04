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
import cl.smartvolt.asistencia.domain.validation.OvertimeRequestErrors
import cl.smartvolt.asistencia.domain.validation.OvertimeRequestInput
import cl.smartvolt.asistencia.domain.validation.OvertimeRequestResult
import cl.smartvolt.asistencia.domain.validation.OvertimeRequestValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Estado del formulario de solicitud que completa el trabajador. */
data class OvertimeRequestFormState(
    val fecha: String = LocalDate.now().toString(),
    val desde: String = "",
    val hasta: String = "",
    val motivo: String = "",
    val errors: OvertimeRequestErrors = OvertimeRequestErrors(),
    val mensaje: String = "",
    val mensajeEsError: Boolean = false,
    val enviando: Boolean = false
)

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

    private val _form = MutableStateFlow(OvertimeRequestFormState(fecha = hoyTexto()))
    val form: StateFlow<OvertimeRequestFormState> = _form.asStateFlow()

    fun actualizarFecha(valor: String) {
        _form.value = _form.value.copy(
            fecha = valor,
            errors = _form.value.errors.copy(fecha = ""),
            mensaje = ""
        )
    }

    fun actualizarDesde(valor: String) {
        _form.value = _form.value.copy(
            desde = valor,
            errors = _form.value.errors.copy(desde = ""),
            mensaje = ""
        )
    }

    fun actualizarHasta(valor: String) {
        _form.value = _form.value.copy(
            hasta = valor,
            errors = _form.value.errors.copy(hasta = ""),
            mensaje = ""
        )
    }

    fun actualizarMotivo(valor: String) {
        _form.value = _form.value.copy(
            motivo = valor,
            errors = _form.value.errors.copy(motivo = ""),
            mensaje = ""
        )
    }

    /**
     * Valida el formulario y, si corresponde, guarda la solicitud del trabajador.
     *
     * La validación se delega en [OvertimeRequestValidator]; el ViewModel solo
     * traduce el resultado a estado de la pantalla.
     */
    fun enviarSolicitud(workerId: String, siteId: String) {
        val actual = _form.value
        if (actual.enviando) return

        when (val resultado = OvertimeRequestValidator.validate(
            OvertimeRequestInput(
                fecha = actual.fecha,
                desde = actual.desde,
                hasta = actual.hasta,
                motivo = actual.motivo
            )
        )) {
            is OvertimeRequestResult.Invalid -> {
                _form.value = actual.copy(
                    errors = resultado.errors,
                    mensaje = "Revisa los campos marcados.",
                    mensajeEsError = true
                )
            }

            is OvertimeRequestResult.Valid -> {
                val datos = resultado.data
                val approval = OvertimeApproval(
                    approvalId = "OT-${System.currentTimeMillis()}",
                    workerId = workerId,
                    siteId = siteId,
                    date = datos.date,
                    authorizedStartMillis = datos.startMillis,
                    authorizedEndMillis = datos.endMillis,
                    approverRole = WorkerRole.ADMIN,
                    approverId = "",
                    status = OvertimeStatus.PENDING,
                    reason = datos.reason
                )
                _form.value = actual.copy(enviando = true, mensaje = "", mensajeEsError = false)
                viewModelScope.launch {
                    withContext(Dispatchers.IO) { dao.insert(approval.toEntity()) }
                    _requests.value = _requests.value + approval
                    _form.value = OvertimeRequestFormState(
                        fecha = datos.date.format(java.time.format.DateTimeFormatter.ofPattern("d/M/uuuu")),
                        mensaje = "Solicitud enviada. Queda pendiente de autorización.",
                        mensajeEsError = false
                    )
                }
            }
        }
    }

    fun limpiarFormulario() {
        _form.value = OvertimeRequestFormState(fecha = hoyTexto())
    }

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
                    req.copy(
                        status = OvertimeStatus.REJECTED,
                        approverId = approverId,
                        decisionNote = motivo
                    )
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
        private fun hoyTexto(): String =
            LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("d/M/uuuu"))

        val Factory: (android.app.Application) -> ViewModelProvider.Factory = { application ->
            viewModelFactory {
                initializer {
                    val db = androidx.room.Room.databaseBuilder(
                        application,
                        cl.smartvolt.asistencia.data.local.AppDatabase::class.java,
                        "smartvolt_attendance.db"
                    ).addMigrations(cl.smartvolt.asistencia.data.local.AppDatabase.MIGRATION_2_3)
                        .fallbackToDestructiveMigration().build()
                    OvertimeViewModel(db.overtimeApprovalDao())
                }
            }
        }
    }
}
