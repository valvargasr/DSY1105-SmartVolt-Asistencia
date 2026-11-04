package cl.smartvolt.asistencia.feature.attendance

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.smartvolt.asistencia.data.local.AttendanceLocalStore
import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.data.local.RoomAttendanceStore
import cl.smartvolt.asistencia.domain.validation.AttendanceValidator
import cl.smartvolt.asistencia.native.bluetooth.AndroidBeaconScanner
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanner
import cl.smartvolt.asistencia.native.location.AndroidLocationProvider
import cl.smartvolt.asistencia.native.location.LocationProvider
import cl.smartvolt.asistencia.native.location.LocationStatus
import cl.smartvolt.asistencia.native.notifications.AttendanceNotifier
import cl.smartvolt.asistencia.native.permissions.PermissionStatus
import java.time.Instant
import java.time.ZoneId
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
 * La pantalla solo observa [uiState]; no ejecuta lógica de negocio. El
 * trabajador y la faena vienen de la sesión activa, no de constantes.
 */
class PresenciaViewModel(
    private val locationProvider: LocationProvider,
    private val beaconScanner: BeaconScanner,
    private val store: AttendanceLocalStore,
    private val notifier: AttendanceNotifier,
    private val workerId: String = LocalAttendanceEvent.WORKER_ID,
    private val siteId: String = LocalAttendanceEvent.SITE_ID
) : ViewModel() {

    private val _uiState = MutableStateFlow(PresenciaUiState())
    val uiState: StateFlow<PresenciaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                store.count() to AttendanceValidator.dayState(
                    events = store.all(),
                    nowMillis = now,
                    workerId = workerId
                )
            }
            _uiState.update {
                it.copy(
                    pendingSyncCount = snapshot.first,
                    dayState = snapshot.second
                )
            }
        }
    }

    fun onVerificarPresencia() {
        if (_uiState.value.isChecking) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isChecking = true,
                    message = null,
                    duplicateNotice = null,
                    location = null,
                    beacon = null
                )
            }

            try {
                val locationDeferred = async { locationProvider.getCurrentLocation() }
                val beaconDeferred = async { beaconScanner.scan() }

                val location = locationDeferred.await()
                val beacon = beaconDeferred.await()
                val dayState = withContext(Dispatchers.IO) {
                    AttendanceValidator.dayState(
                        events = store.all(),
                        nowMillis = location.capturedAtMillis ?: System.currentTimeMillis(),
                        workerId = workerId
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        location = location,
                        beacon = beacon,
                        dayState = dayState,
                        permissionState = when {
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
                    message = "Primero verifica la presencia: se necesita ubicación y baliza.",
                    duplicateNotice = null
                )
            }
            return
        }

        val location = state.location ?: return
        val beacon = state.beacon ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, duplicateNotice = null) }

            try {
                val now = System.currentTimeMillis()
                val timestamp = location.capturedAtMillis ?: now
                val events = withContext(Dispatchers.IO) { store.all() }
                val dayState = AttendanceValidator.dayState(
                    events = events,
                    nowMillis = timestamp,
                    workerId = workerId
                )
                val validation = AttendanceValidator.canRegisterCheckIn(
                    alreadyCheckedIn = dayState.hasCheckIn
                )
                if (validation is AttendanceValidator.ValidationResult.Invalid) {
                    val existente = primeraMarcaDelDia(
                        events,
                        LocalAttendanceEvent.TYPE_CHECK_IN,
                        timestamp
                    )
                    _uiState.update {
                        it.copy(
                            dayState = dayState,
                            message = null,
                            duplicateNotice = DuplicateNotice(
                                title = "Entrada ya registrada",
                                detail = existente?.let { millis ->
                                    "Ya registraste tu entrada hoy a las ${formatHour(millis)}. " +
                                        "Puedes registrar la salida cuando termines tu turno."
                                } ?: "Ya existe una entrada registrada para la jornada de hoy.",
                                type = LocalAttendanceEvent.TYPE_CHECK_IN
                            )
                        )
                    }
                    return@launch
                }

                val event = LocalAttendanceEvent(
                    id = UUID.randomUUID().toString(),
                    type = LocalAttendanceEvent.TYPE_CHECK_IN,
                    timestampMillis = timestamp,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracyMeters,
                    beaconId = beacon.beaconId,
                    rssi = beacon.rssi,
                    mockSignal = location.mockSignal,
                    workerId = workerId,
                    siteId = siteId,
                    syncState = LocalAttendanceEvent.PENDING_SYNC
                )

                val (pendingCount, updatedDayState) = withContext(Dispatchers.IO) {
                    store.save(event)
                    store.count() to AttendanceValidator.dayState(
                        events = store.all(),
                        nowMillis = timestamp,
                        workerId = workerId
                    )
                }

                notifier.notifyCheckInSaved()

                _uiState.update {
                    it.copy(
                        lastEvent = event,
                        pendingSyncCount = pendingCount,
                        dayState = updatedDayState,
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

    fun onRegistrarSalida() {
        val state = _uiState.value
        if (state.isSaving) return
        if (!state.canRegister) {
            _uiState.update {
                it.copy(
                    message = "Primero verifica la presencia: se necesita ubicación y baliza.",
                    duplicateNotice = null
                )
            }
            return
        }

        val location = state.location ?: return
        val beacon = state.beacon ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, duplicateNotice = null) }

            try {
                val now = System.currentTimeMillis()
                val timestamp = location.capturedAtMillis ?: now
                val events = withContext(Dispatchers.IO) { store.all() }
                val dayState = AttendanceValidator.dayState(
                    events = events,
                    nowMillis = timestamp,
                    workerId = workerId
                )

                if (dayState.hasCheckOut) {
                    val existente = primeraMarcaDelDia(
                        events,
                        LocalAttendanceEvent.TYPE_CHECK_OUT,
                        timestamp
                    )
                    _uiState.update {
                        it.copy(
                            dayState = dayState,
                            message = null,
                            duplicateNotice = DuplicateNotice(
                                title = "Salida ya registrada",
                                detail = existente?.let { millis ->
                                    "Ya registraste tu salida hoy a las ${formatHour(millis)}. " +
                                        "La jornada de hoy está cerrada."
                                } ?: "Ya existe una salida registrada para la jornada de hoy.",
                                type = LocalAttendanceEvent.TYPE_CHECK_OUT
                            )
                        )
                    }
                    return@launch
                }

                val validation = AttendanceValidator.canRegisterCheckOut(
                    hasOpenCheckIn = dayState.hasOpenCheckIn,
                    alreadyCheckedOut = dayState.hasCheckOut
                )
                if (validation is AttendanceValidator.ValidationResult.Invalid) {
                    _uiState.update {
                        it.copy(message = validation.reason, dayState = dayState)
                    }
                    return@launch
                }

                val event = LocalAttendanceEvent(
                    id = UUID.randomUUID().toString(),
                    type = LocalAttendanceEvent.TYPE_CHECK_OUT,
                    timestampMillis = timestamp,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracyMeters,
                    beaconId = beacon.beaconId,
                    rssi = beacon.rssi,
                    mockSignal = location.mockSignal,
                    workerId = workerId,
                    siteId = siteId,
                    syncState = LocalAttendanceEvent.PENDING_SYNC
                )

                val (pendingCount, updatedDayState) = withContext(Dispatchers.IO) {
                    store.save(event)
                    store.count() to AttendanceValidator.dayState(
                        events = store.all(),
                        nowMillis = timestamp,
                        workerId = workerId
                    )
                }

                notifier.notifyCheckOutSaved()

                _uiState.update {
                    it.copy(
                        lastEvent = event,
                        pendingSyncCount = pendingCount,
                        dayState = updatedDayState,
                        message = "Salida registrada a las ${formatHour(event.timestampMillis)}. " +
                            "Guardada en este dispositivo y pendiente de sincronización."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        message = "No se pudo guardar la salida en este dispositivo. " +
                            "Intenta de nuevo."
                    )
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun onDescartarMensaje() {
        _uiState.update { it.copy(message = null) }
    }

    fun onDescartarDuplicado() {
        _uiState.update { it.copy(duplicateNotice = null) }
    }

    private fun primeraMarcaDelDia(
        events: List<LocalAttendanceEvent>,
        type: String,
        timestampMillis: Long
    ): Long? {
        val zone = ZoneId.systemDefault()
        val dia = Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate()
        return events
            .filter { it.workerId == workerId && it.type == type }
            .filter {
                Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate() == dia
            }
            .minOfOrNull { it.timestampMillis }
    }

    private fun formatHour(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }

    companion object {
        fun factory(
            workerId: String = LocalAttendanceEvent.WORKER_ID,
            siteId: String = LocalAttendanceEvent.SITE_ID
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as? Application
                    ?: error("PresenciaViewModel requiere el contexto de la aplicación.")
                PresenciaViewModel(
                    locationProvider = AndroidLocationProvider(application),
                    beaconScanner = AndroidBeaconScanner(application),
                    store = RoomAttendanceStore(application),
                    notifier = AttendanceNotifier(application),
                    workerId = workerId,
                    siteId = siteId
                )
            }
        }

        val Factory: ViewModelProvider.Factory
            get() = factory()
    }
}
