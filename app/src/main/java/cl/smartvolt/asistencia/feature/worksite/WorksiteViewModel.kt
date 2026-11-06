package cl.smartvolt.asistencia.feature.worksite

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.smartvolt.asistencia.data.local.AppDatabaseProvider
import cl.smartvolt.asistencia.data.local.WorksiteRepository
import cl.smartvolt.asistencia.domain.model.Worksite
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Expone la faena activa a toda la navegación.
 *
 * La aplicación trabaja con una sola faena editable. Mientras la tabla está
 * vacía se usa la faena por defecto de la demo, sin escribir en la base.
 */
class WorksiteViewModel(repository: WorksiteRepository) : ViewModel() {

    val worksite: StateFlow<Worksite> = repository.observar()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WorksiteRepository.defaultWorksite()
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as? Application
                    ?: error("WorksiteViewModel requiere el contexto de la aplicación.")
                WorksiteViewModel(
                    WorksiteRepository(AppDatabaseProvider.get(application).worksiteDao())
                )
            }
        }
    }
}
