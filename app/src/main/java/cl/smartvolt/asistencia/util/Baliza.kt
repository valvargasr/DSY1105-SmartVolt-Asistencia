package cl.smartvolt.asistencia.util

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
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

enum class EstadoBaliza {
    ENCONTRADA,
    NO_ENCONTRADA,
    SIN_PERMISO,
    BLUETOOTH_APAGADO,
    NO_SOPORTADA,
    TIEMPO_AGOTADO,
    ERROR
}

data class ResultadoBaliza(
    val estado: EstadoBaliza,
    val beaconId: String? = null,
    val rssi: Int? = null,
    val formato: String? = null,
    val major: Int? = null,
    val minor: Int? = null
) {
    val idVisible: String
        get() = when {
            beaconId == null -> "Sin datos"
            major != null && minor != null -> "$beaconId ($major/$minor)"
            else -> beaconId
        }
}

class Baliza(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    fun activa(): Boolean = bluetoothManager?.adapter?.isEnabled == true

    fun soportada(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    @SuppressLint("MissingPermission")
    suspend fun escanear(): ResultadoBaliza {
        if (!soportada()) return ResultadoBaliza(EstadoBaliza.NO_SOPORTADA)

        val adapter = bluetoothManager?.adapter
            ?: return ResultadoBaliza(EstadoBaliza.NO_SOPORTADA)

        if (!adapter.isEnabled) return ResultadoBaliza(EstadoBaliza.BLUETOOTH_APAGADO)
        if (!tienePermiso()) return ResultadoBaliza(EstadoBaliza.SIN_PERMISO)

        val scanner = adapter.bluetoothLeScanner
            ?: return ResultadoBaliza(EstadoBaliza.ERROR)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val resultado = withTimeoutOrNull(VENTANA_ESCANEO_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val baliza = interpretar(result) ?: return
                        if (continuation.isActive) {
                            detener(scanner, this)
                            continuation.resume(
                                ResultadoBaliza(
                                    estado = EstadoBaliza.ENCONTRADA,
                                    beaconId = baliza.id,
                                    rssi = result.rssi,
                                    formato = baliza.formato,
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
                            detener(scanner, this)
                            continuation.resume(ResultadoBaliza(EstadoBaliza.ERROR))
                        }
                    }
                }

                continuation.invokeOnCancellation { detener(scanner, callback) }

                try {
                    scanner.startScan(null, settings, callback)
                } catch (_: SecurityException) {
                    continuation.resume(ResultadoBaliza(EstadoBaliza.SIN_PERMISO))
                } catch (_: Exception) {
                    continuation.resume(ResultadoBaliza(EstadoBaliza.ERROR))
                }
            }
        }

        return resultado ?: if (!adapter.isEnabled) {
            ResultadoBaliza(EstadoBaliza.BLUETOOTH_APAGADO)
        } else {
            ResultadoBaliza(EstadoBaliza.NO_ENCONTRADA)
        }
    }

    private fun detener(
        scanner: android.bluetooth.le.BluetoothLeScanner,
        callback: ScanCallback
    ) {
        runCatching { scanner.stopScan(callback) }
    }

    private fun interpretar(result: ScanResult): BalizaEncontrada? {
        val record = result.scanRecord ?: return null

        val apple = record.manufacturerSpecificData?.get(IBEACON_COMPANY_ID)
        if (apple != null && apple.size >= 23 &&
            apple[0] == 0x02.toByte() && apple[1] == 0x15.toByte()
        ) {
            val uuid = uuidDesde(apple, 2)
            val major = ((apple[18].toInt() and 0xFF) shl 8) or (apple[19].toInt() and 0xFF)
            val minor = ((apple[20].toInt() and 0xFF) shl 8) or (apple[21].toInt() and 0xFF)
            return BalizaEncontrada("iBeacon", uuid, major, minor)
        }

        val eddystoneUuid = ParcelUuid.fromString(EDDYSTONE_UUID).uuid
        val eddystone = record.serviceData?.entries
            ?.firstOrNull { it.key.uuid == eddystoneUuid }
            ?.value
        if (eddystone != null && eddystone.size >= 18 && eddystone[0] == 0x00.toByte()) {
            val namespace = eddystone.copyOfRange(2, 12).aHex()
            val instance = eddystone.copyOfRange(12, 18).aHex()
            return BalizaEncontrada("Eddystone-UID", "$namespace-$instance", null, null)
        }

        val servicio = record.serviceUuids?.firstOrNull {
            it.uuid.toString().equals(UUID_LABORATORIO, ignoreCase = true)
        }
        if (servicio != null) {
            return BalizaEncontrada("GATT", UUID_LABORATORIO, null, null)
        }

        return null
    }

    private fun uuidDesde(bytes: ByteArray, offset: Int): String {
        val buffer = ByteBuffer.wrap(bytes, offset, 16)
        return UUID(buffer.long, buffer.long).toString()
    }

    private fun ByteArray.aHex(): String =
        joinToString("") { byte -> String.format(Locale.ROOT, "%02x", byte) }

    private fun tienePermiso(): Boolean {
        val permiso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(context, permiso) ==
            PackageManager.PERMISSION_GRANTED
    }

    private data class BalizaEncontrada(
        val formato: String,
        val id: String,
        val major: Int?,
        val minor: Int?
    )

    companion object {
        const val UUID_LABORATORIO = "6f1b2c3d-4e5f-4a6b-8c9d-0e1f2a3b4c5d"
        const val IBEACON_COMPANY_ID = 0x004C
        const val EDDYSTONE_UUID = "0000feaa-0000-1000-8000-00805f9b34fb"
        const val VENTANA_ESCANEO_MILLIS = 12_000L
    }
}
