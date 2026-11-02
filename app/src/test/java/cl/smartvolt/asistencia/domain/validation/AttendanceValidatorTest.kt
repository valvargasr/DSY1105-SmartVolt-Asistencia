package cl.smartvolt.asistencia.domain.validation

import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanResult
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanStatus
import cl.smartvolt.asistencia.native.location.LocationResult
import cl.smartvolt.asistencia.native.location.LocationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AttendanceValidatorTest {

    private val zoneId = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 5, 10)

    private fun at(day: LocalDate, hour: Int): Long =
        day.atTime(hour, 0).atZone(zoneId).toInstant().toEpochMilli()

    private fun marca(
        type: String,
        timestampMillis: Long,
        workerId: String = LocalAttendanceEvent.WORKER_ID
    ) = LocalAttendanceEvent(
        id = "evt-$type-$timestampMillis",
        type = type,
        timestampMillis = timestampMillis,
        workerId = workerId
    )

    @Test
    fun `evento valido pasa la validacion`() {
        val evento = LocalAttendanceEvent(
            id = "evt-1",
            type = LocalAttendanceEvent.TYPE_CHECK_IN,
            timestampMillis = 1_700_000_000_000L,
            latitude = -33.4489,
            longitude = -70.6693,
            accuracyMeters = 8f,
            beaconId = "uuid-de-prueba",
            rssi = -62
        )
        val resultado = AttendanceValidator.validateEvent(evento)
        assertTrue(resultado is AttendanceValidator.ValidationResult.Valid)
    }

    @Test
    fun `evento sin id es invalido`() {
        val evento = LocalAttendanceEvent(
            id = "",
            type = LocalAttendanceEvent.TYPE_CHECK_IN,
            timestampMillis = 1_700_000_000_000L
        )
        val resultado = AttendanceValidator.validateEvent(evento)
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `evento sin trabajador es invalido`() {
        val evento = LocalAttendanceEvent(
            id = "evt-1",
            type = LocalAttendanceEvent.TYPE_CHECK_IN,
            timestampMillis = 1_700_000_000_000L,
            workerId = ""
        )
        val resultado = AttendanceValidator.validateEvent(evento)
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `evento sin faena es invalido`() {
        val evento = LocalAttendanceEvent(
            id = "evt-1",
            type = LocalAttendanceEvent.TYPE_CHECK_IN,
            timestampMillis = 1_700_000_000_000L,
            siteId = ""
        )
        val resultado = AttendanceValidator.validateEvent(evento)
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `tipo de marca desconocido es invalido`() {
        val evento = LocalAttendanceEvent(
            id = "evt-1",
            type = "UNKNOWN",
            timestampMillis = 1_700_000_000_000L
        )
        val resultado = AttendanceValidator.validateEvent(evento)
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `timestamp cero es invalido`() {
        val evento = LocalAttendanceEvent(
            id = "evt-1",
            type = LocalAttendanceEvent.TYPE_CHECK_IN,
            timestampMillis = 0L
        )
        val resultado = AttendanceValidator.validateEvent(evento)
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `salida sin entrada previa es invalida`() {
        val resultado = AttendanceValidator.canRegisterCheckOut(
            hasOpenCheckIn = false,
            alreadyCheckedOut = false
        )
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `salida duplicada es invalida`() {
        val resultado = AttendanceValidator.canRegisterCheckOut(
            hasOpenCheckIn = true,
            alreadyCheckedOut = true
        )
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `salida valida con entrada abierta`() {
        val resultado = AttendanceValidator.canRegisterCheckOut(
            hasOpenCheckIn = true,
            alreadyCheckedOut = false
        )
        assertTrue(resultado is AttendanceValidator.ValidationResult.Valid)
    }

    @Test
    fun `entrada duplicada es invalida`() {
        val resultado = AttendanceValidator.canRegisterCheckIn(alreadyCheckedIn = true)
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `entrada nueva es valida`() {
        val resultado = AttendanceValidator.canRegisterCheckIn(alreadyCheckedIn = false)
        assertTrue(resultado is AttendanceValidator.ValidationResult.Valid)
    }

    @Test
    fun `ubicacion lista es valida`() {
        val resultado = AttendanceValidator.validateLocation(
            LocationResult(status = LocationStatus.READY)
        )
        assertTrue(resultado is AttendanceValidator.ValidationResult.Valid)
    }

    @Test
    fun `permiso denegado es invalido`() {
        val resultado = AttendanceValidator.validateLocation(
            LocationResult(status = LocationStatus.PERMISSION_DENIED)
        )
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `ubicacion null es invalida`() {
        val resultado = AttendanceValidator.validateLocation(null)
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `baliza encontrada es valida`() {
        val resultado = AttendanceValidator.validateBeacon(
            BeaconScanResult(status = BeaconScanStatus.FOUND)
        )
        assertTrue(resultado is AttendanceValidator.ValidationResult.Valid)
    }

    @Test
    fun `baliza no encontrada es invalida`() {
        val resultado = AttendanceValidator.validateBeacon(
            BeaconScanResult(status = BeaconScanStatus.NOT_FOUND)
        )
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `baliza null es invalida`() {
        val resultado = AttendanceValidator.validateBeacon(null)
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `faena vacia es invalida`() {
        val resultado = AttendanceValidator.validateSiteId("")
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `faena valida`() {
        val resultado = AttendanceValidator.validateSiteId("FAENA-DEMO-01")
        assertTrue(resultado is AttendanceValidator.ValidationResult.Valid)
    }

    @Test
    fun `trabajador vacio es invalido`() {
        val resultado = AttendanceValidator.validateWorkerId("")
        assertTrue(resultado is AttendanceValidator.ValidationResult.Invalid)
    }

    @Test
    fun `trabajador valido`() {
        val resultado = AttendanceValidator.validateWorkerId("OP-001")
        assertTrue(resultado is AttendanceValidator.ValidationResult.Valid)
    }

    @Test
    fun `jornada sin marcas permite entrada y no salida`() {
        val estado = AttendanceValidator.dayState(
            events = emptyList(),
            nowMillis = at(today, 8),
            zoneId = zoneId
        )

        assertTrue(estado.canCheckIn)
        assertFalse(estado.canCheckOut)
    }

    @Test
    fun `entrada de hoy habilita la salida y bloquea otra entrada`() {
        val estado = AttendanceValidator.dayState(
            events = listOf(marca(LocalAttendanceEvent.TYPE_CHECK_IN, at(today, 8))),
            nowMillis = at(today, 12),
            zoneId = zoneId
        )

        assertFalse(estado.canCheckIn)
        assertTrue(estado.canCheckOut)
    }

    @Test
    fun `salida de ayer no bloquea la jornada de hoy`() {
        val ayer = today.minusDays(1)
        val estado = AttendanceValidator.dayState(
            events = listOf(
                marca(LocalAttendanceEvent.TYPE_CHECK_IN, at(ayer, 8)),
                marca(LocalAttendanceEvent.TYPE_CHECK_OUT, at(ayer, 18))
            ),
            nowMillis = at(today, 8),
            zoneId = zoneId
        )

        assertTrue(estado.canCheckIn)
        assertFalse(estado.canCheckOut)
    }

    @Test
    fun `jornada con entrada y salida queda cerrada`() {
        val estado = AttendanceValidator.dayState(
            events = listOf(
                marca(LocalAttendanceEvent.TYPE_CHECK_IN, at(today, 8)),
                marca(LocalAttendanceEvent.TYPE_CHECK_OUT, at(today, 18))
            ),
            nowMillis = at(today, 19),
            zoneId = zoneId
        )

        assertFalse(estado.canCheckIn)
        assertFalse(estado.canCheckOut)
    }

    @Test
    fun `marcas de otro trabajador no afectan la jornada`() {
        val estado = AttendanceValidator.dayState(
            events = listOf(
                marca(
                    type = LocalAttendanceEvent.TYPE_CHECK_IN,
                    timestampMillis = at(today, 8),
                    workerId = "OP-999"
                )
            ),
            nowMillis = at(today, 9),
            zoneId = zoneId
        )

        assertTrue(estado.canCheckIn)
        assertFalse(estado.canCheckOut)
    }
}
