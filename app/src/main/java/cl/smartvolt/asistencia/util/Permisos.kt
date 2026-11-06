package cl.smartvolt.asistencia.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class EstadoPermiso {
    NO_SOLICITADO,
    CONCEDIDO,
    DENEGADO,
    BLOQUEADO
}

object Permisos {

    private val ubicacion = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun permisosPresencia(): Array<String> {
        val bluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            ubicacion
        }
        return (ubicacion + bluetooth).distinct().toTypedArray()
    }

    fun permisoNotificaciones(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    fun estaConcedido(context: Context, permiso: String): Boolean =
        ContextCompat.checkSelfPermission(context, permiso) ==
            PackageManager.PERMISSION_GRANTED

    fun todosConcedidos(context: Context, permisos: Array<String>): Boolean =
        permisos.all { estaConcedido(context, it) }

    fun ubicacionConcedida(context: Context): Boolean =
        estaConcedido(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            estaConcedido(context, Manifest.permission.ACCESS_COARSE_LOCATION)
}
