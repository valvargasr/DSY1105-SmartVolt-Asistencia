# Pruebas de recursos nativos (Área C)

Notas de trabajo de la Evaluación Parcial 2 de DSY1105. Aquí queda lo que se implementó,
lo que se puede probar y con qué resultado. No hay datos reales, identificadores
confidenciales ni coordenadas de faenas reales.

## Qué cubre este módulo

| Recurso | Implementación | Archivo |
|---|---|---|
| Ubicación GPS | `LocationManager`, lectura única con timeout de 15 s | `native/location/AndroidLocationProvider.kt` |
| Bluetooth LE | `BluetoothLeScanner` sin filtro de servicio; acepta iBeacon, Eddystone-UID y UUID de laboratorio | `native/bluetooth/AndroidBeaconScanner.kt` |
| Permisos | Solicitud en contexto, rechazo y bloqueo permanente | `native/permissions/PermissionChecker.kt` |
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
    val beaconId: String? = null,        // UUID iBeacon, UID Eddystone o UUID de laboratorio
    val rssi: Int? = null,
    val capturedAtMillis: Long? = null,
    val errorCode: String? = null,
    val format: String? = null,          // "iBeacon", "Eddystone-UID" o "GATT"
    val major: Int? = null,
    val minor: Int? = null
)
```

Ninguno de los dos lanza excepciones. Todos los fallos se devuelven como un `status`, así
que el ViewModel y la interfaz no necesitan `try/catch`.

## Pruebas automatizadas

`app/src/test/java/cl/smartvolt/asistencia/feature/attendance/PresenceEvaluatorTest.kt`

Son 15 pruebas y las 15 pasan. El proyecto completo tiene 70 pruebas, también sin fallos
(`.\gradlew.bat testDebugUnitTest`).

Las pruebas cubren la lógica de decisión, no el hardware:

- no se puede registrar sin GPS y baliza;
- cada error tiene su propio mensaje;
- una ubicación simulada genera una advertencia de revisión, no una acusación de fraude;
- el resultado dice "pendiente de sincronización" y nunca "validada".

El hardware se prueba a mano con la matriz de más abajo.

## Cómo probar en el teléfono

### Preparar el teléfono

1. Activar Opciones de desarrollador y Depuración USB.
2. Conectar por USB y autorizar el equipo.
3. Verificar que aparece con `adb devices`.
4. Instalar con `.\gradlew.bat installDebug`.
5. Activar Ubicación y Bluetooth.

### Preparar el emisor BLE

No hay una baliza física autorizada. Para la demostración se usa un iPhone con la app
Locate Beacons emitiendo un iBeacon. Como alternativa sirve un segundo teléfono Android con
nRF Connect. Las dos opciones son sustitutos provisionales y están registradas como consulta
al profesor.

#### Opción A: iPhone con Locate Beacons (la que usamos)

1. Instalar Locate Beacons desde App Store.
2. Elegir el modo iBeacon.
3. Dejar el UUID que trae por defecto o escribir
   `6f1b2c3d-4e5f-4a6b-8c9d-0e1f2a3b4c5d`, con Major `1` y Minor `1`.
4. Pulsar Transmit y mantener la app abierta con la pantalla encendida. iOS detiene el
   anuncio si la app pasa a segundo plano o el teléfono se bloquea.
5. En la app Android, pulsar "Verificar presencia" a menos de un metro del iPhone.

#### Opción B: segundo teléfono Android con nRF Connect

1. Instalar nRF Connect for Mobile (Nordic Semiconductor).
2. Abrir la pestaña Advertiser.
3. Agregar a los datos de advertisement un Service UUID:
   `6f1b2c3d-4e5f-4a6b-8c9d-0e1f2a3b4c5d`.
4. Iniciar la emisión.

El UUID anterior es provisional, inventado para el laboratorio. No corresponde a ninguna
baliza real de la empresa. `BeaconConfig.EXPECTED_IBEACON_UUID` es `null` para aceptar el UUID
que se configure en Locate Beacons; en producción debe fijarse el UUID autorizado.

Si el escaneo no encuentra el emisor, revisar en este orden: que el iPhone esté anunciando con
la pantalla encendida, que el Bluetooth esté encendido en los dos equipos y que la app tenga
el permiso de escaneo. La ventana de escaneo es de 12 segundos.

### Recorrido en la app

Bienvenida, "Comenzar", elegir Obrero en el acceso rápido por rol, inicio, "Registrar
entrada". También se puede escribir `obrero@smartvolt.cl` con contraseña `demo1234`; capataz
y administrador usan sus propios correos.

La app acepta tres formatos de anuncio BLE: iBeacon (Locate Beacons), Eddystone-UID
(servicio `0xFEAA`, trama `0x00`) y el UUID de servicio de laboratorio. No se filtra por un
UUID fijo porque un iBeacon solo publica datos de fabricante.

## Matriz de pruebas en dispositivo físico

Hay que marcar el resultado real al ejecutar. Una fila sin marcar es una prueba pendiente,
no una prueba aprobada.

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| PT-06.1 | Permiso de ubicación denegado | Pulsar Verificar presencia y negar el permiso | La tarjeta de permisos queda en ámbar, se explica qué falta y se puede reintentar | Pendiente |
| PT-06.2 | Permiso bloqueado permanentemente | Negar dos veces, o bloquear desde ajustes, y volver | Aparece el botón "Abrir ajustes de la app" | Pendiente |
| PT-06.3 | Lectura GPS válida | Conceder permiso, ubicación activa, cielo abierto | Estado READY, coordenadas y precisión en metros | Pendiente |
| PT-06.4 | Ubicación del sistema apagada | Apagar Ubicación y verificar | Mensaje "La ubicación del teléfono está apagada", sin coordenadas inventadas | Pendiente |
| PT-06.5 | Sin lectura de GPS | Probar bajo techo o en subterráneo | Tras 15 s se informa el problema; si existe última lectura conocida se usa esa | Pendiente |
| PT-06.6 | Ubicación simulada | Activar una app de ubicación ficticia y verificar | Advertencia "La marca quedará en revisión"; la app no acusa fraude | Pendiente |
| PT-07.1 | Bluetooth apagado | Apagar Bluetooth y verificar | Mensaje "El Bluetooth está apagado" | Pendiente |
| PT-07.2 | Baliza cerca | Emisor a menos de 1 m | FOUND con formato (iBeacon), UUID, major/minor y RSSI negativo (típicamente entre -40 y -70 dBm) | Pendiente |
| PT-07.3 | Baliza ausente | Apagar el emisor y verificar | NOT_FOUND tras 12 s, con instrucción de acercarse e intentar de nuevo | Pendiente |
| PT-07.4 | Permiso de escaneo denegado | En Android 12+, negar el permiso de dispositivos cercanos | Estado PERMISSION_DENIED con explicación | Pendiente |
| PT-07.5 | Teléfono sin BLE | No aplica en teléfonos modernos | Estado NOT_SUPPORTED | No aplicable |
| PT-08 | Registro exitoso | GPS READY y baliza FOUND, pulsar Registrar entrada | Se guarda el evento, mensaje "pendiente de sincronización" y el contador sube en 1 | Pendiente |
| PT-09 | Doble toque | Pulsar Registrar entrada dos veces rápido | Se guarda un solo evento: el botón se deshabilita mientras guarda | Pendiente |
| PT-10 | Registro bloqueado | Solo GPS, sin baliza | El botón Registrar entrada está deshabilitado y se explica por qué | Pendiente |
| PT-11 | Persistencia local | Registrar, cerrar la app por completo, reabrir | El contador de pendientes conserva el valor | Pendiente |
| PT-12 | Notificación | Android 13+, conceder permiso de notificaciones, registrar | Aparece la notificación "Entrada registrada" | Pendiente |
| PT-13 | Notificación sin permiso | Android 13+, negar permiso de notificaciones, registrar | Se guarda igual, sin notificación y sin error | Pendiente |
| PT-14 | Formulario de horas extra | Como obrero, enviar vacío y luego completo | Errores por campo; con fecha, horario y motivo válidos queda pendiente en "Mis solicitudes" | Pendiente |
| PT-15 | Permisos por rol | Entrar como obrero y como administrador | El obrero solo solicita y consulta el estado; el administrador autoriza o rechaza con motivo | Pendiente |
| PT-16 | Mapa sin internet | Desactivar datos y verificar presencia | La tarjeta muestra error y "Reintentar" sin bloquear la marcación | Pendiente |

### Bitácora de hardware

Completar con lo que realmente se probó. No inventar filas.

| Campo | Valor |
|---|---|
| Modelo de teléfono | (pendiente) |
| Versión de Android | (pendiente) |
| Nivel de API | (pendiente) |
| Fecha de la prueba | (pendiente) |
| Emisor BLE usado | (pendiente: segundo teléfono con nRF Connect) |
| Firmware o app del emisor | (pendiente) |

## Decisiones de diseño y sus motivos

No hay umbral de precisión ni de RSSI. El anexo y el ERS piden validar presencia, pero no
fijan valores aceptables. Inventar "máximo 20 m" o "RSSI mínimo -70" sería una regla falsa
presentada como aprobada. `BeaconConfig.MIN_RSSI` es `null`: se acepta la primera coincidencia
y la interfaz muestra la señal real para que el equipo decida después, con el profesor, qué
umbral corresponde.

El `beaconId` es el UUID y no la dirección MAC. Leer `device.address` exige el permiso
`BLUETOOTH_CONNECT`, que no se necesita para escanear y que habría que justificar aparte. El
UUID del servicio identifica la baliza de prueba sin pedir permisos adicionales y además no
es un identificador físico del dispositivo.

Room en lugar de SharedPreferences. La asistencia y las solicitudes de horas extra se
guardan en Room/SQLite (`smartvolt_attendance.db`), de modo que sobreviven al cierre de la
app. La interfaz `AttendanceLocalStore` mantiene el desacoplamiento. La implementación
anterior con SharedPreferences se eliminó cuando se confirmó que nadie la usaba.

No se afirma que la marca está validada. Sin backend no hay validación del servidor.
Todo evento nace como `PENDING_SYNC` y la interfaz lo dice con esas palabras.

Detección de ubicación simulada. Android informa la señal cuando el sistema la conoce,
pero eso no prueba ni descarta manipulación. Se trata como indicio para revisión, tal como
pide el ERS, y nunca como veredicto.

## Limitaciones conocidas

1. No hay baliza física autorizada. El escaneo se ejecutó en emulador con un emisor sintético
   temporal (ya retirado del código) y quedó pendiente la prueba con el iPhone y Locate
   Beacons o con nRF Connect en un teléfono real. La lectura contra una baliza de la empresa
   depende de que la contraparte entregue el dispositivo y su UUID de prueba.
2. El emulador de Android no emite ni recibe Bluetooth LE, así que PT-07 tiene que ser en
   teléfono físico.
3. No hay sincronización remota. El evento queda local. Retrofit y el backend son fase
   posterior según la guía del equipo.
4. Entrada y salida están implementadas. La pantalla registra ambas y respeta el orden; lo
   que falta es el envío al servidor.
5. La identidad es de demostración. Los tres roles usan cuentas ficticias; no hay
   autenticación real ni doble factor.
6. El UUID de la baliza es provisional. Debe reemplazarse por el que entregue la contraparte
   antes de presentarlo como autorizado.

## Consultas abiertas para el profesor o la contraparte

| Tema | Pregunta |
|---|---|
| Baliza física | ¿Se dispondrá de una baliza BLE real con UUID de prueba para el hito? |
| Alternativa | ¿Se acepta un segundo teléfono emitiendo BLE como sustituto para la demostración? |
| Umbral | ¿Qué precisión GPS y qué RSSI mínimo se consideran aceptables? |
| Ubicación simulada | ¿Qué comprobación espera el profesor que se muestre en la demo? |
| Teléfono | ¿Hay un modelo o versión de Android exigido para la presentación? |

Estas consultas deben quedar también como tarjetas en la lista "Bloqueado / consultar" del
tablero Trello, con responsable y fecha.

## Correcciones aplicadas tras la revisión del 29-10-2026

Se revisó el módulo completo y se corrigieron estos puntos. Compilación y pruebas unitarias
verificadas después de los cambios.

| Corrección | Motivo |
|---|---|
| `AndroidBeaconScanner`: captura de cualquier excepción de `startScan`, no solo `SecurityException`, y `stopScan` siempre al terminar | Un fallo del adaptador ya no cierra la aplicación ni deja la radio encendida |
| `AndroidBeaconScanner`: al vencer la ventana se comprueba el adaptador | Si apagan el Bluetooth a mitad del escaneo se informa BLUETOOTH_OFF, no "baliza no detectada" |
| `AndroidLocationProvider`: la cancelación se vuelve a lanzar | `CancellationException` no se trata como un error de ubicación |
| `AndroidLocationProvider`: limpieza del listener antes de registrar proveedores y al fallar el registro | Evita dejar el GPS escuchando si `requestLocationUpdates` lanza una excepción |
| `AndroidLocationProvider`: la última ubicación conocida se descarta si tiene más de 2 minutos | Una lectura vieja ya no se presenta como actual. Límite técnico provisional, no regla de negocio aprobada |
| `PresenciaViewModel`: el contador se carga en `Dispatchers.IO` | La lectura de disco ya no se hace en el hilo principal al crear la pantalla |
| `PresenciaViewModel`: `try/catch/finally` en verificación y guardado | `isChecking` e `isSaving` siempre vuelven a `false` y el usuario recibe un mensaje si algo falla |
| `PresenciaScreen`: se solicita `POST_NOTIFICATIONS` en un lanzador independiente | En Android 13+ la notificación puede aparecer, y negarla no bloquea el registro |
| Manifest: se eliminó `neverForLocation` de `BLUETOOTH_SCAN` | La baliza sí se usa para acreditar presencia física y ese flag filtra anuncios BLE |
| Manifest: `allowBackup="false"` y exclusión de `smartvolt_attendance` en las reglas de respaldo | Las coordenadas guardadas no salen del teléfono en respaldos ni transferencias |
| `MainActivity`: `popUpTo("login")` al entrar a la marcación | Volver atrás ya no regresa al formulario con la contraseña escrita |
| `MainActivity`: desplazamiento y `imePadding` en bienvenida y acceso | El teclado ya no tapa los botones |
| `.gitignore`: exclusión de `*.jks`, `*.keystore` y `keystore.properties` | Las llaves de firma no pueden subirse por accidente |
| `app/build.gradle.kts`: configuración de firma release opcional | Permite generar el APK firmado sin romper el build de quien no tiene el keystore |

Queda pendiente después de esa revisión:

- La validación de presencia no compara coordenadas con un perímetro ni exige precisión o
  RSSI mínimos. No se agregaron umbrales inventados: siguen siendo decisiones abiertas.
- La ubicación simulada se advierte pero no bloquea el registro, y no existe una cola de
  revisión real. El texto de la interfaz lo presenta como una marca sujeta a revisión.
- El instante guardado en el evento es el del guardado, no el del fix de GPS. El contrato
  `LocationResult` sí conserva `capturedAtMillis` para cuando el Área B lo necesite.
- Las pruebas PT-06 a PT-16 siguen pendientes hasta contar con teléfono y baliza reales.

## Verificación del 04-11-2026

### Emulador (API 34)

Se ejecutó el recorrido completo en el AVD `smartvolt`, con `adb emu geo fix` para simular
coordenadas (`-33.45690, -70.64830`, precisión 5 m).

| Comprobación | Resultado |
|---|---|
| Bienvenida, login y validación por campo | Correcto: errores bajo cada campo y chips de rol |
| Inicio por rol (obrero, capataz, administrador) | Correcto; el inicio se refresca al volver de marcar |
| Mapa de Mapbox con internet | Imagen estática cargada con coordenadas y precisión superpuestas |
| Aviso de marca duplicada | Aparece como popup flotante y se cierra solo a los 7 s |
| Botón Atrás | Vuelve a la pantalla anterior; en el inicio no cierra la app |
| Historial filtrado por rol | Obrero ve solo sus marcas; administrador ve la faena |
| Panel de faena y horas extra | Métricas reales y decisión con motivo obligatorio |
| Persistencia Room | Marcas y solicitudes se mantienen al reabrir la app |

La prueba BLE en emulador no es representativa. Se usó un emisor sintético temporal para
verificar el flujo de la interfaz y se retiró del código antes del commit; la prueba real
queda para teléfono físico con Locate Beacons o nRF Connect.

### Formulario de horas extra

El obrero completa fecha, hora de inicio, hora de término y motivo. La validación vive en
`OvertimeFormValidator`, fuera de la interfaz, con 4 pruebas unitarias. El obrero ve sus
solicitudes y el administrador las autoriza o rechaza; el motivo del rechazo es obligatorio.
La decisión del administrador se guarda en la columna `decisionNote` y ya no reemplaza el
motivo que escribió el trabajador.

### Build

`testDebugUnitTest assembleDebug lintDebug` terminó con 70 pruebas y 0 fallos. Lint no
reportó errores y dejó 20 advertencias de dependencias y `UseKtx`, sin impacto funcional.
