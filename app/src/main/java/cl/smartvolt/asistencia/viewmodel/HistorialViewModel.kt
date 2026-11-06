package cl.smartvolt.asistencia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.modelo.WorkerRole
import cl.smartvolt.asistencia.util.Sesion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.obtener(application).asistenciaDao()
    private val cuenta = Sesion.cuenta

    private val workerId = cuenta?.workerId ?: Asistencia.WORKER_ID
    private val siteId = cuenta?.siteId ?: Asistencia.SITE_ID

    val marcas: StateFlow<List<Asistencia>> = if (cuenta?.role == WorkerRole.WORKER) {
        dao.observarPorTrabajador(workerId)
    } else {
        dao.observarPorFaena(siteId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val etiqueta: String = if (cuenta?.role == WorkerRole.WORKER) {
        "Trabajador $workerId"
    } else {
        "${cuenta?.roleLabel ?: "Faena"} - $siteId"
    }
}
