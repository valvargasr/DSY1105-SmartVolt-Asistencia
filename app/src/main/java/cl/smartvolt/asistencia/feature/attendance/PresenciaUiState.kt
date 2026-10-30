package cl.smartvolt.asistencia.feature.attendance

import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanResult
import cl.smartvolt.asistencia.native.location.LocationResult
import cl.smartvolt.asistencia.native.permissions.PermissionStatus

/** Estado observable de la pantalla de marcación. */
data class PresenciaUiState(
    val permissionState: PermissionStatus = PermissionStatus.NOT_REQUESTED,
    val location: LocationResult? = null,
    val beacon: BeaconScanResult? = null,
    val isChecking: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val lastEvent: LocalAttendanceEvent? = null,
    val pendingSyncCount: Int = 0
) {
    val locationMessage: String
        get() = PresenceEvaluator.locationMessage(location)

    val beaconMessage: String
        get() = PresenceEvaluator.beaconMessage(beacon)

    val canRegister: Boolean
        get() = PresenceEvaluator.canRegister(location, beacon)

    val mockWarning: String?
        get() = PresenceEvaluator.mockWarning(location)

    val needsSettings: Boolean
        get() = permissionState == PermissionStatus.PERMANENTLY_DENIED
}
