package cl.smartvolt.asistencia.native.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Implementación real de [LocationProvider] usando LocationManager.
 *
 * No usa Google Play Services para no agregar dependencias al proyecto.
 * Escucha GPS y red, toma la primera lectura disponible y limpia los
 * listeners siempre, incluso cuando vence el tiempo de espera.
 */
class AndroidLocationProvider(
    private val context: Context
) : LocationProvider {

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    override fun isLocationServiceEnabled(): Boolean {
        val manager = locationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult {
        val manager = locationManager
            ?: return LocationResult(
                status = LocationStatus.UNAVAILABLE,
                errorCode = "LOCATION_MANAGER_UNAVAILABLE"
            )

        if (!hasPermission()) {
            return LocationResult(
                status = LocationStatus.PERMISSION_DENIED,
                errorCode = "PERMISSION_DENIED"
            )
        }

        if (!isLocationServiceEnabled()) {
            return LocationResult(
                status = LocationStatus.LOCATION_DISABLED,
                errorCode = "LOCATION_DISABLED"
            )
        }

        val enabledProviders = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }

        if (enabledProviders.isEmpty()) {
            return LocationResult(
                status = LocationStatus.UNAVAILABLE,
                errorCode = "NO_PROVIDER_ENABLED"
            )
        }

        val freshLocation = try {
            awaitFirstLocation(manager, enabledProviders)
        } catch (cancelled: CancellationException) {
            // La cancelación no es un error de ubicación: se propaga para que
            // viewModelScope pueda cerrar la corrutina correctamente.
            throw cancelled
        } catch (security: SecurityException) {
            return LocationResult(
                status = LocationStatus.PERMISSION_DENIED,
                errorCode = "PERMISSION_DENIED"
            )
        } catch (error: Exception) {
            return LocationResult(
                status = LocationStatus.ERROR,
                errorCode = error.javaClass.simpleName
            )
        }

        val location = freshLocation
            ?: lastKnownLocation(manager, enabledProviders)?.takeIf { isRecent(it) }

        if (location == null) {
            return LocationResult(
                status = LocationStatus.TIMEOUT,
                errorCode = "LOCATION_TIMEOUT"
            )
        }

        return LocationResult(
            status = LocationStatus.READY,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
            capturedAtMillis = location.time,
            mockSignal = mockSignalOf(location)
        )
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(
        manager: LocationManager,
        providers: List<String>
    ): Location? = providers
        .mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull { it.time }

    @SuppressLint("MissingPermission")
    private suspend fun awaitFirstLocation(
        manager: LocationManager,
        providers: List<String>
    ): Location? = withTimeoutOrNull(TIMEOUT_MILLIS) {
        suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { manager.removeUpdates(this) }
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }

                    @Deprecated("Requerido por versiones antiguas de Android")
                    override fun onStatusChanged(
                        provider: String?,
                        status: Int,
                        extras: Bundle?
                    ) = Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) = Unit
                }

                // Se instala antes de pedir lecturas: si el registro de un
                // proveedor falla, los listeners ya agregados igual se limpian.
                continuation.invokeOnCancellation {
                    runCatching { manager.removeUpdates(listener) }
                }

                try {
                    providers.forEach { provider ->
                        manager.requestLocationUpdates(
                            provider,
                            0L,
                            0f,
                            listener,
                            Looper.getMainLooper()
                        )
                    }
                } catch (error: Exception) {
                    runCatching { manager.removeUpdates(listener) }
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            }
        }

    private fun isRecent(location: Location): Boolean {
        val ageMillis = System.currentTimeMillis() - location.time
        return ageMillis in 0..MAX_LAST_KNOWN_AGE_MILLIS
    }

    private fun mockSignalOf(location: Location): MockSignal {
        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
        return if (isMock) MockSignal.DETECTED else MockSignal.NOT_DETECTED
    }

    private companion object {
        /** Tiempo máximo de espera de una lectura nueva antes de usar la última conocida. */
        const val TIMEOUT_MILLIS = 15_000L

        /**
         * Antigüedad máxima aceptada para la última ubicación conocida.
         *
         * Es un límite técnico provisional para no presentar una lectura vieja
         * como si fuera actual. No es una regla de negocio aprobada y puede
         * ajustarse cuando el equipo defina la política de evidencia.
         */
        const val MAX_LAST_KNOWN_AGE_MILLIS = 120_000L
    }
}
