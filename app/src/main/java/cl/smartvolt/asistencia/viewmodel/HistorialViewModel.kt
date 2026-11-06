package cl.smartvolt.asistencia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.modelo.WorkerRole
import cl.smartvolt.asistencia.util.Sesion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Entrega las marcas segun el rol de la sesion
class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.obtener(application).asistenciaDao()
    private val faenaDao = AppDatabase.obtener(application).faenaDao()
    private val cuenta = Sesion.cuenta

    // Identificadores de la sesion
    private val workerId = cuenta?.workerId ?: Asistencia.WORKER_ID
    private val siteId = cuenta?.siteId ?: Asistencia.SITE_ID

    // el trabajador ve solo sus marcas, el resto ve toda la faena
    val marcas: StateFlow<List<Asistencia>> = if (cuenta?.role == WorkerRole.WORKER) {
        dao.observarPorTrabajador(workerId)
    } else {
        dao.observarPorFaena(siteId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Texto del encabezado segun el rol
    val encabezado: StateFlow<String> = if (cuenta?.role == WorkerRole.WORKER) {
        flowOf("Trabajador $workerId")
    } else {
        faenaDao.observarActiva().map { faena ->
            "${cuenta?.roleLabel ?: "Faena"} - ${faena?.name ?: siteId}"
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = if (cuenta?.role == WorkerRole.WORKER) {
            "Trabajador $workerId"
        } else {
            "${cuenta?.roleLabel ?: "Faena"} - $siteId"
        }
    )
}
