# Diseño — Área C: recursos nativos y flujo de marcación

- **Fecha:** 2026-10-29
- **Autor:** Joaquín Rojas (Área C)
- **Proyecto:** DSY1105 — SmartVolt Asistencia
- **Estado:** aprobado para implementación

## 1. Contexto

El proyecto necesita registrar entrada y salida de trabajadores en faena con evidencia
de presencia física: ubicación GPS real y detección de una baliza BLE cercana. El Área C
es responsable de los permisos, la lectura de GPS, el escaneo BLE, las notificaciones
locales y la integración de esos resultados con el resto de la aplicación.

El repositorio tiene `main` sin proyecto Android; el código base vive en la rama
`joaquin` (idéntica a `Branch/julian` y `feature/valentina-ui`). Este trabajo parte de
esa rama en `feature/location-permissions`.

## 2. Objetivo

Entregar un módulo nativo funcional y demostrable:

1. Pedir permisos por contexto, con explicación y manejo de rechazo.
2. Obtener una lectura GPS real con metadatos.
3. Escanear una baliza BLE real y reportar identificador y RSSI.
4. Mostrar esos resultados en una pantalla de marcación con estados claros.
5. Persistir el evento localmente como "pendiente de sincronización".
6. Notificar localmente el resultado.

## 3. Fuera de alcance

- Sincronización con backend, Retrofit, Hilt o Spring Boot (fase posterior).
- Cálculo de puntualidad, atrasos, horas extra o feriados (Área B).
- Autenticación y doble factor (Área B).
- Room (Área B). Se deja una interfaz para reemplazar el almacenamiento.
- Detección infalible de fraude. Las señales son indicios, no pruebas.

## 4. Arquitectura

```
app/src/main/java/cl/smartvolt/asistencia/
├── native/
│   ├── location/       LocationResult, LocationProvider, AndroidLocationProvider
│   ├── bluetooth/      BeaconScanResult, BeaconScanner, AndroidBeaconScanner, BeaconConfig
│   ├── permissions/    PermissionStatus, PermissionChecker
│   └── notifications/  AttendanceNotifier
├── feature/attendance/ PresenciaScreen, PresenciaViewModel, PresenciaUiState, PresenceEvaluator
└── data/local/         AttendanceLocalStore, SharedPreferencesAttendanceStore
```

- MVVM: la pantalla observa un `StateFlow`; el ViewModel orquesta permisos, GPS, BLE y
  persistencia; las clases `native/` no conocen Compose.
- Inyección manual mediante un `ViewModelProvider.Factory`, sin Hilt.
- No se modifica `LoginViewModel` ni el tema visual existente.

## 5. Contratos

```kotlin
data class LocationResult(
    val status: LocationStatus,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val capturedAtUtcMillis: Long? = null,
    val mockLocationSignal: MockSignal = MockSignal.UNKNOWN,
    val errorCode: String? = null,
)

enum class LocationStatus { READY, PERMISSION_DENIED, LOCATION_DISABLED, TIMEOUT, INSUFFICIENT_ACCURACY, UNAVAILABLE, ERROR }

data class BeaconScanResult(
    val status: BeaconScanStatus,
    val beaconId: String? = null,
    val rssi: Int? = null,
    val capturedAtUtcMillis: Long? = null,
    val errorCode: String? = null,
)

enum class BeaconScanStatus { FOUND, NOT_FOUND, PERMISSION_DENIED, BLUETOOTH_OFF, NOT_SUPPORTED, TIMEOUT, ERROR }
```

`Instant` requiere API 26; con `minSdk 24` se usa `java.time` mediante desugaring o se
almacena `epochMillis: Long`. **Decisión:** usar `epochMillis: Long` para evitar
dependencias y mantener compatibilidad con API 24.

## 6. Permisos

| Permiso | Uso | Notas |
|---|---|---|
| `ACCESS_FINE_LOCATION` | GPS preciso | Obligatorio |
| `ACCESS_COARSE_LOCATION` | Respaldo | Obligatorio |
| `BLUETOOTH_SCAN` | Escaneo BLE | API 31+; con `neverForLocation` |
| `BLUETOOTH`, `BLUETOOTH_ADMIN` | Escaneo BLE | Solo hasta API 30 |
| `POST_NOTIFICATIONS` | Notificación local | API 33+ |

Flujo: explicación previa → solicitud → si rechaza, mensaje y reintento → si rechaza
definitivamente, botón "Abrir ajustes". Nunca se pide ubicación en segundo plano.

## 7. GPS

- `LocationManager` con proveedor GPS y respaldo de red.
- Timeout de 15 s.
- Devuelve latitud, longitud, precisión y hora UTC del dispositivo.
- `isMock` (API 31+) o `isFromMockProvider` para marcar señal simulada.
- Umbral de precisión **no configurado** por defecto: la UI muestra la precisión
  obtenida y no rechaza por un número inventado.

## 8. BLE

- `BluetoothLeScanner` con ventana de escaneo de 10 s.
- Filtro por UUID de servicio de prueba definido en `BeaconConfig`.
- Se conserva la señal con mejor RSSI.
- `beaconId` = UUID del servicio o dirección del dispositivo, según disponibilidad.
- Sin baliza → `NOT_FOUND`; nunca se fabrican datos.

## 9. Pantalla y estados

`PresenciaUiState`:

```kotlin
data class PresenciaUiState(
    val permissionState: PermissionState,
    val locationState: SignalState,
    val beaconState: SignalState,
    val isChecking: Boolean,
    val isSaving: Boolean,
    val message: String?,
    val lastEvent: LocalAttendanceEvent?,
    val pendingSyncCount: Int,
)
```

Acciones: `onVerificarPresencia()`, `onRegistrarEntrada()`, `onPermitir()`,
`onAbrirAjustes()`, `onDescartarMensaje()`.

Componentes: dos tarjetas de señal con ícono, texto y color (texto siempre presente),
botón principal, resultado y chip "Pendiente de sincronización".

## 10. Persistencia

`AttendanceLocalStore` con implementación `SharedPreferencesAttendanceStore` que guarda
una lista JSON de eventos. Se eligió así para no introducir KSP/Room en esta rama;
Julián puede sustituir la implementación sin cambiar el ViewModel.

## 11. Notificaciones

Canal "Asistencia" creado al iniciar. Se notifica el resultado de la marcación solo si
el permiso está concedido; si no, la app funciona igual.

## 12. Pruebas

Unitarias:
- `PresenceEvaluator`: combinaciones de estados → mensaje y habilitación del botón.
- Mapeo de resultados nativos a estados de UI.

Manuales en dispositivo físico (tabla en `docs/pruebas-area-c.md`):
permiso denegado, GPS apagado, sin baliza, con baliza, Bluetooth apagado, reinicio de app.

## 13. Git

- Rama: `feature/location-permissions` desde `origin/joaquin`.
- Commits: `feat(location):`, `feat(ble):`, `feat(attendance):`, `test:`, `docs:`.
- No se modifica `main` ni se hace push sin credenciales configuradas.

## 14. Riesgos

| Riesgo | Mitigación |
|---|---|
| No hay baliza física para la demo | Segundo teléfono emitiendo BLE con UUID de prueba; documentado como provisional |
| GPS impreciso en interiores | Se muestra la precisión real; no se rechaza con umbrales inventados |
| API 37 / JDK 17 no instalados | Instalar SDK y JDK antes de compilar |
| Conflictos con ramas de A y B | Trabajo aislado en `feature/location-permissions` |

## 15. Pendientes externos

- UUID definitivo de la baliza autorizada (contraparte / profesor).
- Confirmación de que el segundo teléfono como emisor es aceptable.
- Umbral de precisión y RSSI mínimos, si la contraparte los define.
