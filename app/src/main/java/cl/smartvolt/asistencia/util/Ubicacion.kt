package cl.smartvolt.asistencia.util

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

// Estados posibles de la lectura de ubicacion
enum class EstadoUbicacion {
    LISTA,
    SIN_PERMISO,
    UBICACION_APAGADA,
    TIEMPO_AGOTADO,
    NO_DISPONIBLE,
    ERROR
}

// Datos que devuelve una lectura de GPS
data class ResultadoUbicacion(
    val estado: EstadoUbicacion,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val capturadaMillis: Long? = null,
    val simulada: Boolean = false
)

// Obtiene la ubicacion del telefono con un tiempo limite
class Ubicacion(private val context: Context) {

    private val manager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    // Revisa si la app puede leer la ubicacion
    fun tienePermiso(): Boolean = Permisos.ubicacionConcedida(context)

    // Revisa si el GPS esta encendido
    fun estaActiva(): Boolean {
        val servicio = manager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            servicio.isLocationEnabled
        } else {
            servicio.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                servicio.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    @SuppressLint("MissingPermission")
    // Devuelve una lectura lista o el motivo del fallo
    suspend fun obtener(): ResultadoUbicacion {
        // primero permisos y GPS, después esperamos una lectura de verdad
        val servicio = manager ?: return ResultadoUbicacion(EstadoUbicacion.NO_DISPONIBLE)
        if (!tienePermiso()) return ResultadoUbicacion(EstadoUbicacion.SIN_PERMISO)
        if (!estaActiva()) return ResultadoUbicacion(EstadoUbicacion.UBICACION_APAGADA)

        val proveedores = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { runCatching { servicio.isProviderEnabled(it) }.getOrDefault(false) }

        if (proveedores.isEmpty()) return ResultadoUbicacion(EstadoUbicacion.NO_DISPONIBLE)

        val lectura = try {
            esperarLectura(servicio, proveedores)
                ?: ultimaConocida(servicio, proveedores)?.takeIf { esReciente(it) }
        } catch (cancelado: CancellationException) {
            throw cancelado
        } catch (error: Exception) {
            return ResultadoUbicacion(EstadoUbicacion.ERROR)
        }

        if (lectura == null) return ResultadoUbicacion(EstadoUbicacion.TIEMPO_AGOTADO)

        return ResultadoUbicacion(
            estado = EstadoUbicacion.LISTA,
            latitude = lectura.latitude,
            longitude = lectura.longitude,
            accuracyMeters = if (lectura.hasAccuracy()) lectura.accuracy else null,
            capturadaMillis = lectura.time,
            simulada = esSimulada(lectura)
        )
    }

    @SuppressLint("MissingPermission")
    // Busca la ultima ubicacion guardada por el telefono
    private fun ultimaConocida(
        servicio: LocationManager,
        proveedores: List<String>
    ): Location? = proveedores
        .mapNotNull { runCatching { servicio.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }

    @SuppressLint("MissingPermission")
    // Espera una lectura nueva con un maximo de 15 segundos
    private suspend fun esperarLectura(
        servicio: LocationManager,
        proveedores: List<String>
    ): Location? = withTimeoutOrNull(TIEMPO_ESPERA_MILLIS) {
        // si no llega nada en 15 segundos cortamos y probamos con la última conocida
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { servicio.removeUpdates(this) }
                    if (continuation.isActive) continuation.resume(location)
                }

                @Deprecated("Requerido por versiones antiguas de Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) = Unit
            }

            continuation.invokeOnCancellation {
                runCatching { servicio.removeUpdates(listener) }
            }

            try {
                proveedores.forEach { proveedor ->
                    servicio.requestLocationUpdates(
                        proveedor,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            } catch (error: Exception) {
                runCatching { servicio.removeUpdates(listener) }
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    }

    // Revisa si la ultima ubicacion no es muy vieja
    private fun esReciente(location: Location): Boolean {
        val edad = System.currentTimeMillis() - location.time
        return edad in 0..EDAD_MAXIMA_MILLIS
    }

    // Detecta si la ubicacion viene de una app falsa
    private fun esSimulada(location: Location): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }

    // Tiempos usados al esperar la ubicacion
    private companion object {
        const val TIEMPO_ESPERA_MILLIS = 15_000L
        const val EDAD_MAXIMA_MILLIS = 120_000L
    }
}
