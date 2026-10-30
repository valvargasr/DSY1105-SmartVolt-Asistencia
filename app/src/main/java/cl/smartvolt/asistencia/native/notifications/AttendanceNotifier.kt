package cl.smartvolt.asistencia.native.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.smartvolt.asistencia.native.permissions.PermissionChecker

/**
 * Notificación local que confirma que la marca quedó guardada en el teléfono.
 *
 * Aclara siempre que el registro está pendiente de sincronización: la app no
 * puede afirmar que el servidor lo validó cuando todavía no hubo conexión.
 */
class AttendanceNotifier(
    private val context: Context
) {

    private var channelCreated = false

    // El permiso se comprueba en canNotify() antes de llegar aquí y el notify va
    // dentro de runCatching: si el usuario lo revoca en medio, no se cierra la app.
    @SuppressLint("MissingPermission")
    fun notifyCheckInSaved() {
        if (!canNotify()) return

        createChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Entrada registrada")
            .setContentText("Guardada en este dispositivo. Pendiente de sincronización.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun canNotify(): Boolean {
        val permission = PermissionChecker.notificationPermission() ?: return true
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (channelCreated) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Asistencia",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos de registro de asistencia en este dispositivo"
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager ?: return
        manager.createNotificationChannel(channel)
        channelCreated = true
    }

    private companion object {
        const val CHANNEL_ID = "asistencia"
        const val NOTIFICATION_ID = 1001
    }
}
