# SmartVolt Asistencia

Aplicación móvil Android para el registro y la consulta de asistencia en faenas de construcción y servicios de ingeniería.

## 1. Información general

- **Asignatura:** DSY1105 — Desarrollo de Aplicaciones Móviles
- **Evaluación:** Evaluación Parcial 2 (EP2)
- **Organización de referencia:** Smart-Volt SpA
- **Tipo de proyecto:** Producto mínimo viable (MVP) académico
- **Estado:** Preparación del entorno y planificación de la implementación.

## 2. Descripción del proyecto

SmartVolt Asistencia es una propuesta de aplicación móvil orientada a facilitar el registro y la consulta de asistencia del personal en faenas. Busca reducir los errores asociados al registro manual mediante la captura de información de ubicación, datos de dispositivos Bluetooth Low Energy (BLE) y marcas temporales.

El desarrollo se realizará de manera incremental, priorizando una versión mínima funcional y la integración progresiva de sus componentes.

## 3. Objetivos

### 3.1. Objetivo general

Desarrollar una aplicación móvil que permita registrar y consultar la asistencia del personal en faenas, incorporando mecanismos de verificación y almacenamiento local.

### 3.2. Objetivos específicos

- Diseñar una interfaz de usuario clara y accesible.
- Implementar el registro de entrada y salida.
- Capturar información de ubicación y señales BLE, de acuerdo con los permisos disponibles.
- Validar los datos antes de registrar una asistencia.
- Permitir la consulta del historial de marcajes.
- Incorporar persistencia local para escenarios sin conexión.
- Preparar la integración y sincronización con un servicio remoto, según el alcance definido para el MVP.

## 4. Equipo y responsabilidades

| Integrante | Área | Responsabilidades |
|---|---|---|
| Valentina Vargas | Área A | Diseño de interfaz, experiencia de usuario y desarrollo de pantallas. |
| Julián Silva | Área B | Lógica de negocio, almacenamiento local y gestión de datos. |
| Joaquín Rojas | Área C | Recursos nativos de Android e integración de funcionalidades. |

## 5. Tecnologías y herramientas

Las siguientes tecnologías corresponden a la planificación técnica y deberán confirmarse en la configuración definitiva del proyecto.

- **Lenguaje de programación:** Kotlin.
- **Plataforma:** Android.
- **Entorno de desarrollo:** Android Studio.
- **Sistema de construcción:** Gradle.
- **Interfaz de usuario:** Jetpack Compose y Material 3, sujeto a la estructura acordada.
- **Persistencia local:** Room y SQLite.
- **Control de versiones:** Git y GitHub.
- **Gestión de tareas:** Trello.

La integración con servicios web mediante API REST y herramientas como Retrofit se abordará de acuerdo con las prioridades y los requisitos de la etapa correspondiente.

## 6. Funcionalidades previstas

El alcance propuesto para el MVP contempla:

1. Registro de entrada y salida del personal.
2. Captura de coordenadas GPS y datos de dispositivos BLE.
3. Registro de fecha y hora de los marcajes.
4. Validación de la información antes de almacenarla.
5. Consulta del historial de asistencia.
6. Almacenamiento local de registros para escenarios sin conexión.
7. Preparación de mecanismos de sincronización con un servicio remoto.
8. Interfaces adaptadas a los perfiles de usuario considerados en el alcance del proyecto.

Estas funcionalidades representan objetivos de implementación. Su disponibilidad y correcto funcionamiento deberán comprobarse mediante pruebas.

## 7. Arquitectura y organización técnica

La aplicación se organizará de forma modular, separando la interfaz de usuario, la lógica de negocio y el acceso a los datos.

La implementación deberá facilitar el desarrollo paralelo, la integración entre áreas y las pruebas funcionales. La arquitectura definitiva se ajustará a los requisitos académicos y a los acuerdos técnicos del equipo.

## 8. Seguridad y tratamiento de datos

Durante el desarrollo y las pruebas se deberán considerar las siguientes medidas:

- Utilizar datos ficticios, anonimizados o de prueba.
- Solicitar los permisos necesarios para acceder a la ubicación y a Bluetooth.
- Validar la información recibida antes de almacenarla.
- Gestionar los escenarios sin conexión y los posibles errores de captura.
- Evitar la inclusión de contraseñas, tokens, claves y datos personales reales en el repositorio.

## 9. Estado de avance

### Completado

- Repositorio de GitHub creado.
- Distribución inicial de responsabilidades del equipo.
- Documentación inicial de requisitos y planificación.
- Preparación inicial del entorno de desarrollo.

### Pendiente

- Crear e integrar el proyecto Android en el repositorio.
- Implementar las primeras pantallas.
- Desarrollar la lógica de registro de asistencia.
- Incorporar la persistencia local.
- Integrar los recursos nativos y los permisos necesarios.
- Ejecutar pruebas funcionales y de integración.
- Preparar el APK para la evaluación.

El estado de esta sección deberá actualizarse a medida que las tareas se completen y sus resultados sean verificados.

## 10. Gestión del desarrollo

GitHub se utilizará para el control de versiones y la colaboración sobre el código fuente. Trello se utilizará para organizar las tareas, registrar avances y coordinar el trabajo entre las áreas.

La integración se realizará de forma incremental, procurando mantener una versión compilable y verificable durante el desarrollo.

## 11. Alcance y limitaciones

SmartVolt Asistencia corresponde a un proyecto académico de tipo MVP. La descripción de las funcionalidades no implica que estas se encuentren implementadas, validadas o disponibles para uso productivo.

Las decisiones técnicas y los requisitos pendientes deberán revisarse con el equipo y contrastarse con las indicaciones oficiales de la asignatura.

---

**Asignatura:** DSY1105 — Desarrollo de Aplicaciones Móviles  
**Proyecto:** SmartVolt Asistencia
