# Validación de RoadMap UTEPSA

## Ajustes de interfaz del 23 de octubre de 2026

El estudiante confirmó que los ajustes quedaron bien y autorizó subirlos al
repositorio. Es conformidad reportada; no añade una matriz física ni capturas
Android nuevas. Publicación preparada en `fix/cursando-amarillo-salida-perfil`.

Ampliación posterior de Oferta: consulta guardada y selector muestran fondo
`#FFF4CC` y etiqueta «Cursando» en materias EN_CURSO, leyendo `DatosOferta.estados`.
El resaltado no depende de la casilla del borrador. `assembleDebug lintDebug`
finalizó correctamente, con cero incidencias lint y arquitectura sin fallos
(94 fuentes/548 recursos). Log: `app/build/quality-offer-yellow-20261023.txt`.
APK debug actual: SHA-256 `9CF1512A833D1149B0068F0ACB170388FAAE7B8339AB58E689724043E8F4679C`; sustituye la APK de la primera pasada que se
registra debajo. No se repitieron JVM por este ajuste visual; las 300 pruebas
referidas abajo corresponden a la pasada anterior. El estudiante confirmó que los ajustes quedaron bien; no se registraron
escenarios ni dispositivo. No hubo instalación automatizada ni cambios de datos.

Alcance: amarillo pastel EN_CURSO en Malla, Historial y contador Cursando de
Resumen; cerrar sesión desde Cuenta, manteniendo alta inicial de Perfil y las
salidas de verificación y bloqueo biométrico confirmadas por el estudiante.
Se adaptaron los casos UI existentes para usar Cuenta y conservar la advertencia
del borrador de Oferta; no se ejecutaron en Android durante esta revisión.

- Arquitectura: 94 fuentes, 548 recursos, sin fallos.
- JVM: 300 pruebas, cero fallos, errores u omisiones.
- Contraste sobre `#FFF4CC`: acento `#735C00` 5,85:1; nombre `#20232B` 14,26:1;
  texto secundario `#636875` 5,06:1. Se conservan etiquetas y símbolo en Malla.
- Compilación APK debug/androidTest correcta; lint debug sin errores ni advertencias.
  Se eliminaron los tres recursos que quedaron sin uso al retirar las salidas.
- Sin dispositivo/emulador conectado (`adb devices` vacío). La aceptación del estudiante se registra arriba;
  no hubo ejecución automatizada en Android ni capturas definitivas nuevas.

Comando: `gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --offline --max-workers=1`
con JDK de Android Studio, caché Gradle existente y heap de 768 MB. Log local
ignorado por Git: `app/build/quality-final-ui-20261023-clean.txt` (BUILD SUCCESSFUL).
Las dos pasadas anteriores quedan en `quality-final-ui-20261023.txt` y
`quality-final-ui-20261023-final.txt`; el resultado limpio corresponde a la última.
APK debug: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256
`06207440843B5C36A1DE26015B4D576D837421A576AAB880A0FAD48760055BA6`.
Los cambios documentales anteriores se conservaron. No hubo instalación, pruebas
sobre cuenta personal, despliegue ni migración. La publicación posterior fue autorizada por el estudiante. La evidencia del
21/10 que figura debajo sigue correspondiendo únicamente a aquella revisión.




Consolidada el 19/10/2026, hora de Bolivia, sobre la base funcional publicada
`0192056`. Este documento distingue pruebas locales, evidencia del compañero,
servicios demo y comprobaciones productivas. Limpiar la documentación no ejecuta
nuevamente esas pruebas ni amplía su alcance.

## Conformidad final e integración · 19/10/2026

Tras el cierre de verificaciones, el estudiante confirmó «está todo ok» y
autorizó subir el avance al repositorio y actualizar las guías. Se integra en
main desde la base `0192056`, preservando Cuenta, autenticación, avisos,
reorganización, textos y documentación. La restricción anterior de commit/push
deja de aplicar a esta entrega. No se ejecutan despliegues ni instalaciones.

Es una conformidad final reportada por el estudiante, sin nueva matriz de casos
ni hash de APK. Se mantiene la aceptación física previa satisfactoria, incluidos
Google→contraseña y acceso por ambos métodos. El SDK demo sigue 6/7 y no exige
repetir al estudiante ese recorrido ya comprobado. La PC limitada y el AVD
personal inestable justifican priorizar pruebas físicas; no se declara reparado
el emulador ni acreditado el push remoto por esta confirmación.

Se conservan los resultados automatizados y los intentos fallidos de las etapas
siguientes como evidencia fechada. Los informes bajo app/build siguen siendo
locales e ignorados por Git; los resultados resumidos y hashes finales quedan
en estas guías para ambos integrantes:

| APK final | SHA-256 |
|---|---|
| Debug | `8ae47cb817a07722da1edfd2b526b4dbf59e861424594bd2e5f16e63b2fabb0e` |
| Release unsigned | `9f2d2c89b3d4d38619a0b455b332b93edfeadd48e978ba061bb98198e10e98f5` |
| AndroidTest | `69bca23341b16e004b21ae8d90039fcfaf93095de0423bdf62a0c256b1cdc590` |

## Reorganización y lenguaje · 19/10/2026

Trabajo sobre el checkout local de `feat/cuenta-autenticacion`, conservando Cuenta,
avisos y documentación sin confirmar. No se reemplazó por main ni se añadieron
dependencias/módulos. Sin publicación Firebase, instalación en el teléfono,
pruebas personales, commit, push ni PR. El AVD sintético y los servicios demo
ya estaban disponibles; no se utilizó Pixel_7 ni se borraron sus datos.

| Comprobación de esta etapa | Resultado |
|---|---|
| JVM | 300/300, cero fallos/errores/omitidas. Las 299 anteriores más la evaluación independiente del borrador de comparación. |
| APK debug, release y androidTest | Compiladas con la redacción final. Release es la variante unsigned del proyecto. |
| Lint debug/release | 0 errores y 1 aviso en cada variante: disponibilidad de Gradle 9.8.2 frente al wrapper 9.6.0 conservado. Sin baseline ni supresiones nuevas. |
| Regresión Android principal | 71/71 con la redacción final y el ajuste de teclado/desplazamiento de la prueba de Perfil. |
| Avisos, permiso y persistencia | 18/18 UI, 1/1 con permiso denegado y 2/2 fases de persistencia entre procesos con la APK final. |
| SDK Auth/Firestore demo | Nueva ejecución final 6/7: EMAIL_EXISTS en Google→contraseña; los otros seis casos pasan. El runner termina con error porque conserva el caso fallido. |
| Dependencias y textos | `check-architecture.py`: 94 fuentes, 551 recursos, cero infracciones. Sin paquetes fuente vacíos, `.gitkeep` ni imports de ubicaciones retiradas. |
| Diferencias y artefactos | `git diff --check` correcto; hashes SHA-256 y tamaños finales renovados en `artifacts.json`. |

Mapa y reglas en el README raíz y AGENTS.md. Contratos/modelos en dominio,
adaptadores/transacciones/DataStore en datos, pantallas/estados/ViewModels por
función y Credential Manager/biometría/avisos Android en plataforma. Se mantienen
un único AcademicViewModel, UID/versiones, propietarios y alcance de corrutinas,
CAS, diario, relecturas y descarte de respuestas tardías. La comparación de catorce
archivos de reglas/motor/persistencia con la base, ignorando solo paquetes,
imports y espacios, conserva sus cuerpos: `core-preservation.json`. Esto no
certifica por sí solo todo el comportamiento ni sustituye las regresiones.

Los textos están en recursos por función; los estados usan identificadores y las
validaciones puras categorías. Los adaptadores convierten errores de acceso y
conservan causas internas, propagando cancelaciones. Se retiraron detalles de
Firebase, permisos internos, credenciales, servidor, oferta remota/revisiones de
versión, caracteres de control y cambios «en cascada». EMAIL_EXISTS usa la
explicación neutral solicitada. Se conservan recuperación neutral y acciones para
reconciliar guardados inciertos. FCM conserva la recepción y ya no registra tokens.

### Evidencia y correcciones verificadas

Informes locales ignorados por Git en `app/build/reports/reorganization/` y
`app/build/reorganization-*.txt`. `reorganization-delivery-build.txt` contiene la
compilación previa completa. La comprobación retomada está en
`reorganization-resumed-checks.txt`: BUILD SUCCESSFUL, incluidas JVM, las tres
APK y lint debug/release. `artifacts.json` registra los hashes renovados de las APK.
`architecture.json`, `text-inventory.json` y `kotlin-literals.json` permiten revisar
dependencias, recursos XML y los literales Kotlin restantes (claves, formato,
anotaciones y diagnóstico interno). Los informes no viajan automáticamente en un clon.

La pasada inicial de JVM estaba UP-TO-DATE. Lint falló durante su análisis y
coincidió con renombrados: no es una base completa ni se atribuye como problema
preexistente. Se compiló por etapas y se conservaron los intentos fallidos.
La suite detectó un catálogo inválido evaluado demasiado pronto (298/299);
se conservó su prueba y el tratamiento de error de Resumen. Se corrigieron
imports, indentación y recursos sensibles a configuración con LocalResources.
La primera UI terminó 66/70: tres simuladores aún entregaban excepciones SDK
directamente a presentación y una prueba intentaba desplazar un botón sin
contenedor desplazable. Se adaptó la frontera usando el clasificador real y se
mantuvieron las aserciones de cancelación, red, configuración y no exposición del
mensaje crudo. El diálogo de vinculación permite desplazar el contenido largo.
Los fallos permanecen en `UI-first-66of70.txt`.

Después de la redacción final, una pasada terminó 70/71: al guardar Perfil, el
nodo «Tu cuenta» existía pero quedaba fuera de la zona visible. Se conserva en
`UI-wording-first-70of71.txt`. Se ajustó únicamente esa prueba para cerrar el
teclado y desplazar el botón de guardado y el título; conserva la comprobación
del nombre guardado y el retorno a Cuenta. La compilación iniciada después se
interrumpió por petición del usuario: `reorganization-delivery-checks.txt` no
acredita éxito. La comprobación retomada sí terminó correctamente y reutilizó
los resultados JVM vigentes de 300 pruebas, sin fallos, errores ni omitidas.
La regresión retomada terminó 71/71 en `reorganization-resumed-ui.txt` y
`account/Regressions.txt`; no se retiraron aserciones para conseguir ese resultado.
Avisos terminó con 18/18 UI, permiso denegado 1/1 y persistencia seed/check 1/1
cada una. Evidencia en `reminders/UI.txt`, `PermissionDenied.txt`,
`Persistence-seed.txt` y `Persistence-check.txt`; logs de los runners en
`reorganization-resumed-reminders-*.txt`. Se usaron las guardas existentes y
solo el AVD sintético autorizado.

El primer intento SDK retomado terminó 0/7 por conexión rechazada a Auth demo:
los servicios que respondían al inicio dejaron de estar disponibles. Se conserva
en `reorganization-resumed-sdk.txt` y `sdk-unavailable/`, sin atribuirlo al caso
EMAIL_EXISTS. No había procesos escuchando en 9099/8089 al diagnosticarlo.
La caché npx de CLI estaba incompleta (faltaba uuid); se reutilizó la CLI 15.34.0
ya instalada en `app/build/account-tools/`, indicada por el log previo, sin instalar
paquetes. Se iniciaron únicamente Auth/Firestore demo y se prepararon sus fixtures:
`reorganization-resumed-demo-services.txt` y `reorganization-resumed-demo-fixtures.txt`.
La repetición con servicios disponibles terminó 6/7 en
`reorganization-resumed-sdk-final.txt`, `account/SDK.txt` y `account/SDK-results.json`.
Google→contraseña falla durante link password con `AccessFailure: EMAIL_EXISTS`,
conservando la causa SDK. Pasan verificación con acceso privado protegido,
contraseña→Google con UID/perfil conservados y acceso por ambos métodos,
recuperación neutral, reemplazo/conflicto de Oferta, lectura estable de
recomendaciones y posposición persistida de Historial. Cada caso corrió en un
proceso separado. No se cambió la vinculación ni se declaró resuelto el fallo.
Al terminar se detuvieron los servicios demo iniciados para esta repetición;
se comprobó que 9099/8089 ya no tenían procesos escuchando. El AVD se conservó.

Las tres nuevas pruebas de Cuenta verifican mensaje neutral a fuente 1,5,
validación/retorno del diálogo ampliado y envío de verificación aunque cambie la
composición de entrada. Dos comprobaciones de recursos protegen lenguaje neutral
y guardados inciertos. Las capturas de Cuenta, Historial y Oferta están en las
subcarpetas de ese informe; se revisaron diálogos, conflicto, descarte y textos
largos a fuente 1,5. El umbral, los datos y las acciones funcionales se conservan.
Se renovaron las capturas con la APK final: ocho en
`account/account-ui-captures/`, veinte en `history-final/` y diez en `offer-final/`.
La revisión visual final comprobó EMAIL_EXISTS y el diálogo de vinculación a
fuente 1,5, conflicto y selector ampliado de Oferta, consulta ampliada de Historial,
conflictos de historial/estado y guardado sin confirmar. Los mensajes y acciones
revisados son legibles, distinguen borrador y selección guardada y no anuncian
éxito ante incertidumbre. Las carpetas anteriores conservan evidencia de pasadas previas.

### Repetir de forma segura

```powershell
python scripts/check-architecture.py
.\gradlew.bat testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest lintDebug lintRelease --max-workers=1
.\scripts\test-account.ps1 -Mode Regressions -OutputDirectory app/build/reports/reorganization/account
.\scripts\test-reminders.ps1 -Mode UI -SkipInstall -OutputDirectory app/build/reports/reorganization/reminders
.\scripts\test-reminders.ps1 -Mode PermissionDenied -SkipInstall -OutputDirectory app/build/reports/reorganization/reminders
.\scripts\test-reminders.ps1 -Mode Persistence -SkipInstall -OutputDirectory app/build/reports/reorganization/reminders
.\scripts\test-account.ps1 -Mode SDK -SkipInstall -OutputDirectory app/build/reports/reorganization/account
```

SDK exige preparar antes los fixtures Auth/Firestore demo mediante los comandos
del apartado de entorno de esta guía. Los runners conservan guardas de AVD/proyecto
y resuelven clases por función mediante `scripts/test-class-names.ps1`.
No ejecutar la instrumentación completa. Esta etapa no vuelve a acreditar Google
real, recepción push remota, biometría física ni producción; tampoco convierte la
aceptación manual de la APK anterior en aceptación manual de esta reorganización.

## Calidad y avisos de pendientes · actualización del 19/10/2026

Esta ejecución nueva parte de los cambios locales de Cuenta, que se conservaron.
No incluye despliegues Firebase, migraciones, pruebas personales, commit, push ni PR.
Los resultados anteriores se mantienen debajo como evidencia fechada; no son resultados
de esta actualización. Los informes nuevos están en `app/build/reports/quality-reminders/`
y `app/build/quality-*.txt`, ignorados por Git.

### Inventario de herramientas

| Componente | Antes | Ahora |
|---|---|---|
| AGP / Gradle | 9.3.3 / 9.5.0 | 9.4.1 / 9.6.0 |
| Kotlin integrado / plugin Compose | 2.2.10 | 2.4.21 alineados |
| Compose BoM | 2026.02.01 | 2026.09.00 |
| Firebase BoM | 34.3.0 | 35.0.2 |
| Credentials / Play Services | 1.5.0 | 1.6.0 |
| Google ID | 1.1.1 | 1.2.1 |
| DataStore | 1.1.7 | 1.2.1 |
| Coroutines producción / pruebas | 1.9.0 | 1.11.0 |

Versiones explícitas. Se conserva la biometría ya existente, sin añadir versiones
preliminares. Sin cambios de minSdk 35, compile/target 37, paquete, firma debug ni
JVM 11. Se conserva Kotlin integrado: el classpath KGP y el plugin Compose comparten
la versión del catálogo, sin aplicar kotlin-android.
[Compatibilidad oficial AGP](https://developer.android.com/build/releases/agp-9-4-0-release-notes).
Wrapper validado contra el [checksum oficial](https://services.gradle.org/distributions/gradle-9.6.0-bin.zip.sha256):
`bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01`.

Los cuatro grupos se compilaron y pasaron la suite JVM entre actualizaciones.
Evidencia: `quality-group1-tools-fixed.txt`, `quality-group2-compose.txt`,
`quality-group3-storage.txt` y `quality-group4-firebase.txt`. El primer grupo falló
antes de explicitar el tipo FirebaseAuth del listener: Kotlin 2.4 exponía una
anotación Checker Framework inaccesible en el tipo inferido. No se añadió esa
biblioteca ni se alteró la lógica de sesión. También se migró assets.srcDir a
assets.directories y se retiraron expresiones redundantes señaladas por Kotlin.

[Revisión SDK Firebase](https://firebase.google.com/support/release-notes/android):
Auth 25.0.0 añade el error de política de contraseña, ahora reconocido por la app;
Firestore 27.0.1 cambia transporte gRPC/Protobuf y corrige dependencias de pruebas
publicadas accidentalmente. El mínimo de estos SDK es 24, inferior al 35 existente.
Se mantienen linkWithCredential, recarga/token forzado y Source.SERVER/CAS.
FCM 26 conserva la recepción mediante tokens actuales: hay excepciones de
compilador limitadas a onNewToken y fetchTokenForDebug, documentadas en esos métodos.
Son deprecaciones reales, no falsos positivos: migrar a register/FID cambiaría la
identificación del destinatario, fuera de estos avisos locales. No se silencian
categorías lint. [API oficial](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessaging).

### Inventario lint

| Grupo original | Cantidad | Resolución |
|---|---:|---|
| Plurales | 13 | Diez recursos convertidos/compuestos con sus cantidades; tres textos sin referencias retirados. |
| Recursos sin uso | 11 | Búsqueda en código, XML y pruebas; siete colores y cuatro textos eliminados. |
| Versiones disponibles | 10 | Matriz explícita anterior, validada por grupos. |
| Iconos duplicados | 2 | Diez WebP alternativos retirados; se conservan los dos iconos adaptativos y sus vectores. |
| Modifier | 1 | Primer opcional de HistoryCard; llamadas posicionales usan accent nombrado. |
| Etiqueta redundante | 1 | Activity hereda app_name de application. |
| Ortografía | 1 | tools:locale=es; «momento» conservado. |

Declarar español detectó cantidades `many` ausentes: se completaron también en
los plurales anteriores de Oferta, Historial y Para ti. Primera pasada nueva:
**lint debug y release sin incidencias**. No se añadió baseline ni exclusiones lint.
Los avisos de compilación sobre .so sin strip indican que AGP empaqueta las
bibliotecas recibidas tal cual; ambas APK compilan. No se eliminaron bibliotecas
ni se cambiaron sus binarios para ocultar esos mensajes.

Se migraron los entornos Compose de pruebas a los imports v2; las aserciones
funcionales se conservan. [Guía oficial de migración](https://developer.android.com/develop/ui/compose/testing/migrate-v2).
Las pruebas personales solo recibieron el import para seguir compilando; no se ejecutaron.

### Regresión final y dispositivo

Confirmación manual adicional del estudiante el 19/10/2026, después de ejecutar
la app en su dispositivo físico: cuenta creada únicamente con Google → añadir
contraseña correctamente → cerrar sesión → acceder con correo/contraseña →
acceder nuevamente con Google. Reporta los mismos datos conservados y funcionamiento
satisfactorio. Se acepta manualmente ese recorrido productivo; no se inspeccionó
el UID por instrumentación ni se registró el hash de la APK usada en esta prueba.
No acredita por sí solo los avisos nuevos ni todos los escenarios de hardware.
El fallo demo EMAIL_EXISTS permanece registrado por separado; este reporte no
convierte la suite automatizada 6/7 en 7/7 ni requiere modificar linkWithCredential.

Aceptación manual final reportada por el estudiante después de ejecutar todos los
pasos entregados en este chat: «todas funcionaron bien», sin anomalías registradas.
Incluye bloqueo antes de verificar correo, reenvío y acceso después de verificar;
avisos de Historial y Oferta con navegación, prioridad y deduplicación; cancelación
al desactivar, conservación de deduplicación al reactivar y permiso bloqueado con
acceso a ajustes; biometría correcta, cancelación sin acceso y bloqueo al regresar
del segundo plano o del bloqueo de pantalla. Quedan aceptados esos escenarios
en el dispositivo físico. Es reporte manual, sin capturas ni hash adicional de APK;
no constituye una auditoría exhaustiva ni acredita recepción push remota.

Resultados finales automatizados de calidad: **299 JVM**, **68 pruebas Android
existentes** y **19 comprobaciones Android nuevas** aprobadas (16 UI/recursos/rutas,
una de permiso denegado y dos fases de persistencia con force-stop intermedio).
APK debug, release y androidTest compiladas; lint debug/release **0 errores/0 avisos**.
Evidencia: `quality-reminders-visual-final-build.txt`, `quality-base68-runner.txt`
y `quality-reminders/UI.txt`, `PermissionDenied.txt`, `Persistence-seed.txt` y
`Persistence-check.txt`, bajo las carpetas de informes indicadas arriba.

SDK actualizado: **6/7**, confirmado en `account-auth/SDK-results.json` y `SDK.txt`.
El único fallo es Google→contraseña con EMAIL_EXISTS, contrastado con éxito manual
productivo conservando datos. La ejecución agrupada inicial falló también por
«FirebaseApp was deleted» después del teardown de una FirebaseApp secundaria.
El runner ahora ejecuta cada caso en un proceso de instrumentación separado y
force-stop entre casos; eliminó los seis fallos de preparación sin retirar
aserciones y conserva salida de error si falla cualquier caso. Evidencia por caso:
`account-auth/SDK-1.txt` a `SDK-7.txt`; intento agrupado conservado en
`quality-reminders/SDK-grouped-first-0of7.txt`.

Los intentos de UI previos 9/16 y 15/16 se conservan en `quality-reminders/UI-first-9of16.txt`,
`UI-second-15of16.txt` y `UI-third-15of16.txt`: se corrigieron expectativas de etiquetas
para Oferta vacía, seguimiento de Activity tras setIntent y sincronización de
NotificationManager. La primera fase de persistencia no ejecutó por firma JUnit
no void; se corrigió con Unit explícito antes de aprobar ambas fases.

Los runners
`test-account.ps1` y `test-reminders.ps1` exigen el AVD sintético conocido. Este último
separa UI, permiso denegado y persistencia con cierre real del proceso entre fases.

### Reporte general de seguridad: alcance conservado

Se mantiene el reporte previo de reglas demo: aislamiento por propietario/UID,
verificación de correo, perfil completo, esquemas exactos y contratos académicos
(CAS, revisión, diario y Oferta). Incluye las 13 comprobaciones de Cuenta registradas
abajo. **Esta etapa no vuelve a ejecutar esa auditoría automatizada de reglas**;
no equivale a un pentest ni a certificar producción. La nueva regresión de avisos
cubre puertas locales, UID esperado, respuestas tardías y aislamiento de preferencias;
los siete casos SDK demo se registran por separado. Sigue pendiente la revisión
antes de distribución amplia. Sin publicaciones remotas en esta actualización.

## Cuenta y autenticación: validación de la rama local

Rama `feat/cuenta-autenticacion`, con aceptación manual de los escenarios acordados
completada por el estudiante. Los resultados siguientes son la etapa anterior de
Cuenta; los resultados actualizados de calidad y aceptación están arriba.
Se conservaron las modificaciones documentales previas. La APK de main instalada
antes de esta continuación no acreditaba estas funciones; las actualizaciones de
Cuenta y el reporte manual posterior están documentados en esta guía.

- JVM: **283 pruebas, cero fallos/errores** en la continuación del 19/10.
  Incluye validación compartida, verificación, espera monotónica de 60 s, error de lectura de DataStore,
  proveedores, sesión estable, conflictos, respuestas tardías y máquina de bloqueo.
  Incluye la regresión de EMAIL_EXISTS: ese código no demuestra que la credencial
  pertenezca a otra identidad; el mensaje conserva el acceso actual y permite revisar métodos.
  La regresión `backgroundDuringPreferenceLoadingRevokesInteractiveUnlock` falló
  antes de corregir SessionLock: salir al segundo plano durante la lectura de
  DataStore no revocaba la entrada interactiva y podía dejar abierta una sesión
  protegida. Ahora la revoca aunque la preferencia siga cargando; cuando llega
  habilitada, exige desbloqueo. Rotación y selector externo conservan sus excepciones.
  Evidencia antes/después: `continuation-lock-race-before.txt` y
  `continuation-lock-final-build.txt`, sin simular hardware biométrico.
- Compilación final de ambas APK correcta (`continuation-lock-final-build.txt`),
  más comprobación del harness (`continuation-final-harness-check.txt`);
  lint **0 errores/39 advertencias**.
- UI final del 19/10: **68/68 aprobadas**, una ejecución completa de 175,051 s con
  la APK que incluye la corrección de SessionLock. Nueve de Cuenta más 59
  regresiones de Resumen, Malla, Historial, Oferta, Para ti y errores de acceso.
  Verifica la recarga exacta de Para ti tras Perfil/Cuenta, guardado/cancelación
  de Perfil, conservación de filtros/borradores/revisión y bloqueo sintético.
  Log: `account-auth/Regressions.txt` y `continuation-ui-complete-runner.txt`.
  No fue un primer intento verde: quedaron dos repeticiones de 67/68, por fila
  fuera del viewport en Resumen y touch sobre fila retirada en Oferta con fuente 1,5.
  Se cerró explícitamente el teclado, materializó la fila mediante su LazyColumn,
  desplazó hasta ella y ejecutó su acción accesible tras afirmar visibilidad.
  Se conservaron las aserciones de selección, guardado, revisión y recargas;
  estas selecciones sintéticas ya no acreditan inyección táctil de esas filas.
  Evidencia fallida: `continuation-ui-first-result.txt` y `continuation-ui-second-result.txt`.
  El 67/68 anterior a la pausa había detectado la doble recarga de Para ti;
  el informe actual sí acredita su corrección. No acredita Firebase real ni hardware.
- Reglas demo: regresiones Perfil, Malla, Historial, revisión y Oferta correctas
  con el runner existente. **13 comprobaciones nuevas** deniegan lectura de las
  ocho rutas privadas y escritura de perfil sin verificar; admiten consulta propia
  verificada/Google y rechazan anónimo/otro UID.
- SDK demo final del 19/10: **6/7 correctos**, ejecución de 20,437 s. Cuenta pasa
  3/4; Oferta, Recomendaciones y posposición de Historial pasan sus tres regresiones.
  Log: `account-auth/SDK.txt` y `continuation-sdk-runner.txt`; el runner devuelve
  error por la prueba fallida y no la ignora. Se acredita registro/verificación mediante OOB real del
  emulador y token forzado, recuperación y nueva contraseña mediante OOB, y
  correo→Google con acceso posterior por ambos métodos al mismo UID/perfil.
  Se amplió esta última prueba para comprobar también estados académicos guardados.
  **Google→contraseña falla con EMAIL_EXISTS**. Se reprodujo también por REST
  local: el emulador comprueba correo existente en accounts:signUp antes de
  resolver idToken. Existe un [reporte histórico con síntoma similar](https://github.com/firebase/firebase-android-sdk/issues/5659).
  Esto no prueba que producción falle igual, ni acredita éxito de ese sentido.
  La prueba permanece fallida, sin ignorarla, cambiar dependencias Android,
  fusionar identidades o reemplazar linkWithCredential por otro contrato.
- Figma: nueve estados editables añadidos en `73:813`; tipografía Roboto, controles
  existentes, cero capas con capturas de UI. Se corrigió el ajuste de etiquetas y
  se revisó la exportación final del índice visual. Ayuda mínima de ocho caracteres
  actualizada en Registro y Añadir contraseña.
- Biometría: máquina JVM y pantalla sintética; en V2529 actualizado el estudiante
  respondió a la solicitud de probar activación/cancelación/reintento, regreso del
  segundo plano, rotación y conservación de borrador: «funciona bien aparentemente»
  y solo tiene huella configurada. Es aceptación manual reportada hasta ahora,
  sin resultados enumerados por escenario. No acredita rostro, indisponibilidad,
  bloqueo temporal ni recreación del proceso; tampoco simular callbacks los acredita.

### Aceptación manual reportada en producción (19/10/2026)

Tras la activación remota, el estudiante reportó que creó una cuenta nueva con
correo/contraseña y completó el flujo académico: registro de historial, registro
de Oferta, consulta de Resumen y uso general sin fallos observados. También
recuperó/cambió la contraseña y vinculó Google conservando el progreso visible.
Es evidencia manual productiva de **correo → Google**, además del reporte previo
de huella en V2529. No se volvieron a ejecutar pruebas automatizadas por actualizar
esta documentación: permanecen 283 JVM, 68/68 UI y SDK demo 6/7.

No describió por separado el envío/enlace/reenvío de verificación ni intentos de
acceso sin verificar, aunque sí reportó entrada al contenido tras el alta normal.
Posteriormente confirmó haber completado satisfactoriamente los puntos 1 y 2:
Google→contraseña desde una cuenta inicialmente solo Google y nuevo acceso con
ambos métodos conservando la información. Es aceptación manual reportada en
producción; no se midió el UID mediante API ni se inspeccionaron cuentas personales.

Ambos sentidos de vinculación y el nuevo acceso por ambos métodos ya tienen
aceptación manual productiva satisfactoria. El fallo del emulador sigue documentado
y no se convierte en 7/7 por estas pruebas manuales; su limitación local queda
contrastada con el reporte de éxito productivo. Completar la comprobación explícita de verificación
obligatoria y los escenarios de hardware aún no enumerados (recreación de proceso,
cancelación/rechazo e indisponibilidad cuando se puedan reproducir).

### Producción: publicación autorizada y verificada

Consulta previa de configuración real de solo lectura, repetida el 19/10 antes de activar: correo/contraseña deshabilitado,
política adicional no configurada, privacidad de correo activada y plantillas de
verificación/recuperación existentes con handler web Firebase. No se exportaron
usuarios, secretos ni cuerpos de plantillas. La app exige ocho caracteres; Firebase
conserva su política remota actual hasta que se revise su configuración.

Reglas candidatas compiladas en servidor con **dry-run correcto**. Comparación:
solo cambia `isOwner`; el resto conserva exactamente sus tokens (CAS, revisión,
diario y Oferta incluidos). El comparador histórico genérico marca ese bloque
como distinto por diseño; `prepare-account-rollout.cjs` comprueba de forma específica
que los demás bloques sean iguales. Remoto leído SHA-256
`4fc14fb7e06bf3de81b8791bf67db1942202c191868561a8bfbbe61da8ecfc02`;
candidato `06702f203f93a60a485188407cbecae7705038facb74c10e495c67a3342fd870`.
Propuesta exacta en `firebase/account-auth-config.proposed.json`.
Tras la consulta previa y la revisión, el estudiante autorizó explícitamente
publicar esas reglas y habilitar correo. El 19/10 se ejecutó con
`publish-account-auth.cjs --out app/build/reports/account-auth/production-rollout-20261019-final --apply`.
Resultado **verified-published-and-enabled**, deploy código 0 y relecturas correctas:
reglas remotas con hash candidato exacto y `enabled=true,passwordRequired=true`.
Release: `projects/roadmap-utepsa-c9409/rulesets/0a9adb3d-42ab-4482-93d8-3c39a81c2430`,
actualizada a `2026-10-19T12:23:51.942502Z` (08:23:51 Bolivia).
El hash de todos los campos Auth fuera de los dos autorizados se conservó:
`4167bfbdf198d088a77fd47f459c6a3242d85cf07d00ddac2f3c99ce2eb87356`.
No se leyeron/escribieron documentos ni se modificaron cuentas de usuario.
La aceptación real posterior de correo/recuperación/correo→Google está reportada
arriba, junto con Google→contraseña y nuevos accesos por ambos métodos. Falta
completar verificación explícita y escenarios restantes de hardware.

El primer intento (`production-rollout-20261019`) abortó antes de publicar porque
la API omite campos de email cuando está deshabilitado. Se adaptó únicamente la
precondición a ese formato y se volvió a releer todo antes de ejecutar el intento
final. El publicador no repite publicaciones si cambia el hash/release/configuración
y deja resultados inciertos como `requires-reconciliation` para relectura.

Evidencia local: `app/build/reports/account-auth/`, `account-final-build.txt`,
`account-ui.txt`, `account-academic-regressions.txt`, `account-sdk-first.txt` y
`account-rest-diagnosis.cjs`. Informes ignorados por Git. CLI Firebase 15.34.0
preparada aisladamente en `app/build/account-tools`; no cambia dependencias de la APK.
Auth/Firestore demo en 9099/8089 y AVD aislado
`RoadMap_Recommendations_Test_API35`, serial `emulator-5556`.

Repetir con `scripts/test-account.ps1 -Mode UI|Regressions|SDK` tras compilar ambas
APK. El runner exige el AVD aislado y devuelve error si hay una sola prueba fallida;
SDK requiere los servicios/fixtures demo y añade los opt-in de cada clase. Nunca
incluye PersonalAcademicPersistenceTest ni pruebas personales.

### Diagnóstico preciso de Google → contraseña (19/10/2026)

Android usa Firebase BoM 34.3.0 y Auth 24.0.1; el emulador local es Firebase CLI
15.34.0. No se cambiaron esas dependencias. El diagnóstico versionado
`node firebase/scripts/diagnose-account-link-demo.cjs` solo contacta
`127.0.0.1:9099`; comprueba proyecto demo en el token y nunca imprime tokens ni
contraseñas. Devuelve código 1 si la vinculación falla, sin convertirla en éxito esperado.

Reproducción nueva: acceso Google y reautenticación conservan UID;
`accounts:signUp` con idToken/correo/contraseña devuelve HTTP 400 EMAIL_EXISTS.
La relectura conserva el UID y solo `google.com`; volver a acceder con Google
recupera ese UID y acceder con la contraseña nueva sigue rechazado.
Informe local: `account-auth/link-demo-diagnosis.json`.

En el [código oficial de CLI 15.34.0](https://github.com/firebase/firebase-tools/blob/v15.34.0/src/emulator/auth/operations.ts),
`signUp` comprueba `getUserByEmail` antes de `parseIdToken`. Por ello el correo
de la propia identidad Google dispara el rechazo en esta reproducción local.
Esto explica el fallo del emulador; no prueba el comportamiento del backend real.
El [reporte Android #5659](https://github.com/firebase/firebase-android-sdk/issues/5659)
describe un síntoma similar con Auth 22.3.0 y está cerrado; no acredita una solución
ni el resultado de Auth 24.0.1 en este proyecto. La advertencia de incidencia en
la [guía de vinculación](https://firebase.google.com/docs/auth/android/account-linking)
enlaza otro caso (#7675, usuario anónimo/JS), que tampoco demuestra equivalencia.

Se conserva `linkWithCredential`, la reautenticación y la prueba SDK fallida.
No usar accounts:update, recuperación de contraseña, fusión, downgrade ni un
emulador parcheado para declarar satisfecha la prueba demo. El estudiante confirmó
Google→contraseña y ambos accesos en producción sin fallos, conservando datos.
Si aparece una regresión, conservar código de error y métodos releídos para
decidir una solución justificada; la prueba demo fallida permanece visible.

### Revisión del paso productivo

`prepare-account-rollout.cjs` se repitió el 19/10 con el snapshot remoto conservado:
solo cambia isOwner y conserva tokens de todos los contratos académicos. No es una
consulta remota nueva ni una publicación. La propuesta continúa limitada a
`signIn.email.enabled,signIn.email.passwordRequired`; no cambia Google, plantillas,
privacidad, política remota de contraseñas ni datos personales.

Antes de ejecutar: obtener autorización explícita, releer release y configuración,
comparar hashes revisados y detenerse si cambiaron; publicar reglas y verificar su
relectura antes de habilitar correo con la máscara exacta. Registrar la configuración
anterior para poder revertir solo el proveedor si la aceptación falla; no retirar
la protección de verificación de las reglas con cuentas de correo ya creadas.
Aceptar correo/verificación/recuperación, ambos proveedores al mismo UID e historial
y biometría real antes de cerrar T5. La publicación y activación ya se ejecutaron
con esas comprobaciones y autorización; no repetirlas. Sigue pendiente la
verificación explícita y escenarios restantes de hardware. Registro, recuperación,
ambos sentidos de vinculación y nuevos accesos con ambos métodos ya tienen
reporte manual productivo satisfactorio.
El método de actualización con máscara exacta sigue la
[API oficial projects.updateConfig](https://docs.cloud.google.com/identity-platform/docs/reference/rest/v2/projects/updateConfig).

## Versión integrada: comprobación en esta computadora

Gradle `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug
:app:assembleDebugAndroidTest --offline --max-workers=1`, JDK de Android Studio,
heap 768 MiB: **BUILD SUCCESSFUL**, 3 min 19 s, 30 tareas ejecutadas/52 al día,
caché de configuración reutilizada.

- Informes JVM: **258 pruebas, cero fallos/errores**; se ejecutó testDebugUnitTest.
- APK principal y androidTest compiladas.
- Lint: **cero errores y 39 advertencias** pendientes.
- Log local: `app/build/reports/recommendations/review-resumen-20261018.txt`.

No se repitieron aquí UI, SDK demo ni suites de reglas para revisar/instalar
esta versión. La evidencia Android de la integración corresponde al compañero.

## Integración: evidencia entregada por el compañero

Implementación `41ef94a`, publicada en main; cierre documental `0192056`.
La auditoría final reportó 258 JVM correctas, ambas APK compiladas y lint con
0 errores/39 advertencias. Corrigió la prioridad del aviso GUARDADO_PENDIENTE
cuando también hay incoherencia: la regresión falló antes y pasó después.

**59 casos UI sintéticos distintos correctos**, en ejecuciones dirigidas:

| Suite | Casos |
|---|---:|
| ResumenVisualTest | 7 |
| ResumenIntegrationTest | 3 |
| AcademicFlowTest | 11 |
| HistorialFlowTest | 20 |
| OfertaFlowTest | 9 |
| RecomendacionesFlowTest | 6 |
| LoginFeedbackTest | 3 |

AVD aislado `RutaSistemas_Integration_API35`, páginas de 4096 bytes y repositorios
en memoria. Cubre las cuatro pestañas, conteos/avisos, cambio de cuenta, filtros
de Malla, borrador/búsqueda/revisión de Oferta, entrada guardada de Para ti,
retorno de Perfil y aviso de descarte.

No fue una única ejecución sin fallos previos: AcademicFlow dio 9/11 y luego
1/2 al repetir los fallidos. Se corrigió únicamente el harness (cerrar teclado
y desplazar filas antes de afirmar visibilidad); el final completo pasó 11/11.
Los 7 visuales y 3 de integración corresponden a la primera APK de pruebas; el
resto a su recompilación. Producción y esas dos suites no cambiaron entre ambas.

Evidencia local del equipo: `app/build/reports/final-audit/`, `final-run/` e
`integration-t1/`. No está incluida en un clon nuevo. Se utilizó el IDE cuando
la terminal de aquella computadora no pudo ejecutar Gradle/AVD; no se cambiaron
ACL ni funciones Windows. No se acreditan con esta suite Google real, Firebase
productivo, teléfono ni reparación del AVD personal; SDK/reglas no se repitieron.

## Teléfono del estudiante

V2529, API 36: APK principal de `0192056` instalada con
`adb install --no-streaming -r`, resultado Success, sin desinstalar ni borrar datos.
SHA-256: `616bc683e96ee5bc0ffc58e3ec1afed19701af675698a01dd411264a2954a5e4`.
MainActivity abrió con Status ok, COLD de 2910 ms y proceso presente al terminar
el comando. No se instaló androidTest ni se ejecutaron guardados personales por
la automatización durante esta actualización.

Después, el estudiante confirmó pruebas manuales y uso satisfactorios de la app.
No enumeró cada escenario: es aceptación manual reportada, no comprobación
exhaustiva de concurrencia, desconexiones o casos extremos.

Actualización de Cuenta el 19/10/2026: V2529 identificado por modelo antes de
instalar, solo APK principal mediante `adb install --no-streaming -r`, **Success**.
SHA-256: `051f90ff8a9c84a2dbbc16ff4b58336289eaea10500555438d460f3599036290`.
MainActivity abrió con Status ok, COLD de 1561 ms. Sin desinstalar, borrar datos,
instalar androidTest ni ejecutar guardados personales automatizados. Evidencia:
`account-auth/continuation-device-install.txt`. La prueba manual de huella está
descrita arriba; correo real sigue pendiente de activación autorizada.

Tras corregir la carrera de SessionLock, segunda actualización `--no-streaming -r`,
**Success**, apertura Status ok y COLD de 2688 ms. APK final SHA-256:
`f71b2f1034486a55a9cafc021281b8901d2ebd1f22c254f17ae375a071a4d1d5`.
Log: `account-auth/continuation-device-lock-fix-install.txt`. Se conservaron datos.
El reporte manual de huella corresponde a la APK inmediatamente anterior; el
ajuste posterior tiene regresión JVM reproducida y no añade pantallas ni prompts.

## Evidencia previa que sigue siendo relevante

| Entrega | Comprobación conservada | Límite |
|---|---|---|
| Para ti inicial | 222 JVM, 13 UI seleccionadas y 1 SDK demo correctas. | Antes de los ajustes visuales y de avances parciales. |
| Ajustes de Para ti | 232 JVM, 15 UI (6 Para ti + 9 Oferta), lint 0/39 y capturas con fuente 1,5 correctos. Contraste mínimo de textos revisados superior a 4,5:1. | SDK no repetido: lectura/adaptador/contratos no cambiaron. |
| SDK de Para ti | 1 caso, FirebaseApp secundaria, Auth/Firestore demo, cuentas sintéticas; lecturas/cálculo conservan snapshots y cambio de UID rechaza la cuenta anterior. | No es Firebase productivo; no escribe durante la consulta bajo prueba. |
| Oferta inicial | 193 JVM, 7 UI y 1 SDK demo correctas; reglas: 35 comprobaciones más regresiones Perfil 75/Malla 126/Historial 94/Revisión 26 e igualdad de 50 UUID. | Guardados y concurrencia con datos demo. |
| Filtro de aprobadas en Oferta | 199 JVM, 9 UI y lectura productiva de solo consulta correctas. | Apertura/cancelación y búsqueda en teléfono no prueban un guardado personal automatizado. |
| Revisión académica optimizada | 176 JVM, reglas y siete casos SDK aislados correctos desde teléfono con Auth/Firestore demo; 50 cambios, recuperación, conflictos y caché. | La ejecución conjunta esperó; los casos pasaron aislados. Tiempos demo no son benchmarks por Internet. |
| Catálogo SAE | 17 Python y 18 Node correctas; migración productiva atómica verificada, lectura posterior y huellas personales conservadas. | Resultados anteriores; no se repitió la migración en estas revisiones. |

Evidencia local, no versionada: `app/build/reports/recommendations/` (incluidos
`adjustments-*`), `offer/`, `offer-approved-filter/`, `optimization-*`,
`sae-history-ui-final/` y `sae-rollout/`. Exportaciones visuales versionadas en
[el índice](DISENOS/INDICE_Y_ESPECIFICACIONES.md) y
[las capturas de Oferta](DISENOS/EVIDENCIA_OFERTA_T2/README.md).

En los AVD sintéticos hubo intentos fallidos por ANR/presión de memoria y avisos
System UI. El capturador de Para ti pulsa Wait solo ante el diálogo exacto de
System UI; no oculta fallos de instrumentación. Las capturas de modales usan
la ventana Android porque la captura de Activity no incluye el diálogo.
Estos resultados no reparan ni explican completamente ART/system_server de
Pixel_7 API35 de 16 KB. No borrar su userdata para repetir comprobaciones.

La rotación de Oferta está cubierta por ViewModelStore en JVM; la UI comprueba
navegación de la sesión, no una rotación física adicional. Esta distinción sigue
vigente pese a las pruebas manuales satisfactorias.

## Publicaciones Firebase ya realizadas

El catálogo SAE y las reglas de revisión académica/Oferta ya fueron publicados
con autorización y relectura posterior. No quedan como pasos de instalación.

- Catálogo SAE: 50 materias, 28 relaciones y umbral Taller 45. Se preservaron UUID
  y huellas personales en auditorías de solo lectura antes/después de la migración.
- Reglas de Oferta: despliegue `verified-published`, código cero y SHA-256 remoto
  `4fc14fb7e06bf3de81b8791bf67db1942202c191868561a8bfbbe61da8ecfc02`.
  Al retirar los cuatro bloques de Oferta, los demás permisos conservaron sus tokens.
- Lecturas productivas posteriores en V2529 comprobaron catálogo/selector, exclusión
  de aprobadas, sesión y snapshots intactos; no invocaron guardar selección personal.

Evidencia local: `offer-production-rollout-20261018/`,
`offer-device-diagnosis-20261018-remote/`, `optimization-rollout/` y `sae-rollout/`,
bajo `app/build/reports/`. Son verificaciones fechadas, no una consulta remota
nueva. No reutilizar publicadores antiguos ni reinstalar clientes incompatibles
con la revisión académica/catálogo SAE. Las reglas requieren revisión antes de
una distribución amplia; publicar código no vuelve a desplegarlas.

## Repetir comprobaciones

Desde la raíz del clon, con JDK/SDK configurados:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -Dfile.encoding=UTF-8'
python -B -m unittest discover -s firebase/tests -p test_academic_catalog.py -v
node --test firebase/tests/academic-catalog-loader.test.cjs firebase/tests/academic-catalog-command.test.cjs firebase/tests/sae-catalog-migration.test.cjs firebase/tests/sae-catalog-migration-command.test.cjs
node --test firebase/tests/offer-catalog.test.cjs
```

Añadir `--offline` solo si la caché está lista. No repetir toda la compilación por
ediciones documentales: comprobar enlaces, consistencia y `git diff --check`.

### Android con datos sintéticos

Compilar androidTest antes de instalar. Ejecutar solo clases elegidas y verificar
nombre del AVD, no solo su serial. Los runners existentes rechazan dispositivos
ajenos; adaptar `AdbPath` al equipo sin eliminar esas guardas.

- `scripts/test-offer.ps1`: AVD `RoadMap_Offer_Test_API35`, serial `emulator-5556`;
  UI de Oferta, o `-SdkDemo` para su SDK.
- `scripts/test-recommendations.ps1`: AVD `RoadMap_Recommendations_Test_API35`,
  mismo serial reservado; seis UI de Para ti más nueve de Oferta, o `-SdkDemo`.
- No iniciar esos dos AVD simultáneamente en el mismo puerto.
- Las otras clases UI de integración figuran en la tabla de 59 casos; ejecutar
  por clase con repositorios sintéticos, nunca la suite Android completa a ciegas.

`PersonalAcademicPersistenceTest` y `scripts/test-personal-history.ps1` escriben
datos reales y requieren autorización específica; no forman parte de la rutina.

### SDK y reglas en demo

Generar configuración local:

```powershell
node firebase/scripts/prepare-history-sdk-emulator.cjs --config-only
firebase emulators:start --only auth,firestore --project demo-roadmap-utepsa --config app/build/reports/history-sdk-emulator/firebase.local.json
```

Con los servicios listos, desde otra terminal:

```powershell
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8089'
$env:FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099'
$env:GCLOUD_PROJECT = 'demo-roadmap-utepsa'
node firebase/scripts/prepare-history-sdk-emulator.cjs
node firebase/tests/run-offer-rules-tests.cjs
```

El runner de reglas incorpora las regresiones; los runners Android con `-SdkDemo`
requieren esos servicios/fixtures y el opt-in que añaden sus scripts. No sustituir
el proyecto demo por producción. Detener los servicios/AVD de la tarea al terminar.

## Límites de la base y continuación

La revisión de la base integrada no detectó divergencias funcionales en los
bloques académicos contrastados. La consolidación documental previa conservó
la evidencia y los contratos. La solicitud anterior de Cuenta autorizó nueva
implementación; su evidencia se separa de la base histórica de arriba.

Siguen abiertos el diagnóstico del AVD personal, la recepción push remota,
la medición productiva necesaria; las 39 advertencias de esa base se resolvieron en la etapa de calidad descrita arriba. No borrar datos
ni ejecutar pruebas personales a partir de informes antiguos.

## Continuación de la pausa

La pausa anterior quedó sin instalación en V2529; ya se actualizó el 19/10 y se
conserva el reporte manual de huella en esta guía. Después se autorizó y verificó
la publicación productiva de Cuenta descrita arriba. Siguen sin commit/push ni PR.
No se borraron datos ni se usó el AVD personal.

La evidencia anterior permanece en `account-navigation-fix-build.txt`,
`account-preference-error-build.txt` y `account-auth/final-targeted-ui.txt`.
Este último log terminó 16/18: además de touch sobre la fila de Recomendaciones,
falló una fila no compuesta de Oferta en Resumen. No fue una validación del
contador corregido. Los informes SDK/Regressions anteriores se conservaron como
`account-auth/SDK-before-continuation.txt` y `Regressions-before-continuation.txt`.

Nueva evidencia local bajo `account-auth/`: compilación final y del harness,
regresión UI, diagnóstico REST con fallo conservado, 13 comprobaciones de reglas,
regresiones de reglas existentes, preparación del paso remoto e instalación del
teléfono. Los resultados vigentes y sus límites están al inicio de esta guía.
Al terminar se cerraron el AVD sintético y Auth/Firestore demo de esta tarea;
se verificaron sus puertos sin listeners. ADB conserva el V2529 conectado. No se
tocó Pixel_7 ni se realizaron commit, push o PR. La publicación productiva de
Cuenta se realizó después, en el paso expresamente autorizado descrito arriba.
