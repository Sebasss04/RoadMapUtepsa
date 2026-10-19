# RoadMap UTEPSA: estado actual y mantenimiento

## Publicación de rama autorizada — 18/10/2026

El usuario autorizó publicar la entrega revisada en `audit/t1-integracion-final`.
Esta autorización comprende commit y push normales de los 25 archivos auditados.
La integración en main queda para una aprobación posterior. Las secciones siguientes
registran la validación local previa: 258 JVM y 59 UI sintéticas correctas,
compilación correcta y lint 0 errores/39 advertencias. No se vuelve a desplegar
Firebase ni se modifican datos personales al publicar esta rama.


## Auditoría final y corrección local — 18/10/2026

Estado vigente: rama `audit/t1-integracion-final`, base `30685a6`, sin commit ni publicación.
Resumen conserva GUARDADO_PENDIENTE aunque haya contradicciones; CONFIRMADO incoherente
sigue mostrando revisión. Regresión JVM comprobada antes (falló) y después (pasó).
Validación nueva: **258 JVM y 59 UI sintéticas distintas correctas**; APK principal y
AndroidTest compiladas; lint **0 errores/39 advertencias**. Detalles, intentos y límites
en VALIDACION.md. Repositorios, CAS, Historial, Oferta, motor y contratos preservados.
Solo se corrigió producción en ResumenUiState; AcademicFlowTest ajusta teclado/scroll
sin retirar aserciones. Sin Firebase productivo ni cuentas reales. Publicar requiere
una autorización nueva expresa. Los apartados de integración siguientes son antecedentes.


## Integración local de T1 con main — 18/10/2026

Sebastián autorizó actualizar su rama y unificar T1 con los avances de Alexandre.
`main` local avanzó por fast-forward de `96a400a` a `30685a6`, merge del PR #2;
el PR #1 de Oferta ya estaba integrado en `2f21871`. No volver a fusionar las ramas
de Oferta/Para ti ni interpretar sus registros anteriores de PR abierto como pendientes.
T1 está integrado en esta copia de trabajo, **sin commit**: GitHub main todavía
no contiene estos cambios locales de Resumen.

Se respaldaron los 19 archivos locales con manifiesto SHA-256 y lectura posterior
del ZIP antes de actualizar; el respaldo permanece fuera del repositorio. No se
usó reset, restore, clean, stash ni rebase. Los archivos nuevos se conservaron;
el parche versionado respaldado se retiró temporalmente para permitir fast-forward
y se repuso junto a la integración revisada. No se descartó trabajo del estudiante.

Navegación local: Resumen, Malla, Oferta y Para ti comparten un destino exclusivo
por UID/versión; Resumen utiliza el AcademicViewModel existente. Se conservan la
clave de estado de Malla, filtros/búsqueda/semestre, borrador/búsqueda/revisión de
Oferta, recarga de Para ti y avisos de descarte. El CTA de Resumen abre Para ti;
sus gates existentes siguen decidiendo si faltan historial u oferta. Perfil vuelve
a Resumen/Para ti desde esas pantallas y conserva el retorno previo Oferta→Malla.

AppContainer/MainActivity, repositorios, motor, revisión académica/CAS, diario,
UUID y catálogo son los de `30685a6`. No hay modificaciones propias de integración
en sus contratos ni despliegues. Se mantienen los errores controlados del login.
Tres pruebas UI nuevas cubren los cuatro destinos, selección guardada frente a
borrador, revisión al guardar, retorno de Perfil/aviso de cierre y cambio de cuenta.

Integración verificada: Gradle correcto, 257 JVM, 38 Android sintéticos y lint
0 errores/39 advertencias. Ejecuciones, primer intento y límites en VALIDACION.

Google: la firma debug de esta PC se registró con autorización específica en la
sesión anterior; Google autenticó y mostró Completa tu perfil. El JSON descargado
actualizado quedó pendiente por limitación de automatización. No se repite ese
cambio remoto ni se inventan datos de perfil/historial. La incidencia ART/System UI
del AVD personal continúa abierta y no se confunde con validación del código.


Revisión de Para ti (T3/T4) del 18/10/2026, hora de Bolivia. Leer este documento y [PRODUCTO_Y_PLAN.md](PRODUCTO_Y_PLAN.md) para continuar. Actualizar sus secciones en lugar de crear nuevos relevos o añadir estados contradictorios al final.

Arranque directo de MainActivity en el AVD sintético: Status ok, COLD, 5237 ms;
login visible y proceso vivo después de las pruebas. No se inició sesión real.
El AVD sintético se detuvo al terminar; Pixel_7 y sus datos permanecen intactos.

## Ubicaciones y fuentes de verdad

- Código: raíz de este repositorio (`../`).
- Documentación vigente: estos dos documentos.
- Diseños: [DISENOS](DISENOS/). PDF y diagramas de referencia: [REFERENCIAS](REFERENCIAS/). Los diagramas incluyen componentes futuros; contrastarlos con el código.
- Informes y respaldos históricos: conservados en la carpeta documental del autor; no se distribuyen con el repositorio. Las verificaciones narradas más abajo son registros de sesiones anteriores, no ejecuciones nuevas. Ver VALIDACION.md para esta publicación.
- `app/build/reports/` contiene evidencia local regenerable y no se versiona. No depende de esa carpeta para iniciar el desarrollo.

Los viejos documentos acumulaban instrucciones de inicio, pausas, aprobaciones y resultados correspondientes a momentos diferentes. No usarlos como lista vigente de tareas ni reejecutar despliegues o pruebas personales por leer un pendiente antiguo. Algunas rutas de esos informes corresponden a su ubicación anterior; usar el inventario para localizarlas.

## Implementado frente a pendiente

| Componente | Estado observado en código |
|---|---|
| Google / sesión | Firebase Auth y Credential Manager, cambios de UID y separación por sesión. |
| Perfil | Firestore real, alta/edición, validación y conservación del plan. |
| Catálogo | SAE publicado y relectura verificada: 50 materias, 28 relaciones y umbral STS-300 de 45. Cliente compatible también con el formato anterior durante transición. |
| Malla | Búsqueda/filtros/semestres, requisitos, edición y guardado con comparación de estados esperados (CAS). |
| Historial | Primera preparación separada de consulta y edición; resumen guardado, cancelación, revisión de diferencias, confirmación, diario, recuperación y conflictos. Posponer no degrada CONFIRMADO. |
| Coherencia | Dominio Kotlin puro, conectado a ViewModels y repositorios. |
| Notificaciones | FCM y saludo local con permiso Android. Saludo local no acredita recepción push remota. |
| Oferta actual SAE / T2 | Flujo implementado en rama `feat/oferta-actual-sae`: consulta, editor de 50 materias, reemplazo atómico, vacío/ausente, errores, resultado incierto y conflictos explícitos. 193 pruebas JVM, 7 UI y 1 SDK demo correctas; consultar VALIDACION.md para detalles y lint. Reglas de Oferta publicadas con autorización posterior al diagnóstico del teléfono y relectura remota verificada. |
| Para ti / T3-T4 | Implementado en `feat/para-ti`, con avances parciales, secundaria/opcionales, textos breves y colores propios. Diseño Figma actualizado antes de Compose. Ver resultados de la revisión actual y evidencia previa en VALIDACION.md. |
| Resumen / T1 | Integrado localmente sobre `30685a6`, sin confirmar; conectado a Malla/Oferta/Para ti. Ver validación vigente. |
| Correo, registro, recuperación, vinculación, Cuenta completa, biometría | Pendientes; declarar dependencias no equivale a implementar. |
| Bimodulares | Publicadas: 40 modulares, 9 bimodulares y Taller con duración especial; etiquetas visibles, sin horas ni conversión a meses. |

No repetir Historial ni Malla como desarrollos nuevos: ya existen. La revisión actual busca corregirlos y estabilizarlos.

## Para ti — diseño e implementación

Autor/Codex asume T3 y T4 por petición expresa. Rama `feat/para-ti`, adelantada
sin cambios de árbol al merge de Oferta `origin/main` (`2f21871`). No se encontraron
cambios de Resumen allí. Se preservaron los cambios locales del plan al sustituirlo
por el alcance vigente, incluyendo la nueva prioridad de pendientes anteriores.

Diseño: [Figma 11:295](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=11-295),
13 estados revisados antes de Compose, con ajustes posteriores de jerarquía y
detalle parcial; [exportación vigente](DISENOS/08_PARA_TI_AJUSTES_20261018.png).
Implementación: motor Kotlin puro con reglas académicas compartidas, repositorio
solo de consulta, ViewModel por sesión y pantalla con principal, secundaria y opcionales.
La referencia para pendientes anteriores es el mayor semestre con aprobadas/en curso,
no un semestre personal. Priorización orientativa, sin prohibición institucional.

Consulta Preparación y Oferta guardadas entre revisiones académicas estables con
tres intentos acotados y control de identidad; no añade escritores ni listeners.
Malla y Oferta abren Para ti; conserva borrador/búsqueda/revisión esperada de Oferta.
Se mantienen los flujos de Historial, Perfil y cierre de sesión. Cálculo y lectura no
escriben. Diseño, código y verificaciones se registran por separado en VALIDACION.md.
No hay despliegues, migraciones ni pruebas personales en esta entrega.

Ajustes del 18/10/2026 implementados: avances parciales por prerrequisitos directos,
secundaria solo con beneficio académico, opcionales sin favorita artificial y
pendientes anteriores desplegables. Tarjetas verde/azul/gris y textos breves.
Figma actualizado antes de Compose; exportación 08_PARA_TI_AJUSTES_20261018.png.
Resultados y límites de esta revisión en VALIDACION.md. El 18/10/2026 el estudiante
autorizó publicar la entrega completa en `feat/para-ti`. Push confirmado de
`e13ef13` y [PR #2](https://github.com/Sebasss04/RoadMapUtepsa/pull/2) abierto
hacia `main`, pendiente de revisión, sin merge. Resumen sigue con el compañero.
El inventario y las indicaciones
para retomar están en [README.md](README.md#continuar-las-correcciones-en-otro-chat).
No quedan servicios demo, AVD de prueba ni verificaciones de esta tarea en curso.

## Registro T1 anterior a integrar Oferta y Para ti — 18/10/2026

Los accesos deshabilitados descritos aquí pertenecen a la entrega anterior.
La navegación vigente de las cuatro pantallas se describe al inicio.

Implementación local sobre `main`, `96a400a`; copia inicialmente sin cambios.
Se suspendió el flujo histórico de ramas/PR por instrucción del estudiante. No se
hicieron commits, cambios de rama, escrituras GitHub ni modificaciones Firebase.
Durante T1 Google Sign-In quedó pospuesto; el diagnóstico posterior está descrito arriba.
T2, T3, T4 y T5 no se iniciaron.

- `ResumenAcademico` deriva PENDIENTE/EN_CURSO/APROBADA y el porcentaje entero
  `aprobadas / 50 × 100` de la lectura existente. Valida las 50 identidades,
  pertenencia al plan y ausencia de estados ajenos; EN_CURSO no suma aprobación.
- `ResumenUiState` distingue carga, lectura fallida, sesión vencida, preparación
  pendiente, guardado parcial e incoherencia recalculada incluso con CONFIRMADO.
  Los conteos guardados no acreditan coherencia; el aviso permite revisar Historial.
- Resumen comparte `AcademicViewModel` con Malla, conserva filtros/búsqueda/semestre
  y se actualiza tras sus guardados válidos y la relectura de retorno de Historial.
  Consultar no genera escrituras ni persiste otra lista. UID/versión de sesión,
  Perfil, cierre, CAS, revisión global y repositorios se conservaron.
- Referencia Figma ahora accesible: `8:253` (ejemplo), `8:318` (primer ingreso),
  `8:387` (carga), `8:434` (error). Se ajustaron cabecera 56dp, contenido 16/24dp,
  tipografía/alturas de línea, tarjetas, porcentaje 42sp, progreso 8dp, esqueletos,
  borde/colores y bloque de error con el icono original 48dp.
- Se verificaron los cinco iconos existentes con los assets de Figma: misma geometría
  y colores aplicados por Icon. El SVG nuevo de advertencia se conservó sin cambios
  en `design-assets/summary/warning.svg`, del nodo `8:472`; su PNG 192×192 está en
  drawable-xxxhdpi y se muestra a 48dp. No hay URLs temporales en ejecución.
- Oferta, Para ti y el CTA de recomendaciones siguen deshabilitados. El bloque visual
  aprobado se muestra sin implementar T2/T3/T4. Resumen permite ir a Malla e Historial;
  Atrás vuelve a Malla. Los datos del ejemplo usado en QA son sintéticos.

Verificación final: APK app y AndroidTest compilados con Gradle desde Android Studio;
188 pruebas JVM correctas; lint 0 errores y 39 advertencias preexistentes; 9 pruebas
Android verificadas en ejecuciones dirigidas (6 visuales sintéticas + 3 de navegación/
cambio de cuenta), incluidas
actualización tras guardar, retorno con filtros, error/reintento y retiro de datos al
cambiar UID/cerrar. Se revisaron capturas reales contra los cuatro estados Figma y
los avisos de historial. Las capturas usan captureToImage de Compose para evitar
fotogramas vacíos transitorios del compositor del emulador.

El Gradle de la terminal restringida sigue rechazando la comprobación Java de
escritura del directorio. Android Studio pudo ejecutar las tareas sin cambiar
configuración. Una primera ejecución offline no encontró datastore-preferences-jvm;
se descargó la versión ya declarada desde los repositorios configurados, sin cambiar
dependencias. La repetición final offline compiló y pasó lint. APK y pruebas se
instalaron localmente con -r, conservando datos. No se publicaron APK ni se borró o
restableció el AVD. Hubo cierres nativos ART en dos intentos finales: las seis visuales
y una integración pasaron en el reintento; las otras dos integraciones pasaron en
procesos individuales. No se declara reparado ART/system_server; ver VALIDACION.

## Arquitectura y persistencia

`RoadMapApplication → AppContainer → repositorios compartidos → ViewModels → pantallas Compose`. Dominio académico e historial independientes de Android/Firebase. ViewModels identificados por UID y versión de sesión. Conservar el paquete `com.example.roadmap_utepsa` y los cambios existentes. La entrega del 17/10 incorpora estos avances a Git por solicitud del estudiante.

`AppNavigation` comprueba autenticación/perfil y utiliza `HistorialGate`. Sin decisión muestra preparación; con decisión y sin diario activo entra a Malla. Es posible revisar/retomar desde Malla y visitar Perfil conservando borrador.

Proyecto Firebase configurado: `roadmap-utepsa-c9409`, base `(default)`. Reglas en `firebase/firestore.rules`. La evidencia histórica acredita publicación del catálogo y reglas, incluidas las de preparación el 17/10; la revisión SAE no publica reglas y migra únicamente el catálogo tras instalar el cliente compatible. Hash histórico de reglas publicado: `6c1ad83d09335bc3337996b82e4806fc78ec5c31917437fe143738caa3088ed4`; no es una nueva comprobación remota.

Rutas académicas implementadas:

- `materias/{id}` filtradas por `id_plan`, con subcolección `prerrequisitos`.
- `estudiantes/{uid}/estados/{idMateria}`: solo EN_CURSO/APROBADA; pendiente elimina.
- `estudiantes/{uid}/configuracion_academica/historial`: decisión, formato y timestamp.
- `estudiantes/{uid}/operaciones_historial/actual`: diario, huellas de base/objetivo, cambios y aplicados.
- `estudiantes/{uid}/control_academico/revision`: `version_formato=1`, `valor` entero monotónico; todos los escritores académicos deben incrementarlo en el mismo commit.

El guardado de Malla compara los 50 estados, incluidos documentos ausentes, mediante un snapshot completo protegido por la revisión global. El asistente aplica grupos de hasta cinco y valida el conjunto completo; cada transacción comprueba la revisión para detectar cualquier cambio desde el snapshot. El último grupo confirma marcador y diario atómicamente. Las consultas de preparación leen estados y controles entre dos lecturas iguales de revisión; la relectura final desde servidor sigue siendo necesaria antes de comunicar éxito. Las reglas protegen acceso/esquema; la coherencia curricular se valida en la app. Un marcador CONFIRMADO no evita recalcular coherencia tras editar Malla.

## Oferta actual SAE (T2)

Diagnóstico tras la prueba del estudiante en teléfono físico: se consultaron las
reglas activas de producción en modo de solo lectura. Su SHA-256 sigue siendo
`7b70043cfb047d57009062fa1f3ebdd8b87f6cc444e90464855cdb37327479dd`:
no contienen `match /ofertas/actual` y el cierre general deniega esa ruta.
El cliente intenta leerla desde servidor antes de mostrar ausencia/editor, por
lo que el rechazo activa el mensaje genérico de lectura y cada reintento vuelve
a fallar. No confundir desarrollo T2 completo con habilitación productiva.
El estudiante autorizó después publicar las reglas de Oferta. El despliegue terminó
con código cero y relectura remota idéntica al candidato local SHA-256
`4fc14fb7e06bf3de81b8791bf67db1942202c191868561a8bfbbe61da8ecfc02`.
Se comprobó por tokens que, al quitar los cuatro bloques nuevos de Oferta, las
reglas existentes quedan iguales. No se modificaron documentos personales.
No se obtuvo Logcat del teléfono; la causa del bloqueo se confirmó en las reglas
remotas, sin atribuir una excepción observada al dispositivo. Ver VALIDACION.md.

Después de publicar, `ProductionOfferReadOnlyTest` pasó en el teléfono físico:
lectura productiva de 50 materias, oferta ausente correctamente, pantalla de
primera oferta y apertura/cancelación del editor. Misma sesión y snapshots
académicos/oferta iguales antes y después; ninguna escritura invocada. Se conservó
la APK principal y se instaló únicamente la APK de pruebas. El teléfono se
desbloqueó a petición de la verificación y la app quedó reabierta. No se probó un
guardado personal; el estudiante debe registrar su selección real.

Ajuste posterior solicitado por el estudiante: el selector deja de mostrar materias
APROBADA, también en la búsqueda. Se conservan EN_CURSO y pendientes, sin filtrar
por prerrequisitos. El editor recibe estados reales, refresca al volver desde Malla
y conserva borrador/búsqueda/revisión esperada. Las aprobadas de una oferta antigua
se retiran solo del borrador con aviso y se persisten únicamente al pulsar Guardar;
Cancelar conserva la oferta previa. La relectura antes de enviar detecta nuevas
aprobaciones y exige revisar la selección antes de volver a guardar. No modifica
el contrato de Firestore, los estados académicos ni las reglas ya publicadas.
Verificación del ajuste: 199 JVM, nueve UI sintéticas y una lectura productiva
en el teléfono correctas. La comprobación real buscó una aprobada (sin resultados)
y una no aprobada (disponible), conservando sesión y snapshots académicos/oferta.
APK actualizada instalada con `-r`, sin guardados personales de prueba. Lint:
cero errores, 32 advertencias. Evidencia y límites en VALIDACION.md.

Pantalla basada en Figma `15:431` y editor `15:494`, adaptados a Compose/Material 3
con los componentes y cinco iconos existentes (geometría comparada con los SVG de
Figma). El nombre es principal y sigla/duración secundarias por la especificación
actual; los datos de ejemplo del diseño se sustituyen por el catálogo remoto.
Malla e Historial conservan su flujo; Oferta se habilita desde la navegación inferior.
No se cambian los contratos de AcademicData/AcademicRepository ni escritores académicos.
Ver el [contrato de Oferta](PRODUCTO_Y_PLAN.md#oferta-actual-sae-contrato-t2).

La rama partió de `96a400a` tras consultar `origin/main`; no había implementación
T1 en ese estado. Se conservó la modificación local de coordinación en
TRABAJO_EN_EQUIPO.md. Publicación/PR y validaciones finales se registran en
VALIDACION.md; una rama local por sí sola no es una entrega compartida.

## Cierre anterior, espera de confirmación y correcciones

El registro del emulador del 17/10 a las 14:21:01 GMT (10:21:01 en Bolivia) corresponde al paquete de la app. Muestra `SIGSEGV`, dirección `0x10`, hilo principal, `libart/Class::FindClassMethod → ResolveMethod → NterpGetMethod → com.google.android.gms.tasks.zzw.addOnCompleteListener → TasksKt.await → FirebasePreparacionSource.transaccion`. Es un fallo nativo durante la espera de una transacción, no una excepción Kotlin recuperable con try/catch.

Pixel_7 utiliza imagen `sdk_gphone16k_x86_64` API35, build `AE3A.240806.041/12890756`. El flag debug `vmSafeMode` que ya existía NO evitó este cierre y se retiró conservando la configuración de red local. Ese cierre no acreditaba que el historial quedara guardado: pudo caer con una operación enviada y resultado incierto. El nuevo intento manual posterior sí terminó confirmado, como se describe más abajo. Su recuperación debe releer el diario y estados, sin reenviar a ciegas. No modificar manualmente datos del estudiante para simular una reparación.

Correcciones de código:

1. Al entrar en Malla desde el gate, refrescar también en la entrada automática tras recreación. Cancelar una lectura anterior al historial y evitar que su respuesta tardía sustituya estados nuevos.
2. Reconocer `ConflictoHistorial` envuelto por el SDK en guardado y resolución, conservando borrador y mostrando revisión remota.

Verificación anterior a la optimización: 162 pruebas JVM, 0 fallos/errores; APK app y pruebas compiladas. Lint: 0 errores, 36 advertencias (dependencias, Credential Manager, plurales y recursos, entre otras). En Pixel_7, sin vmSafeMode, pasaron 4 pruebas UI aisladas con repositorios en memoria: aprobadas + en curso → confirmar → Malla, posponer/retomar, volver de Perfil conservando borrador y conflicto. Pasó además una prueba con SDK Firebase real contra Auth/Firestore LOCALES: primera confirmación con APROBADA/EN_CURSO y relectura tras recrear el repositorio. No confundir estas pruebas con un nuevo guardado del historial personal.

Verificación productiva anterior al nuevo intento del estudiante: `ProductionPreparationReadOnlyTest` pasó (1 prueba, 32.388 s), 50 materias, misma sesión, pantalla esperada y preparación/diario iguales antes y después. Resultado: preparación ausente, sin operación activa y ninguna acción de escritura. App final instalada con conservación de datos y abierta en MainActivity. Servicios Auth/Firestore demo de la revisión detenidos. Total Android final: 4 UI en memoria + 1 SDK local + 1 lectura productiva, todos correctos; no se acredita un guardado del historial real del estudiante.

## Trabajar y probar

Herramientas observadas: wrapper Gradle 9.5.0, AGP 9.3.3, JDK25, SDK37, minSdk35. No actualizar dependencias por rutina.

```powershell
cd <ruta-local-del-clon>
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline --no-configuration-cache --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8' --console=plain
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Pruebas Android: compilar `:app:assembleDebugAndroidTest`. UI simulada: `HistorialFlowTest` y `AcademicFlowTest`; no acreditan Firebase real. `PreparacionSdkEmulatorTest` requiere Auth/Firestore locales de `demo-roadmap-utepsa` y opt-in explícito. Las pruebas personales antiguas escriben datos reales y no forman parte de la suite rutinaria.

Si reaparece un cierre nativo, contrastar con imagen estándar API35 Google Play de 4 KB (ya instalada en el SDK) o un dispositivo físico, conservando Pixel_7 y sus datos. El intento de AVD adicional no terminó de arrancar con unos 270 MB de RAM libre y fue detenido; no acredita estabilidad de la imagen estándar. No borrar datos, desinstalar ni cambiar cuentas para solucionar un cierre.

## Diagnóstico de la espera tras el nuevo intento

El estudiante volvió a confirmar su historial y reportó «Seguimos esperando».
Se revisó el proceso vivo con Logcat y el depurador, sin reenviar el guardado ni
modificar estados personales. La pantalla de espera se activa a los 10 segundos;
ese aviso no implica fallo ni contiene un límite máximo de la operación.

Durante la revisión la operación terminó por sí misma. El trabajo de guardado
estaba completado y el ViewModel indicó guardando=false, esperando=false,
errorGuardado=false, confirmado=true y recuperacionPendiente=false. La captura
actual del dispositivo mostró «Tu historial está listo» y el botón «Ir a Malla».
Esta observación sustituye el pendiente anterior de confirmar el historial.
No se observaron errores de permisos ni un nuevo cierre en los registros revisados.

Antes de la optimización se identificó este cuello de botella: FirebasePreparacionSource.leer
abre una transacción que hace 52 lecturas individuales (50 estados, preparación y
diario). CoordinadorPreparacionRepository repite esa lectura y otra transacción
con las mismas lecturas para cada grupo de hasta cinco cambios, además de la
inicialización y la confirmación final. Con numerosas selecciones se acumulan
cientos de solicitudes seriales. Esto contribuye a la demora frente a las pruebas
locales de baja latencia; no se midió cuánto aporta cada solicitud de este intento.

## Optimización del guardado

Implementación, pruebas e instalación completadas. El primer intento de publicación
fue bloqueado por auto-review; el estudiante autorizó explícitamente «Sí, publicar
las reglas». Después se publicó y releyó el contenido productivo: resultado
`verified-published`, deploy exit 0, hash remoto
`7b70043cfb047d57009062fa1f3ebdd8b87f6cc444e90464855cdb37327479dd`.
La publicación no leyó ni escribió documentos de estudiantes; cambia las reglas
del contrato académico. Se conservaron los permisos de perfil/catálogo y no se
modificaron Auth, catálogo ni dependencias.

Cierre productivo en el teléfono físico: auditorías antes de instalar, después de
instalar y después de publicar acreditaron las mismas huellas de estados,
preparación y diario, sin operación activa. `ProductionPreparationReadOnlyTest`
pasó con la APK y reglas finales: 50 materias, Malla visible, misma sesión y
estados/preparación/diario sin cambios. La lectura académica productiva midió
4274 ms; la prueba completa, 10.817 s. La comprobación previa ya había completado
su lectura (4585 ms), pero falló al buscar Compose con el teléfono bloqueado.
Se añadió diagnóstico de etapa/código sin datos personales; el estudiante
lo desbloqueó y la verificación final pasó. No presentar esas pruebas como
un guardado productivo de 50 materias: no invocaron escrituras personales.

La APK final quedó abierta en MainActivity. Los servicios demo y los dos puertos
ADB reverse usados en las pruebas fueron retirados. El intento de restaurar la
APK respaldada durante la espera de autorización fue rechazado por Android con
INSTALL_FAILED_TEST_ONLY; no se aplicó la restauración ni se borraron datos.
Tras la autorización se mantuvo la APK optimizada comprobada. No hay pendientes
de publicación o instalación. Sin commits ni push.


Revisión actual solicitada tras comprobar la app en un dispositivo físico: se
reemplazaron las 52 lecturas individuales de cada transacción por un snapshot
completo protegido mediante `control_academico/revision`. `EstadosVersionados`
consulta revisión → estados/controles → revisión; solo acepta el conjunto si
ambas revisiones coinciden. Cada transacción comprueba de nuevo esa revisión;
si cambió invalida la copia local y relee antes de ejecutar la lógica CAS.
Todos los escritores productivos de Malla e Historial usan el mismo contrato,
incluido el acceso antiguo sin comparación explícita. Las reglas exigen avanzar
la revisión en el mismo commit para crear, actualizar o borrar estados y para
modificar preparación/diario. Una APK anterior podrá leer, pero deberá actualizarse
para escribir; no se debe reinstalar el cliente anterior tras activar las reglas.
Cualquier futuro escritor privilegiado Admin debe participar también del contador.

El contador ausente representa revisión cero; el primer guardado lo crea en uno.
No requiere migración ni reescritura administrativa de estados personales.
Se conservan grupos de cinco, huellas, validación curricular, diario recuperable,
conflictos, cancelación y comprobación final desde servidor. Solo se actualiza
la copia local después de recibir confirmación del commit. Un resultado incierto
invalida esa copia y se recupera releyendo; no se anuncia éxito de forma optimista.

Con 35 cambios: 8 transacciones × 3 lecturas + 5 llamadas finales = 29 llamadas
SDK, frente a las 421 de la versión previa. Con 50 cambios: 38 frente a 577.
Estos conteos excluyen carga inicial, escrituras, reintentos y la consulta adicional
de estados si una fuente transaccional se crea sin haber cargado previamente.
No equivalen a lecturas facturadas ni garantizan una duración concreta por Internet.
Las reglas de revisión añaden comprobaciones del servidor; no se confunden con
viajes individuales del cliente. La prueba local permite hasta seis estados más
el diario; el cliente conserva cinco y se probó el último grupo con preparación.

El catálogo completo validado (materias y prerrequisitos, sin estados personales)
se reutiliza en memoria hasta cinco minutos, por identidad exacta de sesión y
plan. La expiración, cambio de sesión o fallo en validación obliga a consultar de
nuevo. Perfil y estados siempre se releen desde servidor. Esto evita las 51
consultas adicionales al entrar a Malla después del historial. No se añadió Room,
catálogo alternativo dentro de la APK, dependencias ni almacenamiento privado nuevo.

Verificación local: 176 pruebas JVM, 0 fallos/errores; APK app/pruebas y lint
correctos, 0 errores y 39 advertencias. Reglas: 75 casos de perfil, 126 académicos,
94 de preparación y 26 del contrato de revisión, además de límites de escrituras.
La primera prueba de consultas denegadas encontró un fallo de traducción del
emulador Java en español (500); se reinició solo el servicio demo en inglés y
pasó. Los lotes de 50 altas denegadas superan el mensaje HTTP/2 del emulador;
la prueba de límites comprueba el rango útil 1..10, transacciones y 50 borrados.

En el teléfono físico V2529 pasaron siete casos SDK contra Auth/Firestore LOCALES,
con cuentas sintéticas: 50 cambios, catálogo frío/reutilizado con estados frescos,
primera confirmación, interrupción/recuperación/borrado, posposición, base obsoleta
y edición concurrente desde Malla entre grupos. La suite conjunta se quedó
esperando en el segundo caso; al aislar cada caso pasaron. No se atribuye esa
espera a una excepción de producción ni se oculta como una ejecución conjunta
correcta. Medición de 50 cambios: 3956 ms para confirmar; prueba completa 7976 ms.
Catálogo: 1344 ms frío y 375 ms reutilizado. Son mediciones contra servicios
locales desde el teléfono, no un benchmark de guardado productivo por Internet.

Evidencia: `app/build/reports/optimization-*.log`, `optimization-sdk-*.txt`,
`optimization-revision-rules.json`, `optimization-security-review.md` y
`optimization-rollout/`. Las reglas se mantienen como prototipo probado y deben
revisarse antes de distribuir ampliamente. El script de publicación específico
valida por defecto; exige APK instalada verificada, evidencia SDK/reglas y
precondición exacta de reglas remotas antes de aplicar. No reutilizar el publicador
histórico de reglas de preparación para este cambio.

La versión previa ya había reducido las relecturas entre grupos y usado una
consulta de colección para la lectura final (168 JVM y pruebas Android locales).
Sus tiempos sintéticos anteriores, 31.045 s y posteriormente 8.175 s en otro
entorno, no constituyen un antes/después controlado de esta medición física.
El runtime ART de Pixel_7 sigue sin declararse reparado; esta revisión usa el
celular y no borra datos del AVD.

## Revisión de historial y catálogo SAE

Implementación del 17/10/2026 solicitada por el estudiante: «Ver historial» abre
consulta sin escrituras; «Editar historial» usa selección precargada y elimina la
bienvenida/posposición de revisiones. Cancelar vuelve a consulta; solo pide
confirmación si hay cambios locales. Las correcciones guardadas conservan CAS,
conflictos, diario y relectura final. La configuración pospuesta puede confirmarse
sin reseleccionar las materias. La confirmación previa y la coherencia calculada
son condiciones distintas; una contradicción no borra estados ni revoca el marcador.

Los nombres tienen prioridad en listas, requisitos, revisión y conflictos. La
sigla permanece secundaria y la búsqueda admite nombres/siglas sin distinguir
tildes o mayúsculas. Los avisos distinguen configuración pendiente, contradicciones,
guardado pendiente y fallo de lectura; posponer usa estilo informativo.

Fuente principal nueva: [pénsum SAE](Pensum%20Ingenieria%20de%20Sistemas.pdf), impreso
el 17/10/2026. Se conservan los 50 UUID, el plan, áreas y nombres completos de la
malla anterior donde la impresión recorta siglas o nombres. El bloque de opciones
electivas no agrega materias obligatorias. Se añade horas_totales (85/170/400),
mostrando solo Modular/Bimodular/Duración especial; no se infieren meses.

El JSON SAE tiene 28 relaciones: añade SDA-303 → SIS-302 y SOI-304 → SOI-303;
retira SRA-304 → SRF-301, SES-302 → SES-301 y SES-303 → SES-302. Taller exige
45 aprobadas distintas del propio Taller. Se corrigió también el cómputo general
de habilitación para excluir la materia evaluada del umbral.

El lector acepta el esquema anterior completo (29 relaciones/40) y SAE completo
(28/45/horas), rechazando mezclas. La migración administrativa específica está en
`firebase/scripts/migrate-sae-catalog.cjs`; verifica catálogo anterior completo,
50 actualizaciones, 2 relaciones nuevas y 3 eliminadas, con precondiciones y commit
atómico. Su modo por defecto es simulación; exige evidencia de APK compatible
instalada y una simulación previa para aplicar. Nunca accede a estudiantes ni
publica reglas. El publicador de creación conserva su prohibición de sobrescribir.
El JSON anterior queda como baseline verificado en `firebase/catalogo/sistemas-rm-615-2016-v1.json`.

Verificación local: 173 JVM sin fallos; 17 pruebas Python y 18 Node del catálogo,
publicador y plan de migración, todas correctas. APK app/pruebas y lint compilados: 0 errores y 39 advertencias (incluyen tres recursos anteriores de conteos ahora sin uso).
Android: 7 casos UI de la APK final correctos, además de las 3 comprobaciones de
entrada/Perfil previas; se revisaron capturas limpias normales y con fuente al 200%.
La primera ejecución tuvo cierres del proceso y un aviso de System UI; el caso de
recuperación pasó al repetirlo sin interferir con el diálogo del sistema. Cinco
pruebas SDK con Auth/Firestore demo locales pasaron (incluidas recuperación parcial,
conflicto y 50 cambios). La primera suite SDK se lanzó antes de estar listo Auth;
se repitió tras comprobar ambos servicios y pasó. El caso sintético de 50 cambios
registró 8.175 s locales; no representa una medición de Internet ni una comparación
antes/después. Servicios demo detenidos.

El arranque inicial de Pixel_7 quedó offline con fallo OpenGL. Se reinició solo ese
AVD, sin borrar userdata, sin cargar/guardar instantáneas y con renderizado software;
volvió a conectar. No cambia su configuración permanente ni acredita reparar ART.
La auditoría adicional se corrigió para devolver Unit (JUnit exige void); el intento
con firma incorrecta no ejecutó lecturas ni escrituras.

Instalación y publicación completadas. Se instaló la APK compatible con `-r`,
conservando sesión y datos; la APK extraída del dispositivo coincide con el build
verificado. SHA-256: `8329f25bd1399b9a64396919f78504e7b10b0af28eea54e01e14ad71442c58b9`.
Antes y después de instalar se ejecutaron auditorías de solo lectura. Sus huellas
de estados, preparación y diario fueron idénticas y no había operación activa.

La simulación productiva encontró exactamente el catálogo anterior esperado.
La migración confirmó 55 escrituras atómicas (50 materias, 2 relaciones añadidas,
3 retiradas) y la relectura administrativa verificó el objetivo SAE completo.
No se escribieron estudiantes, perfiles ni reglas. El informe terminó `verified`,
con verificación `already-applied`. No reinstalar una APK anterior al lector
compatible sobre este catálogo: el viejo lector rechaza campos/relaciones SAE.

Una tercera auditoría, después de migrar, volvió a acreditar las mismas huellas de
estados, preparación y diario que antes de instalar. La prueba productiva de solo
lectura con MainActivity pasó: catálogo de 50 materias, Malla visible, misma sesión
y estados/preparación/diario conservados. La app final quedó abierta en MainActivity.
No se confirmó ni pospuso el historial personal como parte de estas comprobaciones.
Si el marcador ya estaba POSPUESTO por el flujo anterior, el estudiante debe revisar
y confirmar lo precargado desde Completar historial → Editar historial, sin volver
a seleccionar materias. No se infiere esa confirmación por tener aprobadas.

Evidencia del proyecto Android: `app/build/reports/sae-history-ui-final/`
(casos UI, capturas limpias, SDK y resumen de verificación) y
`app/build/reports/sae-rollout/` (instalación, simulación, commit, auditorías y lectura
productiva). Los informes personales contienen huellas/booleanos, no UID ni mapas
de materias. El fallo de firma inicial de la auditoría no ejecutó su cuerpo; la
APK de pruebas corregida pasó las tres auditorías. Sin commits ni push.

## Próxima acción pendiente

El contrato de revisión académica ya fue publicado con autorización explícita.
La APK compatible instalada coincide byte a byte con el build verificado:
`db1b4741446c837646fdbb12dc57a083223cd08dd2bd40fad89dd4ad1b11717c`.
No queda pendiente publicar estas reglas ni instalar la APK optimizada.
Medir el siguiente guardado que el estudiante necesite realizar; no borrar ni
reseleccionar historial para fabricar una prueba de velocidad por Internet.

Diagnóstico del reinicio reportado el 17/10, revisado con registros vivos: Logcat
capturó SIGSEGV de `system_server` a las 20:51:20 UTC (16:51:20 Bolivia), seguido
de `Exit zygote because system server (pid 639) has terminated`; volvió a caer
`system_server` a las 20:51:38 UTC y se registró la animación de arranque. Esto
acredita un reinicio del framework Android del emulador. Teclado, Teléfono,
Mensajes, búsqueda Google y NetworkStack también presentaron fallos; varias
trazas nativas pasan por ART (`libart.so`, resolución de métodos/Nterp).
Los fallos continuaban cuando `pidof` y `dumpsys meminfo` confirmaron que la app
ya no tenía proceso. La imagen activa sigue siendo API35 Google Play de 16KB,
build AE3A.240806.041/12890756, con GPU auto; no es el arranque software anterior.

El host tenía 243648 KiB (unos 238 MiB) de RAM física libre sobre 7558520 KiB;
hay presión de recursos, pero esa medición no demuestra que sea la causa de los
SIGSEGV. No hay Room en dependencias/código: la persistencia académica usa
Firestore. El proceso de la app también murió a las 20:50:16 UTC; el registro
capturado no permite atribuir ese cierre concreto a una excepción propia ni a
falta de memoria. No se debe declarar toda la app libre de errores ni datos
verificados por este diagnóstico: no se consultaron estados personales.

Evidencia local en `app/build/reports/emulator-diagnosis-20261017/`: Logcat,
índice DropBox, fallos nativos, historial de salidas y propiedades/memoria del
dispositivo. Solo se leyeron registros; no se reinició, reinstaló ni borró el AVD,
ni se modificaron datos académicos. La siguiente comparación útil es ejecutar
la misma APK en un dispositivo físico o AVD estándar de 4KB, con recursos
suficientes y un único AVD activo, conservando Pixel_7 y sus datos. La imagen
de 16KB es un entorno sospechoso; no queda aislado el defecto exacto de ART,
instantáneas, imagen o virtualización con esta evidencia.

Probar el nuevo flujo durante el uso normal. No queda pendiente publicar el catálogo
SAE ni instalar la APK de esta revisión. Si el historial quedó pospuesto anteriormente,
revisar y confirmar la selección existente explícitamente; no corregir su marcador
por administración ni reseleccionar materias para fabricar un estado de prueba.

El Pixel_7 quedó ejecutándose con renderizado software y sin ventana propia
(`-no-window`). Para uso con ventana propia, volver a abrir ese mismo AVD con
renderizado software y conservando userdata; no se ha verificado ese arranque con
ventana en esta revisión. Esto no declara reparada la
incidencia ART de la imagen de 16KB ni cambia su configuración permanente. No borrar
datos ni cambiar cuentas para tratar cierres del runtime.

Medir próximas modificaciones reales sin borrar historial para fabricar benchmarks.
El contrato de revisión global ya está implementado en esta optimización; medir
el próximo guardado real sin borrar historial ni repetir confirmaciones solo para
fabricar un benchmark. No quitar CAS silenciosamente. El estado vigente de Resumen, Oferta y Para ti figura en la tabla inicial y PRODUCTO_Y_PLAN.md.
