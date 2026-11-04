package cl.smartvolt.asistencia.native.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Escáner BLE real sobre `BluetoothLeScanner`.
 *
 * Acepta iBeacon (iOS **Locate Beacons**), Eddystone-UID y el UUID de servicio
 * de laboratorio. No filtra por un UUID de servicio fijo, porque un iBeacon
 * solo publica datos de fabricante y el filtro lo descartaría.
 */
class AndroidBeaconScanner(private val context: Context) : BeaconScanner {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    override fun isBluetoothEnabled(): Boolean =
        bluetoothManager?.adapter?.isEnabled == true

    override fun isSupported(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    @SuppressLint("MissingPermission")
    override suspend fun scan(): BeaconScanResult {
        if (!isSupported()) {
            return BeaconScanResult(
                status = BeaconScanStatus.NOT_SUPPORTED,
                errorCode = "BLE_NOT_SUPPORTED"
            )
        }

        val manager = bluetoothManager ?: return BeaconScanResult(
            status = BeaconScanStatus.NOT_SUPPORTED,
            errorCode = "BLUETOOTH_MANAGER_UNAVAILABLE"
        )

        val adapter = manager.adapter ?: return BeaconScanResult(
            status = BeaconScanStatus.NOT_SUPPORTED,
            errorCode = "BLUETOOTH_ADAPTER_UNAVAILABLE"
        )

        if (!adapter.isEnabled) {
            return BeaconScanResult(
                status = BeaconScanStatus.BLUETOOTH_OFF,
                errorCode = "BLUETOOTH_OFF"
            )
        }

        if (!hasScanPermission()) {
            return BeaconScanResult(
                status = BeaconScanStatus.PERMISSION_DENIED,
                errorCode = "SCAN_PERMISSION_DENIED"
            )
        }

        val scanner = adapter.bluetoothLeScanner ?: return BeaconScanResult(
            status = BeaconScanStatus.ERROR,
            errorCode = "LE_SCANNER_UNAVAILABLE"
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val resultado = withTimeoutOrNull(BeaconConfig.SCAN_WINDOW_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : ScanCallback() {

                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val baliza = parseBeacon(result) ?: return
                        if (BeaconConfig.MIN_RSSI != null && result.rssi < BeaconConfig.MIN_RSSI!!) {
                            return
                        }
                        if (continuation.isActive) {
                            detenerEscaneo(scanner, this)
                            continuation.resume(
                                BeaconScanResult(
                                    status = BeaconScanStatus.FOUND,
                                    beaconId = baliza.id,
                                    rssi = result.rssi,
                                    capturedAtMillis = System.currentTimeMillis(),
                                    format = baliza.format,
                                    major = baliza.major,
                                    minor = baliza.minor
                                )
                            )
                        }
                    }

                    override fun onBatchScanResults(results: MutableList<ScanResult>) {
                        results.firstOrNull()?.let { onScanResult(0, it) }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        if (continuation.isActive) {
                            detenerEscaneo(scanner, this)
                            continuation.resume(
                                BeaconScanResult(
                                    status = BeaconScanStatus.ERROR,
                                    errorCode = "SCAN_FAILED_$errorCode"
                                )
                            )
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    detenerEscaneo(scanner, callback)
                }

                try {
                    // Sin filtros: se inspeccionan todos los anuncios cercanos y
                    // se acepta el primero que corresponda a un formato conocido.
                    scanner.startScan(null, settings, callback)
                } catch (_: SecurityException) {
                    continuation.resume(
                        BeaconScanResult(
                            status = BeaconScanStatus.PERMISSION_DENIED,
                            errorCode = "SCAN_PERMISSION_DENIED"
                        )
                    )
                } catch (error: Exception) {
                    continuation.resume(
                        BeaconScanResult(
                            status = BeaconScanStatus.ERROR,
                            errorCode = "SCAN_START_FAILED_${error.javaClass.simpleName}"
                        )
                    )
                }
            }
        }

        return resultado ?: if (!adapter.isEnabled) {
            BeaconScanResult(
                status = BeaconScanStatus.BLUETOOTH_OFF,
                errorCode = "BLUETOOTH_OFF_DURING_SCAN"
            )
        } else {
            BeaconScanResult(
                status = BeaconScanStatus.NOT_FOUND,
                errorCode = "BEACON_NOT_FOUND"
            )
        }
    }

    private fun detenerEscaneo(
        scanner: android.bluetooth.le.BluetoothLeScanner,
        callback: ScanCallback
    ) {
        try {
            scanner.stopScan(callback)
        } catch (_: SecurityException) {
            // Si el permiso se revocó a mitad del escaneo no hay nada que hacer.
        }
    }

    /**
     * Interpreta el anuncio BLE. Devuelve `null` si no es un formato conocido.
     */
    private fun parseBeacon(result: ScanResult): ParsedBeacon? {
        val record = result.scanRecord ?: return null

        // 1. iBeacon: datos de fabricante de Apple.
        val apple = record.manufacturerSpecificData?.get(BeaconConfig.IBEACON_COMPANY_ID)
        if (apple != null && apple.size >= 23 &&
            apple[0] == 0x02.toByte() && apple[1] == 0x15.toByte()
        ) {
            val uuid = uuidFrom(apple, 2)
            val esperado = BeaconConfig.EXPECTED_IBEACON_UUID
            if (esperado == null || esperado.equals(uuid, ignoreCase = true)) {
                val major = ((apple[18].toInt() and 0xFF) shl 8) or
                    (apple[19].toInt() and 0xFF)
                val minor = ((apple[20].toInt() and 0xFF) shl 8) or
                    (apple[21].toInt() and 0xFF)
                return ParsedBeacon(
                    format = "iBeacon",
                    id = uuid,
                    major = major,
                    minor = minor
                )
            }
            return null
        }

        // 2. Eddystone-UID: servicio 0xFEAA con trama 0x00.
        val eddystoneUuid = ParcelUuid.fromString(BeaconConfig.EDDYSTONE_SERVICE_UUID).uuid
        val eddystone = record.serviceData?.entries
            ?.firstOrNull { it.key.uuid == eddystoneUuid }
            ?.value
        if (eddystone != null && eddystone.size >= 18 &&
            eddystone[0] == 0x00.toByte()
        ) {
            val namespace = eddystone.copyOfRange(2, 12).toHex()
            val instance = eddystone.copyOfRange(12, 18).toHex()
            return ParsedBeacon(
                format = "Eddystone-UID",
                id = "$namespace-$instance"
            )
        }

        // 3. Compatibilidad con el UUID de servicio de laboratorio.
        val servicio = record.serviceUuids?.firstOrNull {
            it.uuid.toString().equals(BeaconConfig.TEST_SERVICE_UUID, ignoreCase = true)
        }
        if (servicio != null) {
            return ParsedBeacon(
                format = "GATT",
                id = BeaconConfig.TEST_SERVICE_UUID
            )
        }

        return null
    }

    private fun uuidFrom(bytes: ByteArray, offset: Int): String {
        val buffer = ByteBuffer.wrap(bytes, offset, 16)
        return UUID(buffer.long, buffer.long).toString()
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { byte -> String.format(Locale.ROOT, "%02x", byte) }

    private fun hasScanPermission(): Boolean {
        val permiso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(context, permiso) ==
            PackageManager.PERMISSION_GRANTED
    }

    private data class ParsedBeacon(
        val format: String,
        val id: String,
        val major: Int? = null,
        val minor: Int? = null
    )
}
