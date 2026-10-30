# Pruebas de recursos nativos — Área C (Joaquín)

Documento de apoyo para la Evaluación Parcial 2 de DSY1105. Registra qué se implementó,
qué se puede probar y con qué resultado. **No contiene datos reales, identificadores
confidenciales ni coordenadas de faenas reales.**

---

## 1. Qué cubre este módulo

| Recurso nativo | Implementación | Archivo principal |
|---|---|---|
| Ubicación GPS | `LocationManager`, lectura única con timeout de 15 s | `native/location/AndroidLocationProvider.kt` |
| Bluetooth LE | `BluetoothLeScanner` con filtro por UUID de servicio | `native/bluetooth/AndroidBeaconScanner.kt` |
| Permisos | Solicitud en contexto + rechazo + bloqueo permanente | `native/permissions/PermissionChecker.kt` |
| Notificaciones | Canal `asistencia`, aviso de entrada guardada | `native/notifications/AttendanceNotifier.kt` |

La pantalla que los integra es `feature/attendance/PresenciaScreen.kt`, con su
`PresenciaViewModel` y las reglas de presentación en `PresenceEvaluator.kt`.

### Contratos que se entregan al resto del equipo

```kotlin
data class LocationResult(
    val status: LocationStatus,          // READY, PERMISSION_DENIED, LOCATION_DISABLED,
                                         // TIMEOUT, INSUFFICIENT_ACCURACY, UNAVAILABLE, ERROR
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val capturedAtMillis: Long? = null,
    val mockSignal: MockSignal = MockSignal.UNKNOWN,   // DETECTED, NOT_DETECTED, UNKNOWN
    val errorCode: String? = null
)

data class BeaconScanResult(
    val status: BeaconScanStatus,        // FOUND, NOT_FOUND, PERMISSION_DENIED, BLUETOOTH_OFF,
                                         // NOT_SUPPORTED, TIMEOUT, ERROR
    val beaconId: String? = null,
    val rssi: Int? = null,
    val capturedAtMillis: Long? = null,
    val errorCode: String? = null
)
```

Ninguno de los dos lanza excepciones: todos los fallos se devuelven como un `status`.
Así el ViewModel y la UI no necesitan `try/catch`.

---

## 2. Pruebas automatizadas (ejecutadas)

`app/src/test/java/cl/smartvolt/asistencia/feature/attendance/PresenceEvaluatorTest.kt`

| Resultado | Detalle |
|---|---|
| 15 pruebas | 15 aprobadas, 0 fallidas |
| Comando | `.\gradlew.bat testDebugUnitTest` |
| Qué valida | Que no se pueda registrar sin GPS y baliza; que cada error tenga mensaje propio; que una ubicación simulada genere advertencia de revisión y no una acusación de fraude; que el resultado diga "pendiente de sincronización" y nunca "validada" |

Estas pruebas cubren la lógica de decisión, no el hardware. El hardware se prueba a mano
con la matriz de la sección 4.

---

## 3. Cómo probar en el teléfono

### 3.1 Preparar el teléfono

1. Activar **Opciones de desarrollador** y **Depuración USB**.
2. Conectar por USB y autorizar el equipo.
3. Verificar que el teléfono aparece: `adb devices`.
4. Instalar: `.\gradlew.bat installDebug`.
5. Activar **Ubicación** y **Bluetooth** en el teléfono.

### 3.2 Preparar el emisor BLE de prueba

No se dispone de una baliza física autorizada, así que se usa un **segundo teléfono Android
como emisor**. Es un sustituto provisional y está registrado como consulta al profesor.

1. Instalar **nRF Connect for Mobile** (Nordic Semiconductor) en el segundo teléfono.
2. Abrir la pestaña **Advertiser**.
3. Agregar a los datos de advertisement un **Service UUID**:
   `6f1b2c3d-4e5f-4a6b-8c9d-0e1f2a3b4c5d`
4. Iniciar la emisión.

> El UUID anterior es **provisional, inventado para el laboratorio**. No corresponde a ninguna
> baliza real de la empresa. Cuando la contraparte entregue el listado ficticio de UUIDs
> autorizados, se reemplaza el valor en `BeaconConfig.TEST_SERVICE_UUID` y no se toca nada más.

Si el escaneo no encuentra el emisor, revisar en este orden: que el UUID esté en los datos de
**advertisement** (no solo en scan response), que el Bluetooth esté encendido en ambos
teléfonos, y que la app tenga el permiso de escaneo concedido.

### 3.3 Recorrido en la app

Bienvenida → **Comenzar** → **Entrar como OP-001 — demo** → pantalla **Registrar entrada**.
El botón de demo existe solo para no depender del login, que pertenece a otra área.

---

## 4. Matriz de pruebas en dispositivo físico

Marcar el resultado real al ejecutar. Una fila sin marcar es una prueba pendiente, no una
prueba aprobada.

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| PT-06.1 | Permiso de ubicación denegado | Pulsar Verificar presencia y negar el permiso | La tarjeta de permisos queda en ámbar, se explica qué falta y se puede reintentar | Pendiente |
| PT-06.2 | Permiso bloqueado permanentemente | Negar dos veces, o bloquear desde ajustes, y volver | Aparece el botón **Abrir ajustes de la app** | Pendiente |
| PT-06.3 | Lectura GPS válida | Conceder permiso, ubicación activa, cielo abierto | Estado READY, coordenadas y precisión en metros | Pendiente |
| PT-06.4 | Ubicación del sistema apagada | Apagar Ubicación y verificar | Mensaje "La ubicación del teléfono está apagada", sin coordenadas inventadas | Pendiente |
| PT-06.5 | Sin lectura de GPS | Probar bajo techo o en subterráneo | Tras 15 s se informa el problema; si existe última lectura conocida se usa esa | Pendiente |
| PT-06.6 | Ubicación simulada | Activar una app de ubicación ficticia y verificar | Advertencia "La marca quedará en revisión"; la app no acusa fraude | Pendiente |
| PT-07.1 | Bluetooth apagado | Apagar Bluetooth y verificar | Mensaje "El Bluetooth está apagado" | Pendiente |
| PT-07.2 | Baliza cerca | Emisor a menos de 1 m | FOUND con UUID de prueba y RSSI (valor negativo, típicamente entre -40 y -70 dBm) | Pendiente |
| PT-07.3 | Baliza ausente | Apagar el emisor y verificar | NOT_FOUND tras 10 s, con instrucción de acercarse e intentar de nuevo | Pendiente |
| PT-07.4 | Permiso de escaneo denegado | En Android 12+, negar el permiso de dispositivos cercanos | Estado PERMISSION_DENIED con explicación | Pendiente |
| PT-07.5 | Teléfono sin BLE | No aplica en teléfonos modernos | Estado NOT_SUPPORTED | No aplicable |
| PT-08 | Registro exitoso | GPS READY + baliza FOUND, pulsar Registrar entrada | Se guarda el evento, mensaje "pendiente de sincronización" y el contador sube en 1 | Pendiente |
| PT-09 | Doble toque | Pulsar Registrar entrada dos veces rápido | Se guarda un solo evento: el botón se deshabilita mientras guarda | Pendiente |
| PT-10 | Registro bloqueado | Solo GPS, sin baliza | El botón Registrar entrada está deshabilitado y se explica por qué | Pendiente |
| PT-11 | Persistencia local | Registrar, cerrar la app por completo, reabrir | El contador de pendientes conserva el valor | Pendiente |
| PT-12 | Notificación | Android 13+, conceder permiso de notificaciones, registrar | Aparece la notificación "Entrada registrada" | Pendiente |
| PT-13 | Notificación sin permiso | Android 13+, negar permiso de notificaciones, registrar | Se guarda igual, sin notificación y sin error | Pendiente |

### Bitácora de hardware

Completar con lo que realmente se probó. No inventar filas.

| Campo | Valor |
|---|---|
| Modelo de teléfono | *(pendiente)* |
| Versión de Android | *(pendiente)* |
| Nivel de API | *(pendiente)* |
| Fecha de la prueba | *(pendiente)* |
| Emisor BLE usado | *(pendiente: segundo teléfono con nRF Connect)* |
| Firmware o app del emisor | *(pendiente)* |

---

## 5. Decisiones de diseño y sus motivos

**Por qué no hay umbral de precisión ni de RSSI.** El anexo y el ERS piden validar presencia,
pero no fijan valores aceptables. Inventar "máximo 20 m" o "RSSI mínimo -70" sería una regla
falsa presentada como aprobada. `BeaconConfig.MIN_RSSI` es `null`: se acepta la primera
coincidencia y la UI muestra la señal real para que el equipo decida después, con el profesor,
qué umbral corresponde.

**Por qué el `beaconId` es el UUID y no la dirección MAC.** Leer `device.address` exige el
permiso `BLUETOOTH_CONNECT`, que no se necesita para escanear y que habría que justificar
aparte. El UUID del servicio identifica la baliza de prueba sin pedir permisos adicionales y
además no es un identificador físico del dispositivo.

**Por qué SharedPreferences y no Room.** Room pertenece al Área B y todavía no está integrado.
La interfaz `AttendanceLocalStore` deja el reemplazo como un cambio de una sola clase cuando
exista el repositorio real.

**Por qué no se afirma que la marca está validada.** Sin backend no hay validación del
servidor. Todo evento nace como `PENDING_SYNC` y la interfaz lo dice con esas palabras.

**Sobre detección de ubicación simulada.** Android informa la señal cuando el sistema la
conoce, pero eso no prueba ni descarta manipulación. Se trata como indicio para revisión, tal
como pide el ERS, y nunca como veredicto.

---

## 6. Limitaciones conocidas

1. **No hay baliza física autorizada.** El escaneo está probado con un emisor simulado. La
   lectura BLE real contra una baliza de la empresa queda pendiente de que la contraparte
   entregue el dispositivo y su UUID de prueba.
2. **Emulador no sirve para BLE.** El emulador de Android no emite ni recibe Bluetooth LE real,
   así que la prueba de PT-07 tiene que ser en teléfono físico.
3. **Sin sincronización remota.** El evento queda local. Retrofit y el backend son fase
   posterior según la guía del equipo.
4. **Sin pantalla de salida todavía.** Este módulo registra entrada. La salida depende de la
   regla de orden que implementa el Área B.
5. **Identidad fija de demostración.** `OP-001` y `FAENA-DEMO-01` son constantes de prueba; la
   sesión real la define el Área B.
6. **El UUID de la baliza es provisional.** Debe reemplazarse por el que entregue la
   contraparte antes de presentar como si fuera el autorizado.

---

## 7. Consultas abiertas para el profesor o la contraparte

| Tema | Pregunta |
|---|---|
| Baliza física | ¿Se dispondrá de una baliza BLE real con UUID de prueba para el hito? |
| Alternativa | ¿Se acepta un segundo teléfono emitiendo BLE como sustituto para la demostración? |
| Umbral | ¿Qué precisión GPS y qué RSSI mínimo se consideran aceptables? |
| Ubicación simulada | ¿Qué comprobación espera el profesor que se muestre en la demo? |
| Teléfono | ¿Hay un modelo o versión de Android exigido para la presentación? |

Estas consultas deben quedar también como tarjetas en la lista **Bloqueado / consultar** del
tablero Trello, con responsable y fecha.
