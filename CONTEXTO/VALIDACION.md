# Validación de RoadMap UTEPSA

## Publicación de Para ti — 18/10/2026

El estudiante autorizó subir todos los últimos cambios con documentación. Esta
petición sustituye la espera previa de commit/push de T3/T4. Entrega en
`feat/para-ti`, basada en `2f21871`, con PR hacia `main` para revisión del equipo;
no implica merge automático. Fetch previo correcto: `origin/main` continúa en
`2f21871`, sin cambios nuevos de Resumen. T1 sigue con el compañero.

Incluye motor, consulta estable, ViewModel por sesión, pantalla, navegación,
recursos, pruebas JVM/UI/SDK, runner protegido, dos exportaciones de diseño y
guías actualizadas. No incluye APK, cachés, credenciales, logs ni informes bajo
`app/build/`; esos informes son locales y regenerables.

Comprobación de publicación: Gradle `:app:testDebugUnitTest :app:assembleDebug
:app:lintDebug :app:assembleDebugAndroidTest --offline --max-workers=1`, JDK de
Android Studio y heap de 768 MiB: **BUILD SUCCESSFUL**, 30 s, ocho tareas
ejecutadas y 74 al día. La caché de configuración se reutilizó. Los informes JVM
contienen **232 pruebas, cero fallos y errores**; no se forzó su reejecución.
Lint: **cero errores y 39 advertencias**. La APK generada al publicar tiene
SHA-256 `05ba5f841d5be597819b065228e9155a1a2c358f9a28c9cdfcb049785b102ead`;
el hash del ajuste anterior se conserva debajo como evidencia de aquella ejecución.
Se conserva la validación previa de **15 UI** y **1 SDK demo** con las fechas y
límites detallados debajo; no se volvió a iniciar AVD ni Firebase demo para subir
el código. Sin instalaciones, despliegues, migraciones ni escrituras personales.
La primera ejecución restringida no accedió al bloqueo de la caché Gradle; la
ejecución con acceso a esa caché completó correctamente.

Evidencia local de esta comprobación:
`app/build/reports/recommendations/publication-build-20261018.txt`.

## Ajustes de Para ti — 18/10/2026

Petición posterior a la entrega inicial T3/T4: avances parciales por requisitos
directos, secundaria con beneficio y opcionales sin favorita artificial, conservando
prioridad de pendientes anteriores. Texto reducido, pendientes desplegables y
tarjetas verde/azul/gris. Cambios internos, sin persistencia nueva ni cambios de
Firestore/catálogo/reglas. No se ejecutan pruebas personales ni se instala en el teléfono.

Diseño: sección existente de Figma `11:292`, principal `11:295` y explicación
`11:362` actualizadas antes de Compose. Detalle ilustrativo de Calidad con 1/2
requisitos de Proyecto Integrador; nueva exportación
[08_PARA_TI_AJUSTES_20261018.png](DISENOS/08_PARA_TI_AJUSTES_20261018.png).
Verificación estructural: 773 descendientes, 393 frames, 267 textos, 87 vectores,
17 rectángulos y 9 instancias; sin imágenes que sustituyan la UI, familia Roboto
comprobada. Revisión visual de composición y corrección de acentos residuales.
Las exportaciones muestran el encuadre inicial; los contenidos largos son desplazables.

La primera comprobación ejecutó 229 JVM correctamente y compiló la APK, pero lint
falló al generar el informe (`Range ... out of bounds`) después de editar los
recursos mientras la ejecución estaba activa. No cuenta como compilación completa
aprobada. La repetición con código fijo terminó correctamente. Durante la revisión
visual se reforzó el resumen por cantidades cuando hay beneficios mixtos y se fijó
el nombre de las opcionales en texto oscuro; la verificación definitiva incluye
estos cambios y las pruebas adicionales.
Con Gradle y AVD concurrentes hubo poca memoria libre; se detuvo únicamente el
AVD sintético de esta tarea para terminar la compilación antes de instrumentar.

- Gradle final: `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug
  :app:assembleDebugAndroidTest --offline --max-workers=1`, heap 768 MiB,
  **BUILD SUCCESSFUL**, 1 min 13 s. APK principal y APK de pruebas compiladas.
- **232 JVM, cero fallos/errores**; 20 pruebas del motor (10 nuevas frente a la
  entrega inicial). Incluyen Calidad/Proyecto Integrador con Análisis pendiente,
  aprobado y en curso, transición parcial→completo, prioridad anterior, orden por
  cantidad de avances, secundaria que salta opciones sin beneficio, cero/una/ninguna
  candidata, umbrales, cadenas, no duplicación y conservación de entradas. El caso
  de cinco ofertadas comprueba Inglés principal, Calidad secundaria y tres opcionales.
  Los tests existentes de lectura estable y sesión siguen correctos.
- Lint final: **cero errores, 39 advertencias existentes**. Se corrigieron las dos
  advertencias nuevas de Modifier y pluralización; no quedan avisos en los archivos
  de recomendaciones revisados.
- Contraste calculado: principal 6,14:1, secundaria 7,67:1, opcional 14,78:1;
  metadatos sobre verde/azul 4,99:1 y 4,95:1. Todos superan 4,5:1.
- **UI final: 15/15 correctas**, 94,594 s: seis de Para ti y nueve de Oferta,
  con repositorios sintéticos en memoria. Verifica beneficios mixtos resumidos
  por cantidades, detalle con ambos tipos de objetivos, ejemplo Inglés/Calidad,
  1/2 requisitos y Análisis de Datos faltante, opcionales sin secundaria artificial,
  una sola candidata, pendientes desplegables y avisos, estados bloqueantes,
  fuente 1,5, nombres largos y navegación conservando búsqueda/borrador/revisión
  de Oferta. Capturas finales revisadas visualmente; 17 PNG bajo
  `recommendations-ui-captures/` en el directorio de evidencia.
  Se usó únicamente `RoadMap_Recommendations_Test_API35`, `emulator-5556`,
  resolución 824×1784/densidad 320, sin modificar el teléfono ni Pixel_7.
  La ejecución anterior 14/14 pasó antes del ajuste exclusivo de resumen mixto
  y color del nombre; la final 15/15 corresponde a la APK cuyo hash figura abajo.
- SDK demo no repetido: no cambió la lectura, adaptador, persistencia ni contrato
  remoto. La evidencia SDK de la entrega inicial sigue documentada con su fecha.

SHA-256 de APK final:
`925c7a6dc851361e11730632e32174065d20eeddee49f1607cd135edb7ce9299`.

Evidencia local regenerable: `app/build/reports/recommendations/adjustments-*`.
Runner: `scripts/test-recommendations.ps1 -AdbPath <ruta-al-adb-local>` con el AVD
sintético indicado iniciado. No ejecutar toda la suite Android ni tests personales.
AVD de esta revisión detenido al terminar; no se iniciaron servicios demo Firebase.
La entrega inicial y sus resultados históricos se conservan debajo; no sustituyen
las verificaciones de este ajuste. Al cerrar esta revisión todo quedó local;
la autorización posterior de publicación se registra en la sección inicial.

## Para ti — T3/T4, 18/10/2026

Responsable: autor/Codex por asignación expresa del estudiante; el compañero
conserva Resumen. Rama local `feat/para-ti`, base actualizada a `2f21871` (merge
del PR #1 de Oferta confirmado mediante fetch). No había cambios T1 en main.
Los cambios locales previos de plan/coordinación se sustituyeron por el alcance
vigente solicitado. Al cerrar la entrega inicial no se crearon commits, push ni
PR; la publicación posterior se registra en la sección inicial.

Cierre documental solicitado por el estudiante tras la entrega: continuará las
correcciones en otro chat. Este cierre solo actualiza las guías; no modifica
código, no ejecuta nuevamente las suites y no amplía los resultados siguientes.

### Diseño terminado

Figma se actualizó antes de crear la pantalla Compose. Se revisaron visualmente
los 13 estados editables: principal/otras opciones, explicación, oferta ausente,
oferta vacía, ninguna candidata, error, carga, pendientes anteriores, alternativa
disponible, historial pendiente/incoherente, recuperación y detalle cero.
[Diseño principal](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=11-295)
y [captura versionada](DISENOS/08_PARA_TI_ESTADOS_20261018.png).
Se conservan Roboto, paleta, navegación e iconos. Flecha (18×18), advertencia, Oferta y Malla de estado (48×48) se exportaron
como SVG y se trasladaron sin cambiar paths/strokes a vectores Android; las barras
conservan sus cinco iconos existentes. Ejemplos identificados como ilustrativos, sin datos personales.

### Implementación terminada

Motor Kotlin puro, lectura conjunta estable y ViewModel por UID/versión conectados
a Compose. La consulta no expone escrituras. Prioriza pendientes de semestres
inferiores al mayor con aprobadas/en curso, luego desbloqueos inmediatos; conserva
ceros, excluye activas/cadenas/ya habilitadas e incluye umbrales y no ofertadas.
La navegación reutiliza Historial/Malla/Oferta/Perfil; mantiene borrador, consulta
y revisión esperada de Oferta. Resumen conserva su independencia y acceso actual.

### Verificaciones realizadas

- Gradle final: `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug
  :app:assembleDebugAndroidTest`, **BUILD SUCCESSFUL**, 2 min 28 s, un trabajador
  y heap 768 MiB para compartir memoria con el AVD. APK principal y androidTest.
- **222 JVM, cero fallos/errores**: 23 nuevas (10 motor, 7 lectura, 6 ViewModel).
  Prioridad, mayor desbloqueo dentro/fuera del grupo anterior, alternativa,
  desempates, ceros, doble requisito, cadena, umbral 44→45, entradas intactas,
  historial incoherente/planes/IDs; ausencia/vacío/error, cambios en ambas lecturas,
  límite de tres intentos, identidad, carga sustituida, respuesta no cancelable,
  cambio de cuenta y de versión de sesión. Dobles fallan si se intenta escribir.
- Lint final: **cero errores, 39 advertencias**; sin advertencias en los archivos
  nuevos de recomendaciones después de resolver los dos avisos de plurales.
- **SDK demo: 1/1 correcta**, 23,778 s. FirebaseApp secundaria, proyecto
  `demo-roadmap-utepsa`, Auth 9099 y Firestore 8089, cuentas sintéticas, catálogo
  SAE de 50 materias. Confirmación y selección inicial como fixture; consulta y
  cálculo dejan snapshots de estados/preparación/diario/revisión/oferta iguales.
  Tras cambios guardados fuera de la consulta, relee revisiones, excluye aprobada y
  vuelve a comprobar snapshots intactos. Cambio de cuenta rechaza el UID anterior.
  Ejecutada antes de los últimos ajustes visuales de tarjeta, plurales e iconos;
  motor, repositorio y adaptador Firebase no cambiaron después de esa ejecución.
- **UI final: 13/13 correctas**, 53,193 s: 4 nuevas de Recomendaciones y 9 de
  regresión de Oferta. Estados y acciones de carga/error/historial/recuperación/
  ausente/vacía/sin candidatas, prioridad y alternativa, explicación y cero,
  nombres largos y fuente 1,5. Navegación Oferta → Para ti → Oferta conserva
  búsqueda/borrador/revisión; solo calcula la selección guardada y vuelve a cargar
  al reentrar y regresar de Perfil. Regresiones de Historial, Perfil, advertencia
  de cierre, errores, conflictos, filtros y guardado de Oferta correctas.
  Una ejecución anterior dio 11/13: se corrigieron el test que pulsaba navegación
  antes de terminar Guardar y el capturador que suponía una sola raíz al abrir
  el modal. Se esperó el estado guardado y se detectó el panel por contenido y se capturó su ventana Android separada; no se relajaron las aserciones funcionales.
  Capturas de los estados y resultados revisadas frente a Figma; SVG y métricas
  de tarjeta corregidos desde el diseño para mantener nitidez y jerarquía.
  Tras el ajuste exclusivo del capturador, androidTest recompilado y el caso de
  detalle/fuente 1,5 repetido: **1/1 correcta**, 12,781 s, con las capturas del
  modal y cero desbloqueos revisadas limpias. No cambió producción ni se repitieron
  JVM/lint tras esa modificación exclusiva de pruebas.
- `git diff --check` correcto. No se modificaron reglas, Auth productivo ni catálogo.

### Entorno, evidencia y límites

Se creó `RoadMap_Recommendations_Test_API35` separado de Pixel_7, imagen API35 de
4 KB ya instalada, puerto 5556, sin ventana y sin snapshots. Pixel_7 y datos
personales conservados. El host mostró presión de memoria y el primer arranque
conjunto de instrumentación terminó con ANR de inicio; una captura posterior
mostró también diálogo de System UI, aunque el caso aislado prioridad/alternativa
pasó (1/1). No se presenta ese primer intento como éxito ni el AVD como reparado.
Se precompiló la APK, liberaron procesos de compilación de esta tarea y redujo la
resolución del AVD sintético a 824×1784/densidad 320. Se intentó suprimir los diálogos nuevos de error solo en ese AVD, pero el aviso
ya abierto persistió; reiniciar System UI no acreditó una reparación. El
capturador pulsa «Wait» únicamente si la ventana activa contiene exactamente
«System UI isn't responding», conservando el diagnóstico y permitiendo una
captura limpia. No cierra la app ni cambia sus datos. Los errores de las pruebas
siguen haciendo fallar la instrumentación. Las capturas de página usan el árbol Compose; las del detalle usan la ventana
Android completa porque PixelCopy de la Activity no incluye el Dialog del panel.
Las aserciones confirman el contenido y el desplazamiento del panel antes de capturarlo.

Evidencia regenerable local: `app/build/reports/recommendations/` contiene
`build-final.txt`, `sdk.txt`, `ui.txt`, diagnóstico de arranque, capturas Compose
y copia del diseño; `verification.json` resume conteos, geometrías de los cuatro
SVG y SHA-256 de la APK (`8d84ae45544d181f9ebade22618954e668f3a477e9e876d65fcac2d45697d37d`).
El código de producción coincide con la ejecución UI de 13 casos; después solo
se modificó el capturador androidTest y la documentación. El script `scripts/test-recommendations.ps1` rechaza dispositivos
desconocidos y ejecuta únicamente las clases sintéticas seleccionadas; `-SdkDemo`
exige los servicios y fixtures demo descritos abajo. No ejecutar la suite Android
completa ni los tests personales para repetir esta entrega. Auth/Firestore demo y
el AVD aislado se detuvieron al terminar; no se borró ningún AVD.

Para repetir SDK: generar config con
`node firebase/scripts/prepare-history-sdk-emulator.cjs --config-only`, iniciar
`firebase emulators:start --only auth,firestore --project demo-roadmap-utepsa
--config app/build/reports/history-sdk-emulator/firebase.local.json`, preparar
fixtures con el mismo script (variables `FIRESTORE_EMULATOR_HOST=127.0.0.1:8089`,
`FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099`, `GCLOUD_PROJECT=demo-roadmap-utepsa`)
y ejecutar `scripts/test-recommendations.ps1 -SdkDemo`.


## T2 — Oferta actual SAE, 18/10/2026

Rama `feat/oferta-actual-sae`, base `96a400a`. Se conserva la modificación local
previa de coordinación y la asignación de T1 al compañero. Los resultados de desarrollo
figuran aquí. El estudiante primero pidió mantener la entrega local; después de
aprobar sus pruebas en el teléfono el 18/10/2026, autorizó subir toda la entrega
para que el compañero actualice su rama. Esa instrucción sustituye la espera de
push/PR. Se conserva `feat/oferta-actual-sae` y el commit inicial `269aa92`, añadiendo
las correcciones verificadas y estas guías. Un rechazo automático a una publicación
anterior pertenece a aquella sesión; no acredita una publicación de esta entrega.

Comprobado hasta ahora:

- JVM: 193 pruebas, cero fallos/errores (17 nuevas de Oferta): ausencia/vacío,
  normalización, selección/reemplazo, cancelación, errores, duplicados, conservación en
  ViewModelStore, conflictos con ambas decisiones, resultado incierto y cambios de sesión.
- Compilación APK y APK de pruebas correctas. Lint final sobre la navegación y código
  de entrega: **cero errores, 32 advertencias**; BUILD SUCCESSFUL. No es una ejecución
  sin advertencias.
- `offer-catalog.test.cjs`: igualdad exacta de los 50 UUID permitidos con catálogo técnico.
- `offer-rules.test.cjs`: 35 comprobaciones correctas en Firestore demo local, incluida
  la selección completa de 50, ausencia/vacío, revisión, tipos, campos, timestamp,
  terceros, usuarios sin autenticar, proveedor anónimo, perfil incompleto, listados y borrados.
- Regresiones existentes de Perfil/Malla (`run-rules-tests.cjs`), preparación
  (`history-preparation-rules.test.cjs`, 94 comprobaciones) y revisión global
  (`academic-revision-rules.test.cjs`) correctas contra el mismo servicio demo.
  El runner completo confirmó códigos cero en sus seis etapas: Perfil 75 comprobaciones,
  Malla 126, preparación 94, revisión 26, además de Oferta 35 e igualdad del catálogo.
  Hash SHA-256 de las reglas locales validadas:
  `4fc14fb7e06bf3de81b8791bf67db1942202c191868561a8bfbbe61da8ecfc02`.
  Este hash no acredita un despliegue productivo.
- `OfertaSdkEmulatorTest`: 1 prueba correcta, 38,501 s, SDK Android real con aplicación
  Firebase secundaria y usuario sintético email/contraseña, Auth 9099 + Firestore 8089,
  proyecto `demo-roadmap-utepsa`. Crear, reemplazar, vacío, recrear repositorio, 50 materias
  y dos editores concurrentes (un éxito, un conflicto) sin modificar revisión académica.
- AVD nuevo `RoadMap_Offer_Test_API35`, serial `emulator-5556`, imagen estándar API35
  con páginas de 4096 bytes; userdata bajo `app/build/test-services/offer-avds`.
  Dos primeros arranques de instrumentación murieron por ANR de inicio antes de casos.
  Tras liberar Gradle, SDK pasó y UI ejecutó sus casos. Esto no prueba una reparación
  del AVD personal Pixel_7 ni identifica por sí solo la causa de los ANR.
- `OfertaFlowTest`: **7/7 correctas**, 128,522 s. Consulta ausente/vacía, editor,
  guardando, error/éxito, conflicto, cancelación, carga, error de lectura, fuente 1,5
  y búsqueda sin resultados. Navegación Oferta → Perfil → Malla → Oferta → Malla →
  Historial → Malla → Oferta conserva el borrador. Se verifican avisos de descarte
  al cerrar sesión desde Perfil/Malla. Perfil conserva su retorno a Malla.
  Se corrigieron dos localizadores para filas fuera del viewport; la suite final
  completa pasó después de esas correcciones.
- [Diez capturas sintéticas versionadas](DISENOS/EVIDENCIA_OFERTA_T2/README.md),
  revisadas visualmente sin recortes de nombres ni controles en la fuente ampliada.
- Cinco SVG de Figma 15:431 comparados con los originales locales: geometrías y
  dimensiones idénticas (cuenta 24×24; navegación 21×21). PNG existentes no vacíos,
  callsites de Oferta a 24dp/21dp; Checkbox/Buttons/TextField de Material 3.

Evidencia local regenerable: `app/build/reports/offer/` (reglas, SDK, UI, capturas y
comparación de assets). El catálogo JSON solo se incorpora a la APK androidTest como
fixture, nunca a la app de producción.

### Repetir T2 sin datos personales

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
node --test firebase/tests/offer-catalog.test.cjs
# Solo con Firestore demo local configurado y reglas actuales cargadas:
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8089'
$env:FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099'
$env:GCLOUD_PROJECT = 'demo-roadmap-utepsa'
node firebase/scripts/prepare-history-sdk-emulator.cjs
node firebase/tests/run-offer-rules-tests.cjs
# AVD sintético de Oferta ya iniciado; el runner valida nombre y serial antes de instalar:
.\scripts\test-offer.ps1
.\scripts\test-offer.ps1 -SkipInstall -SdkDemo
```

`run-offer-rules-tests.cjs` ejecuta regresiones, prepara las reglas finales y valida
Oferta, guardando cada salida y abortando ante cualquier subproceso fallido. La suite
académica antigua se actualizó únicamente para permitir comprobar la ausencia de
Oferta actual del propietario (antes toda Oferta estaba denegada); documentos anidados,
campos inválidos y rutas desconocidas siguen denegados.

El preparador existente `--config-only` genera configuración local para iniciar
Firebase CLI con `--project demo-roadmap-utepsa --only auth,firestore`; no usar deploy.
Los runners no ejecutan suites personales. Ajustar `AdbPath` a cada computadora;
la guarda exige el AVD aislado nombrado, nunca Pixel_7.

### Cierre de desarrollo

T2 cumple el criterio de desarrollo: flujo completo con Firebase demo, comprobaciones
correctas y navegación integrada con Malla/Historial. Resumen no había llegado a
`origin/main` en las dos consultas de esta tarea (`96a400a`); no se reescribió T1.
Se detuvieron el AVD aislado y los servicios demo después de validar. Persisten
únicamente sus artefactos de prueba bajo `app/build`, ignorados por Git.

La conservación por rotación se valida en JVM mediante ViewModelStore y claves de
sesión; la instrumentación cubre navegación de la misma sesión, no una rotación
física adicional del AVD. Los ANR de inicio descritos no se presentan como reparados.

### Pendiente de producción e integración

Diagnóstico del error de lectura reportado en dispositivo físico: inspección nueva
de metadatos y reglas productivas con `compare-history-rules.cjs`, sin leer ni
escribir documentos. Evidencia local:
`app/build/reports/offer-device-diagnosis-20261018-remote/`.
Resultado `compared`, release estable durante la consulta. Reglas remotas SHA-256
`7b70043cfb047d57009062fa1f3ebdd8b87f6cc444e90464855cdb37327479dd`,
sin permiso de Oferta; candidato local
`4fc14fb7e06bf3de81b8791bf67db1942202c191868561a8bfbbe61da8ecfc02`.
El diff contiene las funciones y el bloque nuevos de Oferta más comentarios;
no se publicó nada. El comparador histórico marca `existingBlocksPreserved=false`
porque su extracción del prefijo de estudiantes incluye el nuevo bloque de Oferta;
no interpretar ese booleano como validación completa para desplegar ni como cambio
en los permisos de perfil. Los demás bloques enumerados tienen tokens iguales.
La primera consulta restringida falló al leer metadatos; la repetición de solo
lectura con acceso ampliado completó la comparación. ADB no pudo iniciar su
servidor en el intento restringido: no se obtuvo Logcat ni se ejecutaron pruebas
en el teléfono en ese primer diagnóstico. La evidencia confirmó el bloqueo de reglas.
La prueba posterior a la publicación se detalla debajo; no cambió código Android
de producción.

El estudiante autorizó explícitamente publicar las reglas tras este diagnóstico.
`publish-offer-rules.cjs --apply` terminó `verified-published`, deploy exit 0.
Evidencia: `app/build/reports/offer-production-rollout-20261018/`.
Validación de compilación remota previa, comprobación de los 50 UUID y comparación
de reglas inmediatamente antes y después del despliegue. Hash remoto final idéntico
al candidato local `4fc14fb7e06bf3de81b8791bf67db1942202c191868561a8bfbbe61da8ecfc02`.
La comparación por tokens retirando únicamente `offerOwner`, `offerSubjectIds`,
`validOffer` y `match /ofertas/actual` confirma que el resto de las reglas se conserva.
El publicador no leyó ni escribió documentos, no migró catálogo ni cambió Auth.
Las comprobaciones demo anteriores siguen siendo demo; no prueban un guardado
personal productivo. La publicación autorizada no implica push ni PR.

Validación posterior en el teléfono físico V2529: nueva prueba opt-in
`ProductionOfferReadOnlyTest`, compilada con `:app:assembleDebugAndroidTest`
(BUILD SUCCESSFUL). Se instaló únicamente la APK de pruebas con `-r`.
Resultado **1/1 correcto**, 58,98 s: repositorio productivo lee 50 materias desde
servidor, oferta aún ausente, pantalla muestra primera oferta sin error, selector
se abre y se cancela sin cambios. Snapshots anteriores/posteriores de estados,
preparación, diario, revisión académica y oferta iguales; misma sesión Firebase.
La pantalla estaba bloqueada durante el inicio; el estudiante la desbloqueó y
la misma ejecución terminó correctamente, sin repetir la prueba.
Evidencia saneada: `device-read-only.txt` y `device-evidence/` dentro del directorio
de rollout. App reabierta en MainActivity. No se invocó guardar oferta ni se
cambiaron materias personales para fabricar una prueba de escritura. El guardado
productivo de la selección que el estudiante decida registrar no está acreditado
por esta comprobación; creación/reemplazo/conflictos tienen la evidencia demo previa.

Integración con Resumen pendiente si T1 todavía no llegó a `main`: incorporar `main`
y conservar ambas funciones en los puntos de navegación/contenedor. No se calcularon
recomendaciones. La integración final se hará mediante PR revisado, sin merge automático.

## Ajuste del selector de Oferta — 18/10/2026

Petición posterior del estudiante: mostrar como opciones solo materias aún no
aprobadas. `FirestoreOfertaRepository.load` entrega los estados reales del historial
en `DatosOferta`; selector y búsqueda excluyen APROBADA y conservan EN_CURSO y
pendientes, incluidas bloqueadas. Al regresar desde Malla se refrescan estados
sin perder borrador/búsqueda ni sustituir la revisión esperada de Oferta.
Las aprobadas de una oferta guardada se retiran del borrador con aviso; solo Guardar
persiste la sustitución, Cancelar conserva el documento previo. Antes de enviar se
relee el historial: nuevas aprobaciones en la selección detienen el envío y exigen
revisar/guardar de nuevo. Se conserva la reconciliación de resultados inciertos.
No se cambian reglas, contrato remoto ni escritores de Malla/Historial.

Verificación local: 199 pruebas JVM, cero fallos/errores (seis nuevas de filtrado,
selección anterior, refresco, error, revisión previa al guardado y conflictos).
APK principal y de pruebas compiladas; lint de producción: cero errores y 32
advertencias. La APK de pruebas se recompiló después de ampliar las verificaciones
de solo lectura para búsqueda y listas largas; no se repitió lint tras esos cambios
exclusivos de androidTest.

Instalación con `-r` en el teléfono físico V2529 conservando datos. `OfertaFlowTest`:
**9/9 correctas**, 36,494 s, con repositorios sintéticos en memoria, sin escrituras
personales. Incluye dos nuevas pruebas: búsqueda de aprobada sin resultados y
selección de EN_CURSO; aviso al retirar una aprobada guardada, cancelación sin
escrituras, selector sin opciones al aprobar todo y guardado vacío explícito con
fuente 1,5. Regresiones de navegación, borrador, conflictos, carga/error y búsqueda
correctas. Evidencia local: `app/build/reports/offer-approved-filter/ui.txt`.

`ProductionOfferReadOnlyTest`: **1/1 correcta**, 14,167 s, con la APK nueva y
sesión real existente. Verificó lectura desde servidor, búsqueda de una aprobada
sin resultados, búsqueda de una no aprobada disponible, apertura/cancelación del
editor y snapshots académicos/oferta idénticos antes/después, misma sesión.
Evidencia saneada: `production-read-only.txt` y el JSON más reciente de
`device-evidence/` en el mismo directorio de la revisión. No se guardó ninguna
selección personal para probar; los guardados del nuevo selector son sintéticos
en las pruebas UI. App final reabierta en MainActivity. Sin nuevos despliegues,
commits, push ni PR durante la revisión del filtro. Posteriormente el estudiante
aprobó sus pruebas manuales y autorizó la publicación de toda la entrega.

### Publicación y continuidad del compañero

La publicación autorizada incluye Oferta completa, exclusión de aprobadas,
prueba opt-in de solo lectura y publicador protegido de reglas, junto con el
contrato y las instrucciones del equipo. No incluye APK, cachés, credenciales,
logs personales ni informes locales bajo `app/build/`. Los resultados anteriores
corresponden al código entregado; los cambios finales de publicación son de
documentación. `git diff --check` correcto.

Consulta remota previa a publicar: `origin/main` sigue en `96a400a`, sin commits
nuevos que integrar; la rama remota de Oferta no existía. El compañero puede
incorporar `origin/feat/oferta-actual-sae` antes de subir T1 según TRABAJO_EN_EQUIPO.md.
La publicación de código no vuelve a desplegar Firebase ni modifica cuentas.

Push confirmado de `feat/oferta-actual-sae` con seguimiento de `origin` y commits
`269aa92` + `18056b0`. [PR #1](https://github.com/Sebasss04/RoadMapUtepsa/pull/1)
abierto hacia `main`, pendiente de revisión del compañero; no se ejecutó merge.
El último ajuste de esta publicación registra el enlace y las instrucciones,
sin cambiar el código ya verificado.

## Registro anterior — actualización del 17/10/2026

Esta publicación reúne cambios locales desde el commit `1d5f9dd` y añade contexto portable para el equipo. No implementa las tareas nuevas de Resumen/Oferta/Recomendador propuestas en el tablero.

## Comprobaciones de esta entrega

- Gradle `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`: BUILD SUCCESSFUL. La primera ejecución reutilizó tareas al día; la ejecución JVM adicional con `--rerun-tasks` también terminó BUILD SUCCESSFUL (25 tareas ejecutadas).
- Pruebas JVM: 176, cero fallos/errores en los reportes del proyecto. Resultado comprobado tras la ejecución forzada. Se observó una advertencia Kotlin existente en AcademicStateConcurrencyTest sobre una propiedad delegada; no impidió las pruebas.
- Lint: cero errores y 39 advertencias existentes; no se presenta como una ejecución sin advertencias.
- Python: 17 pruebas del catálogo correctas.
- Node: 18 pruebas del catálogo, publicador y migración correctas, con transporte administrativo simulado; no accedieron a Firebase.
- Contexto: documentos, fuentes curriculares, diagramas y exportaciones visuales incorporados; README y AGENTS utilizan rutas dentro del clon.

## Repetir comprobaciones locales

Configurar Java/SDK según README. Desde la raíz:

```powershell
# Ajustar JAVA_HOME al JDK 25 de la propia máquina si el wrapper no encuentra Java.
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
python -B -m unittest discover -s firebase/tests -p test_academic_catalog.py -v
node --test firebase/tests/academic-catalog-loader.test.cjs firebase/tests/academic-catalog-command.test.cjs firebase/tests/sae-catalog-migration.test.cjs firebase/tests/sae-catalog-migration-command.test.cjs
```

Usar `--offline` solo cuando las dependencias estén en caché. Las pruebas Node crean sus fixtures temporales bajo `app/build/`, que está ignorado. Python solo necesita su biblioteca estándar.

## Límites

No se repitieron en esta publicación las pruebas UI/SDK Android ni las suites de reglas con servicios demo. Sus resultados previos, despliegues y auditorías de solo lectura se describen con sus límites en ESTADO_ACTUAL. No se volvió a consultar Firebase productivo ni se guardaron datos personales, publicaron reglas o migraron catálogos. Un push de código no ejecuta esas operaciones.

El problema de ART/`system_server` del emulador sigue sin causa exacta aislada. Compilación y pruebas locales correctas no acreditan que ese AVD esté reparado, recepción push remota ni estabilidad completa de producción.
