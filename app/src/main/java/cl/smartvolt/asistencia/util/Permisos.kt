package cl.smartvolt.asistencia.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

// Estados posibles de un permiso del telefono
enum class EstadoPermiso {
    NO_SOLICITADO,
    CONCEDIDO,
    DENEGADO,
    BLOQUEADO
}

// Utilidades para pedir y revisar permisos
object Permisos {

    private val ubicacion = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Permisos necesarios para ubicacion y escaneo de balizas
    fun permisosPresencia(): Array<String> {
        // según la versión de Android cambia lo que hay que pedir para el bluetooth
        val bluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            ubicacion
        }
        return (ubicacion + bluetooth).distinct().toTypedArray()
    }

    // Devuelve el permiso de notificaciones si la version lo necesita
    fun permisoNotificaciones(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    // Revisa si un permiso ya fue concedido
    fun estaConcedido(context: Context, permiso: String): Boolean =
        ContextCompat.checkSelfPermission(context, permiso) ==
            PackageManager.PERMISSION_GRANTED

    // Revisa si estan todos los permisos de una lista
    fun todosConcedidos(context: Context, permisos: Array<String>): Boolean =
        permisos.all { estaConcedido(context, it) }

    // Revisa si hay permiso de ubicacion, fino o aproximado
    fun ubicacionConcedida(context: Context): Boolean =
        estaConcedido(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            estaConcedido(context, Manifest.permission.ACCESS_COARSE_LOCATION)
}
