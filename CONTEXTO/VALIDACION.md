# Validación de la actualización del 17/10/2026

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
