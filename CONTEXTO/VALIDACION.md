# Validación de RoadMap UTEPSA

## T2 — Oferta actual SAE, 18/10/2026

Rama `feat/oferta-actual-sae`, base `96a400a`. Se conserva la modificación local
previa de coordinación y la asignación de T1 al compañero. Los resultados de desarrollo
figuran aquí. **La entrega T2 sigue local, sin push ni PR creado**: la revisión
automática de aprobación rechazó la publicación combinada al repositorio público por
no considerar explícitamente autorizado ese destino y contenido. Se requiere esa
confirmación para publicar; editar estas guías no las vuelve compartidas.

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

Reglas de Oferta **no publicadas**, sin migraciones ni instalaciones personales.
Producción continúa denegando Oferta: se necesita revisión del equipo y autorización
específica para publicar reglas. No atribuir pruebas demo a persistencia productiva.
Integración con Resumen pendiente si T1 todavía no llegó a `main`: incorporar `main`
y conservar ambas funciones en los puntos de navegación/contenedor. No se calcularon
recomendaciones. La integración final se hará mediante PR revisado, sin merge automático.

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
