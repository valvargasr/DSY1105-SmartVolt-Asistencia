package cl.smartvolt.asistencia.feature.history

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

/**
 * ViewModel para la pantalla de historial de marcas.
 *
 * Lee las marcas persistidas localmente y las expone a la UI.
 */
class HistoryViewModel(
    private val store: AttendanceLocalStore
) : ViewModel() {

    private val _events = MutableStateFlow<List<LocalAttendanceEvent>>(emptyList())
    val events: StateFlow<List<LocalAttendanceEvent>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        cargarHistorial()
    }

    fun cargarHistorial() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lista = withContext(Dispatchers.IO) {
                    store.all()
                }
                _events.value = lista
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refrescar() {
        cargarHistorial()
    }

    companion object {
        fun factory(store: AttendanceLocalStore): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    HistoryViewModel(store)
                }
            }
    }
}
