package cl.smartvolt.asistencia.domain.validation

import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanResult
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanStatus
import cl.smartvolt.asistencia.native.location.LocationResult
import cl.smartvolt.asistencia.native.location.LocationStatus

/**
 * Validaciones de asistencia desacopladas de la UI (RNF-07, RF-22).
 *
 * Son funciones puras de Kotlin para poder probarlas con tests unitarios
 * sin depender de Android ni de la interfaz.
 */
object AttendanceValidator {

    /** Resultado de una validación. */
    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    /**
     * Valida que un evento de marca tenga los datos mínimos requeridos.
     */
    fun validateEvent(event: LocalAttendanceEvent): ValidationResult {
        if (event.id.isBlank()) {
            return ValidationResult.Invalid("El evento no tiene identificador.")
        }
        if (event.workerId.isBlank()) {
            return ValidationResult.Invalid("Falta el identificador del trabajador.")
        }
        if (event.siteId.isBlank()) {
            return ValidationResult.Invalid("Falta el código de faena.")
        }
        if (event.type != LocalAttendanceEvent.TYPE_CHECK_IN &&
            event.type != LocalAttendanceEvent.TYPE_CHECK_OUT
        ) {
            return ValidationResult.Invalid("Tipo de marca no reconocido.")
        }
        if (event.timestampMillis <= 0) {
            return ValidationResult.Invalid("La marca no tiene fecha/hora válida.")
        }
        return ValidationResult.Valid
    }

    /**
     * Valida que exista una entrada abierta antes de registrar una salida.
     */
    fun canRegisterCheckOut(
        hasOpenCheckIn: Boolean,
        alreadyCheckedOut: Boolean
    ): ValidationResult {
        if (!hasOpenCheckIn) {
            return ValidationResult.Invalid("No hay una entrada registrada para esta jornada.")
        }
        if (alreadyCheckedOut) {
            return ValidationResult.Invalid("Ya existe una salida registrada para esta jornada.")
        }
        return ValidationResult.Valid
    }

    /**
     * Valida que no se duplique una marca del mismo tipo en la misma jornada.
     */
    fun canRegisterCheckIn(alreadyCheckedIn: Boolean): ValidationResult {
        if (alreadyCheckedIn) {
            return ValidationResult.Invalid("Ya existe una entrada registrada para esta jornada.")
        }
        return ValidationResult.Valid
    }

    /**
     * Valida que la ubicación esté lista para registrar.
     */
    fun validateLocation(location: LocationResult?): ValidationResult {
        if (location == null) {
            return ValidationResult.Invalid("No se ha verificado la ubicación.")
        }
        return when (location.status) {
            LocationStatus.READY -> ValidationResult.Valid
            LocationStatus.PERMISSION_DENIED ->
                ValidationResult.Invalid("Falta el permiso de ubicación.")
            LocationStatus.LOCATION_DISABLED ->
                ValidationResult.Invalid("La ubicación del teléfono está apagada.")
            LocationStatus.TIMEOUT ->
                ValidationResult.Invalid("No se obtuvo una lectura de GPS a tiempo.")
            LocationStatus.INSUFFICIENT_ACCURACY ->
                ValidationResult.Invalid("La precisión de la ubicación no es suficiente.")
            LocationStatus.UNAVAILABLE ->
                ValidationResult.Invalid("La ubicación no está disponible.")
            LocationStatus.ERROR ->
                ValidationResult.Invalid("Error al leer la ubicación.")
        }
    }

    /**
     * Valida que la baliza BLE haya sido detectada.
     */
    fun validateBeacon(beacon: BeaconScanResult?): ValidationResult {
        if (beacon == null) {
            return ValidationResult.Invalid("No se ha escaneado la baliza.")
        }
        return when (beacon.status) {
            BeaconScanStatus.FOUND -> ValidationResult.Valid
            BeaconScanStatus.NOT_FOUND ->
                ValidationResult.Invalid("No se detectó la baliza autorizada.")
            BeaconScanStatus.PERMISSION_DENIED ->
                ValidationResult.Invalid("Falta el permiso de Bluetooth.")
            BeaconScanStatus.BLUETOOTH_OFF ->
                ValidationResult.Invalid("El Bluetooth está apagado.")
            BeaconScanStatus.NOT_SUPPORTED ->
                ValidationResult.Invalid("Este teléfono no soporta BLE.")
            BeaconScanStatus.TIMEOUT ->
                ValidationResult.Invalid("El escaneo de la baliza no terminó a tiempo.")
            BeaconScanStatus.ERROR ->
                ValidationResult.Invalid("Error al escanear la baliza.")
        }
    }

    /**
     * Valida el código de faena (no vacío, formato simple).
     */
    fun validateSiteId(siteId: String): ValidationResult {
        if (siteId.isBlank()) {
            return ValidationResult.Invalid("El código de faena no puede estar vacío.")
        }
        if (siteId.length > 32) {
            return ValidationResult.Invalid("El código de faena es demasiado largo.")
        }
        return ValidationResult.Valid
    }

    /**
     * Valida el identificador del trabajador (no vacío).
     */
    fun validateWorkerId(workerId: String): ValidationResult {
        if (workerId.isBlank()) {
            return ValidationResult.Invalid("El identificador del trabajador no puede estar vacío.")
        }
        return ValidationResult.Valid
    }
}
