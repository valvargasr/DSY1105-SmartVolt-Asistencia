package cl.smartvolt.asistencia.feature.attendance

import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.domain.validation.AttendanceValidator
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanResult
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanStatus
import cl.smartvolt.asistencia.native.location.LocationResult
import cl.smartvolt.asistencia.native.permissions.PermissionStatus

/**
 * Aviso flotante cuando la marca ya existe para la jornada actual.
 *
 * Se muestra como popup sobre la pantalla, no como error de campo, porque el
 * formulario no es inválido: simplemente la acción ya se realizó.
 */
data class DuplicateNotice(
    val title: String,
    val detail: String,
    val type: String
)

/** Estado observable de la pantalla de marcación. */
data class PresenciaUiState(
    val permissionState: PermissionStatus = PermissionStatus.NOT_REQUESTED,
    val location: LocationResult? = null,
    val beacon: BeaconScanResult? = null,
    val isChecking: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val duplicateNotice: DuplicateNotice? = null,
    val lastEvent: LocalAttendanceEvent? = null,
    val pendingSyncCount: Int = 0,
    val dayState: AttendanceValidator.DayAttendanceState = AttendanceValidator.DayAttendanceState()
) {
    val locationMessage: String
        get() = PresenceEvaluator.locationMessage(location)

    val beaconMessage: String
        get() = PresenceEvaluator.beaconMessage(beacon)

    val canRegister: Boolean
        get() = PresenceEvaluator.canRegister(location, beacon)

    val canCheckIn: Boolean
        get() = canRegister && dayState.canCheckIn

    val canCheckOut: Boolean
        get() = canRegister && dayState.canCheckOut

    val alreadyCheckedIn: Boolean
        get() = dayState.hasCheckIn

    val alreadyCheckedOut: Boolean
        get() = dayState.hasCheckOut

    val mockWarning: String?
        get() = PresenceEvaluator.mockWarning(location)

    val needsSettings: Boolean
        get() = permissionState == PermissionStatus.PERMANENTLY_DENIED ||
            beacon?.status == BeaconScanStatus.PERMISSION_DENIED
}
