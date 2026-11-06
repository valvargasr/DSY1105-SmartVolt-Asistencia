package cl.smartvolt.asistencia.feature.worksite

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.smartvolt.asistencia.data.local.AppDatabaseProvider
import cl.smartvolt.asistencia.data.local.WorksiteRepository
import cl.smartvolt.asistencia.domain.validation.WorksiteErrors
import cl.smartvolt.asistencia.domain.validation.WorksiteInput
import cl.smartvolt.asistencia.domain.validation.WorksiteValidation
import cl.smartvolt.asistencia.domain.validation.WorksiteValidator
import cl.smartvolt.asistencia.feature.attendance.PresenceEvaluator
import cl.smartvolt.asistencia.native.location.AndroidLocationProvider
import cl.smartvolt.asistencia.native.location.LocationProvider
import cl.smartvolt.asistencia.native.location.LocationStatus
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FaenaAdminUiState(
    val nombre: String = "",
    val direccion: String = "",
    val radio: String = "",
    val latitud: String = "",
    val longitud: String = "",
    val errores: WorksiteErrors = WorksiteErrors(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val ubicacionEnCurso: Boolean = false,
    val mensaje: String? = null,
    val guardado: Boolean = false
) {
    val latitudPreview: Double? get() = latitud.replace(',', '.').toDoubleOrNull()
    val longitudPreview: Double? get() = longitud.replace(',', '.').toDoubleOrNull()
}

/**
 * Edita el nombre, la dirección y el perímetro de la faena activa.
 *
 * Solo la pantalla del administrador crea este ViewModel. La validación vive
 * en [WorksiteValidator] y el guardado en [WorksiteRepository].
 */
class FaenaAdminViewModel(
    private val repository: WorksiteRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaenaAdminUiState())
    val uiState: StateFlow<FaenaAdminUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            val faena = withContext(Dispatchers.IO) { repository.actual() }
            _uiState.update {
                it.copy(
                    nombre = faena.name,
                    direccion = faena.address,
                    radio = faena.radiusMeters.toString(),
                    latitud = formato(faena.latitude),
                    longitud = formato(faena.longitude),
                    cargando = false
                )
            }
        }
    }

    fun onNombreChange(valor: String) {
        _uiState.update {
            it.copy(nombre = valor, errores = it.errores.copy(name = ""), mensaje = null)
        }
    }

    fun onDireccionChange(valor: String) {
        _uiState.update {
            it.copy(direccion = valor, errores = it.errores.copy(address = ""), mensaje = null)
        }
    }

    fun onRadioChange(valor: String) {
        if (!valor.all { it.isDigit() }) return
        _uiState.update {
            it.copy(radio = valor, errores = it.errores.copy(radiusMeters = ""), mensaje = null)
        }
    }

    fun onLatitudChange(valor: String) {
        _uiState.update {
            it.copy(latitud = valor, errores = it.errores.copy(latitude = ""), mensaje = null)
        }
    }

    fun onLongitudChange(valor: String) {
        _uiState.update {
            it.copy(longitud = valor, errores = it.errores.copy(longitude = ""), mensaje = null)
        }
    }

    fun onUsarUbicacionActual() {
        if (_uiState.value.ubicacionEnCurso) return

        viewModelScope.launch {
            _uiState.update { it.copy(ubicacionEnCurso = true, mensaje = null) }

            try {
                val resultado = locationProvider.getCurrentLocation()
                val latitud = resultado.latitude
                val longitud = resultado.longitude

                if (resultado.status == LocationStatus.READY && latitud != null && longitud != null) {
                    _uiState.update {
                        it.copy(
                            latitud = formato(latitud),
                            longitud = formato(longitud),
                            errores = it.errores.copy(latitude = "", longitude = ""),
                            mensaje = "Se usó la ubicación actual del teléfono."
                        )
                    }
                } else {
                    _uiState.update { it.copy(mensaje = PresenceEvaluator.locationMessage(resultado)) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(mensaje = "No se pudo leer la ubicación. Intenta de nuevo.")
                }
            } finally {
                _uiState.update { it.copy(ubicacionEnCurso = false) }
            }
        }
    }

    fun onGuardar() {
        val estado = _uiState.value
        if (estado.guardando) return

        val entrada = WorksiteInput(
            name = estado.nombre,
            address = estado.direccion,
            radiusMeters = estado.radio,
            latitude = estado.latitud,
            longitude = estado.longitud
        )

        when (val resultado = WorksiteValidator.validate(entrada)) {
            is WorksiteValidation.Invalid -> {
                _uiState.update { it.copy(errores = resultado.errors, mensaje = null) }
            }

            is WorksiteValidation.Valid -> {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(guardando = true, errores = WorksiteErrors(), mensaje = null)
                    }

                    try {
                        val datos = resultado.data
                        withContext(Dispatchers.IO) {
                            val actual = repository.actual()
                            repository.guardar(
                                actual.copy(
                                    name = datos.name,
                                    address = datos.address,
                                    latitude = datos.latitude,
                                    longitude = datos.longitude,
                                    radiusMeters = datos.radiusMeters
                                )
                            )
                        }
                        _uiState.update {
                            it.copy(guardando = false, guardado = true, mensaje = "Faena actualizada.")
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        _uiState.update {
                            it.copy(
                                guardando = false,
                                mensaje = "No se pudo guardar la faena. Intenta de nuevo."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun formato(valor: Double): String = String.format(Locale.US, "%.6f", valor)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as? Application
                    ?: error("FaenaAdminViewModel requiere el contexto de la aplicación.")
                FaenaAdminViewModel(
                    repository = WorksiteRepository(
                        AppDatabaseProvider.get(application).worksiteDao()
                    ),
                    locationProvider = AndroidLocationProvider(application)
                )
            }
        }
    }
}
