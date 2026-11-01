package cl.smartvolt.asistencia.domain.model

/**
 * Estado de sincronización de una marca con el backend.
 */
enum class SyncState(val code: String) {
    PENDING_SYNC("PENDING_SYNC"),
    SYNCED("SYNCED"),
    REJECTED("REJECTED"),
    REVIEW("REVIEW");

    companion object {
        fun fromCode(code: String): SyncState =
            entries.firstOrNull { it.code == code } ?: PENDING_SYNC
    }
}
