package cl.smartvolt.asistencia.session

import android.content.Context
import cl.smartvolt.asistencia.domain.model.WorkerRole

/**
 * Guarda la sesión activa en SharedPreferences.
 *
 * Permite reabrir la app sin volver a escribir las credenciales (RF-02) y
 * recuerda qué rol demo estaba activo. Solo contiene datos ficticios.
 */
class SessionRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("smartvolt_session", Context.MODE_PRIVATE)

    fun current(): DemoAccount? {
        val workerId = prefs.getString(KEY_WORKER_ID, null) ?: return null
        return DemoDirectory.byWorkerId(workerId)
    }

    fun save(account: DemoAccount) {
        prefs.edit()
            .putString(KEY_WORKER_ID, account.workerId)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_WORKER_ID = "worker_id"
    }
}

/** Utilidad para describir el rol en la interfaz. */
fun WorkerRole.isWorker(): Boolean = this == WorkerRole.WORKER
