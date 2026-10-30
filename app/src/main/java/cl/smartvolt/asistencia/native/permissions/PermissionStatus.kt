package cl.smartvolt.asistencia.native.permissions

/** Estado del permiso tal como lo ve la aplicación, no solo el sistema. */
enum class PermissionStatus {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}
