package cl.smartvolt.asistencia.native.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centraliza qué permisos necesita la app según la versión de Android.
 *
 * Las listas cambian entre versiones: en Android 12+ se pide BLUETOOTH_SCAN y
 * en versiones anteriores el escaneo depende del permiso de ubicación.
 */
object PermissionChecker {

    private val locationPermissions: Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /** Permisos necesarios para leer ubicación y escanear la baliza. */
    fun presencePermissions(): Array<String> {
        val bluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            locationPermissions
        }
        return (locationPermissions + bluetooth).distinct().toTypedArray()
    }

    /** Permiso de notificaciones, obligatorio solo desde Android 13. */
    fun notificationPermission(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    fun allGranted(context: Context, permissions: Array<String>): Boolean =
        permissions.all { isGranted(context, it) }

    fun isLocationGranted(context: Context): Boolean =
        isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
}
