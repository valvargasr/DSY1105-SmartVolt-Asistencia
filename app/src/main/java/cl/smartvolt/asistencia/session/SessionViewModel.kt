package cl.smartvolt.asistencia.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado de sesión compartido por toda la navegación.
 *
 * La pantalla no decide quién es el usuario: solo observa [session] y muestra
 * las acciones permitidas para ese rol (RNF-01).
 */
class SessionViewModel(
    application: Application,
    private val repository: SessionRepository = SessionRepository(application)
) : AndroidViewModel(application) {

    private val _session = MutableStateFlow(repository.current())
    val session: StateFlow<DemoAccount?> = _session.asStateFlow()

    fun iniciar(account: DemoAccount) {
        repository.save(account)
        _session.value = account
    }

    fun cerrar() {
        repository.clear()
        _session.value = null
    }

    /** Permite actualizar el perfil guardado cuando cambie el rol demo. */
    fun refrescar() {
        _session.value = repository.current()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[
                    ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                ] as Application
                SessionViewModel(application)
            }
        }
    }
}
