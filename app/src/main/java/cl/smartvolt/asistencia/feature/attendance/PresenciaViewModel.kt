package cl.smartvolt.asistencia.feature.attendance

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.smartvolt.asistencia.data.local.AttendanceLocalStore
import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.data.local.SharedPreferencesAttendanceStore
import cl.smartvolt.asistencia.native.bluetooth.AndroidBeaconScanner
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanner
import cl.smartvolt.asistencia.native.location.AndroidLocationProvider
import cl.smartvolt.asistencia.native.location.LocationProvider
import cl.smartvolt.asistencia.native.location.LocationStatus
import cl.smartvolt.asistencia.native.notifications.AttendanceNotifier
import cl.smartvolt.asistencia.native.permissions.PermissionStatus
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Coordina permisos, lectura de ubicación, escaneo BLE y guardado local.
 *
 * La pantalla solo observa [uiState]; no ejecuta lógica de negocio.
 */
class PresenciaViewModel(
    private val locationProvider: LocationProvider,
    private val beaconScanner: BeaconScanner,
    private val store: AttendanceLocalStore,
    private val notifier: AttendanceNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(PresenciaUiState())
    val uiState: StateFlow<PresenciaUiState> = _uiState.asStateFlow()

    init {
        // El contador se lee fuera del hilo principal: leer SharedPreferences y
        // parsear el JSON en el constructor bloquearía la composición.
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                runCatching { store.count() }.getOrDefault(0)
            }
            _uiState.update { it.copy(pendingSyncCount = count) }
        }
    }

    fun onVerificarPresencia() {
        if (_uiState.value.isChecking) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isChecking = true,
                    message = null,
                    location = null,
                    beacon = null
                )
            }

            try {
                val locationDeferred = async { locationProvider.getCurrentLocation() }
                val beaconDeferred = async { beaconScanner.scan() }

                val location = locationDeferred.await()
                val beacon = beaconDeferred.await()

                _uiState.update { state ->
                    state.copy(
                        location = location,
                        beacon = beacon,
                        permissionState = when {
                            // Si hubo lectura, el permiso está concedido sin importar lo que
                            // se haya registrado antes (por ejemplo, si se habilitó en ajustes).
                            location.status == LocationStatus.READY ->
                                PermissionStatus.GRANTED
                            location.status == LocationStatus.PERMISSION_DENIED ->
                                PermissionStatus.DENIED
                            else -> state.permissionState
                        }
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(message = "No se pudo verificar la presencia. Intenta de nuevo.")
                }
            } finally {
                _uiState.update { it.copy(isChecking = false) }
            }
        }
    }

    fun onRegistrarEntrada() {
        val state = _uiState.value
        if (state.isSaving) return
        if (!state.canRegister) {
            _uiState.update {
                it.copy(
                    message = "Primero verifica la presencia: se necesita ubicación y baliza."
                )
            }
            return
        }

        val location = state.location ?: return
        val beacon = state.beacon ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val event = LocalAttendanceEvent(
                    id = UUID.randomUUID().toString(),
                    type = LocalAttendanceEvent.TYPE_CHECK_IN,
                    timestampMillis = System.currentTimeMillis(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracyMeters,
                    beaconId = beacon.beaconId,
                    rssi = beacon.rssi,
                    mockSignal = location.mockSignal,
                    syncState = LocalAttendanceEvent.PENDING_SYNC
                )

                val pendingCount = withContext(Dispatchers.IO) {
                    store.save(event)
                    store.count()
                }

                notifier.notifyCheckInSaved()

                _uiState.update {
                    it.copy(
                        lastEvent = event,
                        pendingSyncCount = pendingCount,
                        message = PresenceEvaluator.resultMessage(event)
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        message = "No se pudo guardar la entrada en este dispositivo. " +
                            "Intenta de nuevo."
                    )
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun onPermisosResultado(concedidos: Boolean, denegadoPermanente: Boolean) {
        _uiState.update {
            it.copy(
                permissionState = when {
                    concedidos -> PermissionStatus.GRANTED
                    denegadoPermanente -> PermissionStatus.PERMANENTLY_DENIED
                    else -> PermissionStatus.DENIED
                }
            )
        }
    }

    fun onDescartarMensaje() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as? Application
                    ?: error("PresenciaViewModel requiere el contexto de la aplicación.")
                PresenciaViewModel(
                    locationProvider = AndroidLocationProvider(application),
                    beaconScanner = AndroidBeaconScanner(application),
                    store = SharedPreferencesAttendanceStore(application),
                    notifier = AttendanceNotifier(application)
                )
            }
        }
    }
}
