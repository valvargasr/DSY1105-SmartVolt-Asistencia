package cl.smartvolt.asistencia.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// Muestra avisos locales cuando se guarda una marca
class Notificacion(private val context: Context) {

    @SuppressLint("MissingPermission")
    // Avisa que se registro la entrada
    fun avisarEntrada() = avisar("Entrada registrada")

    @SuppressLint("MissingPermission")
    // Avisa que se registro la salida
    fun avisarSalida() = avisar("Salida registrada")

    @SuppressLint("MissingPermission")
    // Arma y muestra la notificacion
    private fun avisar(titulo: String) {
        // un aviso simple cuando se guarda una marca, sin robar la pantalla
        val permiso = Permisos.permisoNotificaciones()
        if (permiso != null && !Permisos.estaConcedido(context, permiso)) return

        crearCanal()

        val aviso = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText("Marca guardada en este dispositivo. Pendiente de sincronización.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(ID, aviso)
        }
    }

    // Crea el canal de notificaciones en Android 8 o superior
    private fun crearCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL,
            "Asistencia",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos de registro de asistencia"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager ?: return
        manager.createNotificationChannel(canal)
    }

    // Datos fijos del canal y del aviso
    private companion object {
        const val CANAL = "asistencia"
        const val ID = 1001
    }
}
