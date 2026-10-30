# Área C — Recursos nativos y flujo de marcación: Plan de implementación

> **Para quien ejecute:** usar `superpowers:executing-plans` tarea por tarea. Los pasos están con checkbox (`- [ ]`).

**Objetivo:** entregar permisos, GPS real, escaneo BLE real, notificación local y una pantalla de marcación con persistencia local mínima, en la rama `feature/location-permissions`.

**Arquitectura:** MVVM con `StateFlow`. Las clases en `native/` no conocen Compose; el ViewModel las orquesta; la pantalla solo observa estado y dispara acciones.

**Stack:** Kotlin 2.2.10, Compose BOM 2026.02.01, Material 3, `LocationManager`, `BluetoothLeScanner`, `SharedPreferences` + `org.json`. Sin dependencias nuevas.

**Rama base:** `feature/location-permissions` (creada desde `origin/joaquin`, que es la única con proyecto Android).

---

## Estructura de archivos

| Archivo | Responsabilidad |
|---|---|
| `app/src/main/AndroidManifest.xml` | Declarar permisos y `uses-feature` BLE |
| `native/permissions/PermissionStatus.kt` | Enum de estado de permiso |
| `native/permissions/PermissionChecker.kt` | Consultar permisos según API |
| `native/location/LocationResult.kt` | Contrato GPS |
| `native/location/LocationProvider.kt` | Interfaz |
| `native/location/AndroidLocationProvider.kt` | Implementación con `LocationManager` |
| `native/bluetooth/BeaconScanResult.kt` | Contrato BLE |
| `native/bluetooth/BeaconConfig.kt` | UUID de prueba y ventana de escaneo |
| `native/bluetooth/BeaconScanner.kt` | Interfaz |
| `native/bluetooth/AndroidBeaconScanner.kt` | Implementación con `BluetoothLeScanner` |
| `native/notifications/AttendanceNotifier.kt` | Canal y notificación local |
| `data/local/LocalAttendanceEvent.kt` | Modelo de evento local |
| `data/local/AttendanceLocalStore.kt` | Interfaz de almacenamiento |
| `data/local/SharedPreferencesAttendanceStore.kt` | Implementación JSON |
| `feature/attendance/PresenciaUiState.kt` | Estado de pantalla |
| `feature/attendance/PresenceEvaluator.kt` | Lógica pura de mensajes y habilitación |
| `feature/attendance/PresenciaViewModel.kt` | Orquestación |
| `feature/attendance/PresenciaScreen.kt` | UI Compose |
| `MainActivity.kt` | Ruta `presencia` y botón de acceso demo |
| `app/src/test/.../PresenceEvaluatorTest.kt` | Pruebas unitarias |
| `docs/pruebas-area-c.md` | Matriz de pruebas manuales |

---

## Tarea 0: Verificar entorno de compilación

**Archivos:** ninguno.

- [ ] **Paso 1:** instalar SDK y JDK

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-37.0" "build-tools;37.0.0"
```

- [ ] **Paso 2:** compilar la rama sin cambios

```powershell
$env:JAVA_HOME="C:\Users\juako\jdk\jdk-17.0.x"
.\gradlew.bat assembleDebug
```

Esperado: `BUILD SUCCESSFUL`. Si falla por JDK, usar el JDK 17 descargado, no el 24.

- [ ] **Paso 3:** commit (solo si hubo ajustes de entorno versionables; normalmente no hay).

---

## Tarea 1: Contratos nativos

**Archivos:**
- Crear: `app/src/main/java/cl/smartvolt/asistencia/native/location/LocationResult.kt`
- Crear: `app/src/main/java/cl/smartvolt/asistencia/native/bluetooth/BeaconScanResult.kt`
- Crear: `app/src/main/java/cl/smartvolt/asistencia/native/permissions/PermissionStatus.kt`

- [ ] **Paso 1:** escribir `LocationResult.kt`

```kotlin
package cl.smartvolt.asistencia.native.location

enum class LocationStatus {
    READY,
    PERMISSION_DENIED,
    LOCATION_DISABLED,
    TIMEOUT,
    INSUFFICIENT_ACCURACY,
    UNAVAILABLE,
    ERROR,
}

enum class MockSignal { DETECTED, NOT_DETECTED, UNKNOWN }

data class LocationResult(
    val status: LocationStatus,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val capturedAtMillis: Long? = null,
    val mockSignal: MockSignal = MockSignal.UNKNOWN,
    val errorCode: String? = null,
)
```

- [ ] **Paso 2:** escribir `BeaconScanResult.kt`

```kotlin
package cl.smartvolt.asistencia.native.bluetooth

enum class BeaconScanStatus {
    FOUND,
    NOT_FOUND,
    PERMISSION_DENIED,
    BLUETOOTH_OFF,
    NOT_SUPPORTED,
    TIMEOUT,
    ERROR,
}

data class BeaconScanResult(
    val status: BeaconScanStatus,
    val beaconId: String? = null,
    val rssi: Int? = null,
    val capturedAtMillis: Long? = null,
    val errorCode: String? = null,
)
```

- [ ] **Paso 3:** escribir `PermissionStatus.kt`

```kotlin
package cl.smartvolt.asistencia.native.permissions

enum class PermissionStatus { NOT_REQUESTED, GRANTED, DENIED, PERMANENTLY_DENIED }
```

- [ ] **Paso 4:** commit

```bash
git add app/src/main/java/cl/smartvolt/asistencia/native
git commit -m "feat(attendance): agrega contratos de ubicacion y beacon"
```

---

## Tarea 2: Permisos en el manifest y checker

**Archivos:**
- Modificar: `app/src/main/AndroidManifest.xml`
- Crear: `native/permissions/PermissionChecker.kt`

- [ ] **Paso 1:** agregar permisos antes de `<application>`

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission
    android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />
```

- [ ] **Paso 2:** escribir `PermissionChecker.kt`

```kotlin
package cl.smartvolt.asistencia.native.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionChecker(private val context: Context) {

    fun locationPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    fun scanPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun notificationPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

    fun requiredPermissions(): Array<String> =
        (locationPermissions() + scanPermissions() + notificationPermissions()).distinct().toTypedArray()

    fun hasLocationPermission(): Boolean = granted(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasScanPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            granted(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            granted(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
```

- [ ] **Paso 3:** compilar

```powershell
.\gradlew.bat assembleDebug
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Paso 4:** commit

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/cl/smartvolt/asistencia/native/permissions
git commit -m "feat(location): declara permisos de ubicacion, bluetooth y notificaciones"
```

---

## Tarea 3: Proveedor de GPS

**Archivos:**
- Crear: `native/location/LocationProvider.kt`
- Crear: `native/location/AndroidLocationProvider.kt`

- [ ] **Paso 1:** interfaz

```kotlin
package cl.smartvolt.asistencia.native.location

interface LocationProvider {
    suspend fun getCurrentLocation(timeoutMillis: Long = 15_000L): LocationResult
}
```

- [ ] **Paso 2:** implementación

Puntos obligatorios:
- Verificar permiso antes de tocar `LocationManager`.
- `isLocationEnabled` en API 28+; en versiones menores revisar proveedores.
- Timeout con `withTimeoutOrNull`.
- `requestSingleUpdate` sobre GPS y respaldo de red.
- Mapear `isMock` (API 31+) o `isFromMockProvider` a `MockSignal`.
- Nunca devolver coordenadas inventadas.

- [ ] **Paso 3:** compilar

- [ ] **Paso 4:** commit `feat(location): agrega lectura de ubicacion real`

---

## Tarea 4: Escáner BLE

**Archivos:**
- Crear: `native/bluetooth/BeaconConfig.kt`
- Crear: `native/bluetooth/BeaconScanner.kt`
- Crear: `native/bluetooth/AndroidBeaconScanner.kt`

- [ ] **Paso 1:** `BeaconConfig.kt`

```kotlin
package cl.smartvolt.asistencia.native.bluetooth

object BeaconConfig {
    // UUID de prueba emitido por el segundo telefono (nRF Connect / Beacon Simulator).
    // PROVISIONAL: reemplazar por el UUID autorizado cuando la contraparte lo entregue.
    const val TEST_SERVICE_UUID: String = "6f1b2c3d-4e5f-4a6b-8c9d-0e1f2a3b4c5d"
    const val SCAN_WINDOW_MILLIS: Long = 10_000L
    const val MIN_RSSI: Int? = null // sin umbral configurado
}
```

- [ ] **Paso 2:** interfaz `BeaconScanner`

- [ ] **Paso 3:** implementación

Puntos obligatorios:
- `BluetoothManager` nulo → `NOT_SUPPORTED`.
- `adapter.isEnabled == false` → `BLUETOOTH_OFF`.
- `bluetoothLeScanner` nulo → `NOT_SUPPORTED`.
- Filtro por `TEST_SERVICE_UUID`.
- `ScanSettings.SCAN_MODE_LOW_LATENCY`.
- Timeout con `withTimeoutOrNull` → `TIMEOUT`.
- `onScanFailed` → `ERROR` con el código.
- `SecurityException` → `PERMISSION_DENIED`.
- Si `MIN_RSSI` es nulo se acepta la primera coincidencia; si tiene valor, se exige `rssi >= MIN_RSSI`.
- `beaconId` = UUID del servicio o, si no viene, la dirección del dispositivo.
- Siempre `stopScan` al terminar o cancelar.

- [ ] **Paso 4:** commit `feat(ble): agrega escaneo real de baliza con identificador y RSSI`

---

## Tarea 5: Persistencia local y notificaciones

**Archivos:**
- Crear: `data/local/LocalAttendanceEvent.kt`
- Crear: `data/local/AttendanceLocalStore.kt`
- Crear: `data/local/SharedPreferencesAttendanceStore.kt`
- Crear: `native/notifications/AttendanceNotifier.kt`

- [ ] **Paso 1:** modelo y contrato

```kotlin
data class LocalAttendanceEvent(
    val id: String,
    val type: String,
    val timestampMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val beaconId: String?,
    val rssi: Int?,
    val mockSignal: String,
    val syncState: String,
)
```

```kotlin
interface AttendanceLocalStore {
    fun save(event: LocalAttendanceEvent)
    fun pending(): List<LocalAttendanceEvent>
    fun count(): Int
}
```

- [ ] **Paso 2:** implementación con `SharedPreferences` y `JSONArray`, tolerante a JSON corrupto (si falla, devuelve lista vacía).

- [ ] **Paso 3:** `AttendanceNotifier` con canal `asistencia` y `NotificationCompat`; verificar `POST_NOTIFICATIONS` en API 33+ antes de notificar.

- [ ] **Paso 4:** commit `feat(attendance): persiste eventos locales y notifica el resultado`

---

## Tarea 6: Lógica pura y pruebas

**Archivos:**
- Crear: `feature/attendance/PresenceEvaluator.kt`
- Crear: `app/src/test/java/cl/smartvolt/asistencia/feature/attendance/PresenceEvaluatorTest.kt`

- [ ] **Paso 1:** escribir las pruebas primero (deben fallar)

Casos mínimos:
1. Sin resultados → no se puede registrar.
2. GPS listo + baliza encontrada → se puede registrar.
3. GPS listo + baliza no encontrada → no se puede registrar.
4. Permiso GPS denegado → mensaje de permiso.
5. Bluetooth apagado → mensaje para activar Bluetooth.
6. GPS con mock detectado → el mensaje advierte revisión, no acusa fraude.
7. Precisión disponible → el mensaje incluye el valor en metros.

- [ ] **Paso 2:** ejecutar y ver el fallo

```powershell
.\gradlew.bat testDebugUnitTest --tests "*PresenceEvaluatorTest*"
```

- [ ] **Paso 3:** implementar `PresenceEvaluator` con `when` exhaustivos sobre los enums.

- [ ] **Paso 4:** ejecutar y ver el paso

- [ ] **Paso 5:** commit `test(attendance): agrega casos de evaluacion de presencia`

---

## Tarea 7: ViewModel

**Archivos:**
- Crear: `feature/attendance/PresenciaUiState.kt`
- Crear: `feature/attendance/PresenciaViewModel.kt`

- [ ] **Paso 1:** estado

```kotlin
data class PresenciaUiState(
    val permissionState: PermissionStatus = PermissionStatus.NOT_REQUESTED,
    val location: LocationResult? = null,
    val beacon: BeaconScanResult? = null,
    val isChecking: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val lastEvent: LocalAttendanceEvent? = null,
    val pendingSyncCount: Int = 0,
)
```

- [ ] **Paso 2:** ViewModel

Acciones: `onVerificarPresencia()`, `onRegistrarEntrada()`, `onPermisosResultado(concedidos, denegadoPermanente)`, `onDescartarMensaje()`.

Reglas:
- `onVerificarPresencia` lanza GPS y BLE en paralelo con `async`.
- Mientras corre, `isChecking = true` y botones deshabilitados.
- Al guardar: `id = UUID.randomUUID().toString()`, `syncState = "PENDING_SYNC"`, `type = "CHECK_IN"`.
- Tras guardar: actualizar `pendingSyncCount`, `lastEvent` y notificar.
- Factory con `viewModelFactory { initializer { ... } }`.

- [ ] **Paso 3:** compilar y commit `feat(attendance): agrega viewmodel de verificacion de presencia`

---

## Tarea 8: Pantalla Compose

**Archivos:**
- Crear: `feature/attendance/PresenciaScreen.kt`
- Modificar: `MainActivity.kt`

- [ ] **Paso 1:** pantalla con:
  - Tarjeta explicativa de permisos con botón "Permitir".
  - Tarjeta Ubicación: ícono, título, mensaje y detalle (precisión y hora).
  - Tarjeta Baliza: ícono, título, mensaje, identificador y RSSI.
  - Botón "Verificar presencia" (deshabilitado mientras `isChecking`).
  - Botón "Registrar entrada" (habilitado solo si `canRegister`).
  - Resultado con `AnimatedVisibility`.
  - Chip "Pendiente de sincronización: N".
  - `rememberLauncherForActivityResult(RequestMultiplePermissions())`.
  - Si se deniega dos veces → botón "Abrir ajustes" con `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)`.

- [ ] **Paso 2:** agregar la ruta `"presencia"` al `NavHost` y un botón de acceso demo en `PantallaLogin` ("Entrar como OP-001 — demo"), sin tocar `LoginViewModel`.

- [ ] **Paso 3:** compilar, ejecutar y commit `feat(attendance): agrega pantalla de verificacion de presencia`

---

## Tarea 9: Documentación y cierre

**Archivos:**
- Crear: `docs/pruebas-area-c.md`

- [ ] **Paso 1:** tabla de pruebas manuales con columnas: escenario, pasos, resultado esperado, resultado obtenido, fecha, dispositivo.
- [ ] **Paso 2:** documentar modelo y versión de Android del teléfono usado.
- [ ] **Paso 3:** nota de limitaciones: sin backend, sin Room, UUID provisional.
- [ ] **Paso 4:** commit `docs(area-c): agrega matriz de pruebas de recursos nativos`

---

## Verificación final

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Esperado: pruebas en verde y APK generado en `app/build/outputs/apk/debug/`.

## Riesgos abiertos

- Sin credenciales de Git configuradas no se puede hacer push; se entregan los comandos.
- La baliza física no existe: se usa un segundo teléfono como emisor, pendiente de aprobación.
- `main` no tiene proyecto: hay que mergear antes de la entrega formal.
