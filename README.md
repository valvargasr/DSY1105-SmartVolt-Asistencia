# SmartVolt Asistencia

Aplicación móvil Android para el registro de asistencia en faenas, con verificación de
ubicación GPS y proximidad mediante una baliza Bluetooth Low Energy (BLE).

## 1. Información general

- **Asignatura:** DSY1105 — Desarrollo de Aplicaciones Móviles
- **Evaluación:** Evaluación Parcial 2 (EP2)
- **Organización de referencia:** Servicio de Ingeniería Smart-Volt SpA (caso académico)
- **Tipo de proyecto:** MVP académico. No es un sistema productivo.
- **Paquete de la aplicación:** `cl.smartvolt.asistencia`

## 2. Equipo y responsabilidades

| Integrante | Área | Responsabilidades |
|---|---|---|
| Valentina Vargas | Área A | Diseño de interfaz, experiencia de usuario y pantallas. |
| Julián Silva | Área B | Lógica de negocio, validaciones, almacenamiento local y gestión de datos. |
| Joaquín Rojas | Área C | Recursos nativos de Android (GPS, BLE, permisos, notificaciones) e integración. |

Tablero de tareas: Trello **DSY1105 - SmartVolt Asistencia**.

## 3. Estado de implementación

Estado verificado en la rama `feature/location-permissions`. Compila y sus pruebas
unitarias pasan. **El GPS y el BLE todavía no se han probado en un teléfono físico.**

### Implementado

- Pantalla de bienvenida y pantalla de acceso con validación por campo
  (correo con formato válido y contraseña no vacía), errores junto al campo.
- Navegación entre pantallas con Navigation Compose.
- Pantalla de marcación (`PresenciaScreen`) con estados de permisos, GPS y baliza.
- Solicitud de permisos en contexto con explicación y manejo de rechazo, incluido el
  rechazo permanente con acceso directo a los ajustes de la aplicación.
- Lectura real de ubicación con `LocationManager`: latitud, longitud, precisión,
  instante de la lectura y señal de ubicación simulada.
- Escaneo BLE real con `BluetoothLeScanner`, filtrado por UUID de servicio, con ventana
  de escaneo acotada y reporte de RSSI.
- Detección y mensaje para: permiso denegado, ubicación apagada, Bluetooth apagado,
  hardware sin BLE, baliza no encontrada y errores del sistema.
- Guardado local de la marcación con identificador único y estado `PENDING_SYNC`.
- Contador de marcaciones pendientes de sincronización.
- Notificación local al guardar una marcación, con solicitud del permiso
  `POST_NOTIFICATIONS` en Android 13 y posteriores.
- Reglas de presencia en Kotlin puro (`PresenceEvaluator`), separadas de la interfaz.
- 15 pruebas unitarias de esas reglas.

### Pendiente

- Pruebas en dispositivo físico (GPS y baliza). Ver `docs/pruebas-area-c.md`.
- Registro de salida (check-out), historial y persistencia con Room (Área B).
- Panel de supervisor, autorización de horas extra y calendario de feriados.
- Activación con doble factor, sesión persistente y vinculación del dispositivo.
- Sincronización con API REST (Retrofit, Hilt y Spring Boot), etapa posterior.
- APK release firmado: requiere generar el keystore (ver sección 9).

## 4. Requisitos del entorno

- Android Studio Rabbit 1 (2026.2.1).
- **JDK 17.** El proyecto no compila con JDK 24.
- Android SDK Platform **android-37.0** y Build-Tools **37.0.0**.
- Gradle **9.6.0**, incluido mediante el wrapper del repositorio (`gradlew`).
  No es necesario instalar Gradle por separado.
- Variables de entorno `JAVA_HOME` apuntando al JDK 17 y `ANDROID_HOME` al SDK.

Versiones declaradas: AGP 9.4.1, Kotlin 2.2.10, Compose BOM 2026.02.01,
`compileSdk` 37, `targetSdk` 37, `minSdk` 24.

No actualizar estas versiones de forma independiente: se revisan en conjunto con el equipo.

## 5. Cómo compilar

Desde la raíz del proyecto, en Windows (PowerShell):

```powershell
$env:JAVA_HOME = "C:\ruta\al\jdk-17"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# Compila la aplicación de depuración
.\gradlew.bat assembleDebug

# Ejecuta las pruebas unitarias
.\gradlew.bat testDebugUnitTest

# Compila la variante release (sin firmar si no hay keystore.properties)
.\gradlew.bat assembleRelease
```

El APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk`.

También se puede abrir la carpeta del proyecto en Android Studio, esperar la
sincronización de Gradle y usar **Run** sobre un dispositivo conectado.

## 6. Cómo ejecutar la demostración

1. Instalar el APK en un teléfono Android con GPS y Bluetooth.
2. Abrir la aplicación y presionar **Comenzar**.
3. En el acceso, presionar **Entrar como OP-001 — demo**. Este botón es solo para la
   demostración: no representa una autenticación real.
4. Presionar **Verificar presencia** y conceder los permisos solicitados.
5. Revisar el estado de la ubicación y de la baliza.
6. Con ambos disponibles, presionar **Registrar entrada**.

### Baliza de prueba

La aplicación busca el UUID de servicio definido en
`native/bluetooth/BeaconConfig.kt`:

```
6f1b2c3d-4e5f-4a6b-8c9d-0e1f2a3b4c5d
```

**Es un UUID provisional de laboratorio, no uno entregado por la empresa.** Mientras no
exista una baliza autorizada, se puede usar un segundo teléfono con una aplicación emisora
(por ejemplo nRF Connect) anunciando ese UUID como Service UUID.

Si no hay ningún emisor, la aplicación informa «No se detectó la baliza autorizada» y no
habilita el registro. Ese comportamiento es el esperado, no un error.

## 7. Arquitectura y organización

MVVM con Jetpack Compose, estado expuesto con `StateFlow` y reglas de negocio separadas
de la interfaz.

```
cl.smartvolt.asistencia
├── MainActivity.kt                 Navegación, bienvenida y acceso
├── LoginViewModel.kt               Estado y validación del acceso
├── ui/theme/                       Tema Material 3
├── feature/attendance/
│   ├── PresenciaScreen.kt          Pantalla de marcación
│   ├── PresenciaViewModel.kt       Orquestación y estado observable
│   ├── PresenciaUiState.kt         Estado inmutable de la pantalla
│   └── PresenceEvaluator.kt        Reglas puras, sin dependencias de Android
├── data/local/
│   ├── LocalAttendanceEvent.kt     Modelo de la marcación
│   ├── AttendanceLocalStore.kt     Puerto de persistencia
│   └── SharedPreferencesAttendanceStore.kt
└── native/
    ├── location/                   LocationProvider y AndroidLocationProvider
    ├── bluetooth/                  BeaconScanner, AndroidBeaconScanner, BeaconConfig
    ├── permissions/                PermissionChecker y PermissionStatus
    └── notifications/              AttendanceNotifier
```

Los puertos (`LocationProvider`, `BeaconScanner`, `AttendanceLocalStore`) declaran que no
lanzan excepciones: devuelven un objeto de resultado con estado y código de error. Esto
permite sustituirlos por implementaciones de prueba.

## 8. Pruebas

```powershell
.\gradlew.bat testDebugUnitTest
```

`PresenceEvaluatorTest` cubre 15 casos: ausencia de lecturas, GPS disponible, baliza
detectada, baliza ausente, permiso denegado, ubicación apagada, Bluetooth apagado, tiempo
de espera agotado, precisión informada, ubicación simulada y mensajes de sincronización.

La matriz de pruebas en dispositivo físico y su bitácora están en `docs/pruebas-area-c.md`.

## 9. Firma del APK release

El keystore y sus contraseñas **no se suben al repositorio** (están en `.gitignore`).

1. Copiar `keystore.properties.example` como `keystore.properties` en la raíz del proyecto.
2. Generar el keystore:

   ```
   keytool -genkeypair -v -keystore smartvolt-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias smartvolt
   ```

3. Completar las contraseñas en `keystore.properties`.
4. Ejecutar `.\gradlew.bat assembleRelease`.

Sin `keystore.properties` la compilación sigue funcionando y genera un APK sin firmar,
que no se puede instalar.

## 10. Privacidad y tratamiento de datos

- Se usan identificadores ficticios: `OP-001` y `FAENA-DEMO-01`. No hay nombres, RUT,
  correos ni datos personales reales.
- La aplicación no declara el permiso `INTERNET`: las marcaciones quedan en el teléfono.
- El respaldo automático está deshabilitado (`allowBackup="false"`) y las preferencias con
  marcaciones se excluyen de las reglas de respaldo y transferencia.
- La ubicación simulada se informa como una señal de revisión, no como una acusación de
  fraude. No se promete una detección infalible.
- No se guardan contraseñas, tokens ni claves en el repositorio.

## 11. Alcance y limitaciones

- El registro exige ubicación disponible y baliza detectada, pero **todavía no compara la
  posición con un perímetro de faena ni exige una precisión o un RSSI mínimo**. Esos
  umbrales dependen de decisiones pendientes con el profesor y la contraparte.
- No hay validación de presencia en servidor. El estado `PENDING_SYNC` significa guardado
  local pendiente de sincronizar, nunca validado por un backend.
- El UUID de la baliza es provisional y clonable: no constituye una prueba criptográfica
  de identidad.
- La persistencia actual usa `SharedPreferences` con JSON. La migración a Room corresponde
  al Área B.
- El acceso de demostración no implementa autenticación ni doble factor.

---

**Asignatura:** DSY1105 — Desarrollo de Aplicaciones Móviles
**Proyecto:** SmartVolt Asistencia
