# Validación de RoadMap UTEPSA

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
