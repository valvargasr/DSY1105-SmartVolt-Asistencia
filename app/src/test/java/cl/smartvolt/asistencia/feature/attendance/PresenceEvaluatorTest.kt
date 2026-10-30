package cl.smartvolt.asistencia.feature.attendance

import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanResult
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanStatus
import cl.smartvolt.asistencia.native.location.LocationResult
import cl.smartvolt.asistencia.native.location.LocationStatus
import cl.smartvolt.asistencia.native.location.MockSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresenceEvaluatorTest {

    private fun ubicacionLista(
        precision: Float = 8f,
        mock: MockSignal = MockSignal.NOT_DETECTED
    ) = LocationResult(
        status = LocationStatus.READY,
        latitude = -33.4489,
        longitude = -70.6693,
        accuracyMeters = precision,
        capturedAtMillis = 1_700_000_000_000L,
        mockSignal = mock
    )

    private fun balizaEncontrada(rssi: Int = -62) = BeaconScanResult(
        status = BeaconScanStatus.FOUND,
        beaconId = "uuid-de-prueba",
        rssi = rssi,
        capturedAtMillis = 1_700_000_000_000L
    )

    @Test
    fun `sin resultados no se puede registrar`() {
        assertFalse(PresenceEvaluator.canRegister(null, null))
    }

    @Test
    fun `ubicacion lista y baliza encontrada permiten registrar`() {
        assertTrue(
            PresenceEvaluator.canRegister(
                ubicacionLista(),
                balizaEncontrada()
            )
        )
    }

    @Test
    fun `ubicacion lista sin baliza no permite registrar`() {
        assertFalse(
            PresenceEvaluator.canRegister(
                ubicacionLista(),
                BeaconScanResult(status = BeaconScanStatus.NOT_FOUND)
            )
        )
    }

    @Test
    fun `baliza encontrada sin ubicacion valida no permite registrar`() {
        assertFalse(
            PresenceEvaluator.canRegister(
                LocationResult(status = LocationStatus.PERMISSION_DENIED),
                balizaEncontrada()
            )
        )
    }

    @Test
    fun `permiso denegado se explica al usuario`() {
        val mensaje = PresenceEvaluator.locationMessage(
            LocationResult(status = LocationStatus.PERMISSION_DENIED)
        )

        assertTrue(mensaje.contains("permiso", ignoreCase = true))
    }

    @Test
    fun `ubicacion apagada se explica al usuario`() {
        val mensaje = PresenceEvaluator.locationMessage(
            LocationResult(status = LocationStatus.LOCATION_DISABLED)
        )

        assertTrue(mensaje.contains("apagada", ignoreCase = true))
    }

    @Test
    fun `tiempo agotado del gps se informa sin inventar coordenadas`() {
        val mensaje = PresenceEvaluator.locationMessage(
            LocationResult(status = LocationStatus.TIMEOUT)
        )

        assertTrue(mensaje.contains("GPS", ignoreCase = true))
    }

    @Test
    fun `la precision disponible se muestra en metros`() {
        val mensaje = PresenceEvaluator.locationMessage(ubicacionLista(precision = 7f))

        assertTrue(mensaje.contains("7 m"))
    }

    @Test
    fun `bluetooth apagado se explica al usuario`() {
        val mensaje = PresenceEvaluator.beaconMessage(
            BeaconScanResult(status = BeaconScanStatus.BLUETOOTH_OFF)
        )

        assertTrue(mensaje.contains("Bluetooth", ignoreCase = true))
    }

    @Test
    fun `baliza no detectada no se presenta como error tecnico`() {
        val mensaje = PresenceEvaluator.beaconMessage(
            BeaconScanResult(status = BeaconScanStatus.NOT_FOUND)
        )

        assertTrue(mensaje.contains("No se detect", ignoreCase = true))
    }

    @Test
    fun `baliza encontrada muestra el rssi`() {
        val mensaje = PresenceEvaluator.beaconMessage(balizaEncontrada(rssi = -71))

        assertTrue(mensaje.contains("-71"))
    }

    @Test
    fun `ubicacion simulada genera advertencia de revision`() {
        val advertencia = PresenceEvaluator.mockWarning(
            ubicacionLista(mock = MockSignal.DETECTED)
        )

        assertNotNull(advertencia)
        assertTrue(advertencia!!.contains("revisión", ignoreCase = true))
        assertFalse(advertencia.contains("fraude", ignoreCase = true))
    }

    @Test
    fun `sin senal de simulacion no hay advertencia`() {
        assertNull(PresenceEvaluator.mockWarning(ubicacionLista()))
        assertNull(PresenceEvaluator.mockWarning(null))
    }

    @Test
    fun `el resultado nunca afirma validacion del servidor`() {
        val evento = LocalAttendanceEvent(
            id = "evento-1",
            type = LocalAttendanceEvent.TYPE_CHECK_IN,
            timestampMillis = 1_700_000_000_000L,
            latitude = -33.4489,
            longitude = -70.6693,
            accuracyMeters = 8f,
            beaconId = "uuid-de-prueba",
            rssi = -62
        )

        val mensaje = PresenceEvaluator.resultMessage(evento)

        assertTrue(mensaje.contains("pendiente de sincronización", ignoreCase = true))
        assertFalse(mensaje.contains("validada", ignoreCase = true))
        assertEquals(LocalAttendanceEvent.PENDING_SYNC, evento.syncState)
    }

    @Test
    fun `el contador de pendientes se describe en texto`() {
        assertTrue(
            PresenceEvaluator.pendingSyncMessage(0).contains("Sin registros")
        )
        assertTrue(
            PresenceEvaluator.pendingSyncMessage(3).contains("3")
        )
    }
}
