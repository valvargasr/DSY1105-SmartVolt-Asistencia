# Registro mensual

El administrador puede consultar las horas trabajadas en el mes y guardar el detalle en un
archivo CSV. La entrada está en la tarjeta "Registro del mes" del inicio, visible solo para
ese rol.

## Qué muestra

La pantalla abre en el mes actual y permite retroceder con las flechas. No deja avanzar más
allá del mes en curso porque no existen marcas futuras.

El resumen de la faena incluye el total del mes, las horas de asistencia, las horas extra
aprobadas, la cantidad de trabajadores, la cantidad de días con marcas y las solicitudes
pendientes. Si un trabajador quedó con una entrada sin salida, aparece un aviso y esas horas
no se suman.

Debajo se listan los trabajadores con su total, el desglose de asistencia y horas extra, y
una fila por día con el horario registrado.

## Cálculo

El total del mes suma la asistencia y las horas extra aprobadas, que es lo que se pidió para
esta demo. El detalle por día se obtiene así:

1. Se ordenan las marcas del día por hora.
2. Cada entrada abre un tramo y la salida siguiente lo cierra.
3. Los tramos cerrados se suman en minutos.
4. Si el día termina con una entrada abierta, el tramo no se suma y el día queda marcado como
   sin salida.
5. Las horas extra aprobadas del día se suman aparte, usando la hora autorizada de inicio y
   de término.

Las solicitudes pendientes o rechazadas se cuentan en el resumen, pero no suman horas. La
autorización de una solicitud no demuestra que esas horas se hayan trabajado, tal como
advierte el modelo `OvertimeApproval`.

Las marcas se agrupan por fecha local del teléfono. Un turno que cruza la medianoche queda
partido en dos días.

## Archivo CSV

El botón "Guardar registro en CSV" abre el selector de archivos de Android con el nombre
`registro-smartvolt-AAAA-MM.csv`. No se piden permisos de almacenamiento.

El archivo usa UTF-8 con BOM, separador punto y coma y coma decimal, para que Excel en
español lo abra sin pasos extra. Tiene una fila por trabajador y día, con estas columnas:

| Columna | Contenido |
|---|---|
| Fecha | Fecha ISO, por ejemplo 2026-11-03 |
| Trabajador | Nombre de la cuenta demo |
| Identificador | Código del trabajador |
| Entrada | Hora de la primera entrada o vacío |
| Salida | Hora de la última salida o vacío |
| Horas asistencia | Horas decimales del día |
| Sin salida | Sí o No |
| Horas extra aprobadas | Horas decimales del día |
| Horas totales | Suma de asistencia y extra del día |

La última fila lleva la etiqueta TOTAL con la asistencia, las horas extra y el total del mes.

## Límites

El registro se calcula solo con lo que está guardado en el teléfono. No hay backend, así que
no incluye marcas de otros dispositivos ni sincronización pendiente. Tampoco considera
feriados, turnos teóricos ni atrasos: para eso haría falta definir esas reglas primero.

## Cómo probarlo

1. Entrar como administrador con `admin@smartvolt.cl` y la contraseña `demo1234`.
2. Abrir "Registro del mes" desde el inicio.
3. Cambiar de mes con las flechas y volver al mes actual.
4. Tocar "Guardar registro en CSV" y elegir una carpeta.
5. Abrir el archivo y revisar que el total coincida con la suma de las filas.
