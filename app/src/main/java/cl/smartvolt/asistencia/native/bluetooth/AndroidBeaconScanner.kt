package cl.smartvolt.asistencia.native.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Implementación real de [BeaconScanner] con BluetoothLeScanner.
 *
 * Filtra por el UUID de servicio de [BeaconConfig] y siempre detiene el
 * escaneo antes de devolver un resultado, para no dejar la radio encendida.
 */
class AndroidBeaconScanner(
    private val context: Context
) : BeaconScanner {

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

        val adapter = bluetoothManager?.adapter
            ?: return BeaconScanResult(
                status = BeaconScanStatus.NOT_SUPPORTED,
                errorCode = "BLUETOOTH_MANAGER_UNAVAILABLE"
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

        val scanner = adapter.bluetoothLeScanner
            ?: return BeaconScanResult(
                status = BeaconScanStatus.ERROR,
                errorCode = "LE_SCANNER_UNAVAILABLE"
            )

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(BeaconConfig.TEST_SERVICE_UUID))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val found = withTimeoutOrNull(BeaconConfig.SCAN_WINDOW_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : ScanCallback() {

                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val rssi = result.rssi
                        val minimum = BeaconConfig.MIN_RSSI
                        if (minimum != null && rssi < minimum) return

                        runCatching { scanner.stopScan(this) }
                        if (continuation.isActive) {
                            continuation.resume(
                                BeaconScanResult(
                                    status = BeaconScanStatus.FOUND,
                                    beaconId = BeaconConfig.TEST_SERVICE_UUID,
                                    rssi = rssi,
                                    capturedAtMillis = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    override fun onBatchScanResults(results: MutableList<ScanResult>) {
                        results.firstOrNull()?.let { onScanResult(0, it) }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        runCatching { scanner.stopScan(this) }
                        if (continuation.isActive) {
                            continuation.resume(
                                BeaconScanResult(
                                    status = BeaconScanStatus.ERROR,
                                    errorCode = "SCAN_FAILED_$errorCode"
                                )
                            )
                        }
                    }
                }

                try {
                    scanner.startScan(filters, settings, callback)
                } catch (security: SecurityException) {
                    if (continuation.isActive) {
                        continuation.resume(
                            BeaconScanResult(
                                status = BeaconScanStatus.PERMISSION_DENIED,
                                errorCode = "SCAN_PERMISSION_DENIED"
                            )
                        )
                    }
                    return@suspendCancellableCoroutine
                }

                continuation.invokeOnCancellation {
                    runCatching { scanner.stopScan(callback) }
                }
            }
        }

        return found ?: BeaconScanResult(
            status = BeaconScanStatus.NOT_FOUND,
            errorCode = "BEACON_NOT_FOUND"
        )
    }

    private fun hasScanPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
