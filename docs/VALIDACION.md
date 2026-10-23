# Validación del MVP

## Comprobaciones locales

Desde la raíz del repositorio:

```powershell
python scripts/check-architecture.py
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --max-workers=1
python -B firebase/scripts/validate-academic-catalog.py
python -B -m unittest discover -s firebase/tests -p test_academic_catalog.py -v
```

El comprobador de arquitectura verifica paquetes, dirección de dependencias y recursos. Las pruebas JVM cubren reglas académicas, recomendaciones, ViewModels, concurrencia y aislamiento de sesión. `assembleDebugAndroidTest` compila las pruebas instrumentadas; no las ejecuta.

Los resultados de Gradle se generan en `app/build/test-results/` y `app/build/reports/`; son archivos locales y no se incluyen en Git. El catálogo JSON es una referencia y un fixture de pruebas; producción lee Firestore.

## Última validación funcional registrada

El 23/10/2026 pasaron 300 pruebas JVM, sin fallos, errores ni omisiones, y se compilaron las APK debug e instrumentada. Tras ampliar el amarillo EN_CURSO a Oferta se repitieron `assembleDebug` y `lintDebug`, sin incidencias lint; no se repitieron las JVM por ese ajuste visual. La arquitectura comprobó 94 fuentes y 548 recursos sin fallos.

El estudiante dio conformidad a los ajustes finales. Este reporte no identifica un dispositivo ni una matriz física nueva. Los logs de esa ejecución se conservan localmente bajo `app/build/quality-*.txt`.

La limpieza de documentación del 23/10 no modifica el código, las pruebas ni la configuración Android. Se comprobó arquitectura y catálogo SAE, se verificaron los enlaces locales de los Markdown modificados contra el contenido versionado y se revisó el diff sin errores de formato. No se volvió a compilar ni a ejecutar la suite funcional por esta limpieza.

## Pruebas Android e integración

Los ejecutores PowerShell de `scripts/` seleccionan clases y comprueban dispositivos de prueba. Cuenta y recomendaciones usan el AVD aislado `RoadMap_Recommendations_Test_API35`; otros ejecutores conservan sus propios parámetros y guardas. Revisar el encabezado del ejecutor antes de usarlo.

No ejecutar toda la instrumentación contra una cuenta personal: `PersonalAcademicPersistenceTest` y `scripts/test-personal-history.ps1` escriben estados reales. Para integración rutinaria se usan identidades sintéticas y Firebase Auth/Firestore Emulator con proyecto `demo-roadmap-utepsa`.

Preparación de los fixtures SDK, con Node.js y Firebase CLI instalados:

```powershell
node firebase/scripts/prepare-history-sdk-emulator.cjs --config-only
firebase emulators:start --only auth,firestore --project demo-roadmap-utepsa --config app/build/reports/history-sdk-emulator/firebase.local.json
```

En otra terminal, después de iniciar los emuladores:

```powershell
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8089'
$env:FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099'
node firebase/scripts/prepare-history-sdk-emulator.cjs
```

Estos pasos preparan emuladores y datos sintéticos; no despliegan reglas en producción. Los publicadores y migradores de `firebase/scripts/` son herramientas administrativas específicas, no pasos para abrir o compilar un clon.

## Límites conocidos

- El último SDK demo registrado obtuvo 6/7 por `EMAIL_EXISTS` al vincular Google con contraseña. El estudiante reportó éxito de ese recorrido en dispositivo físico; no se cambió la prueba para ocultar el fallo demo.
- La recepción de push remoto no quedó acreditada; se conservó la implementación FCM y se probaron los avisos locales en etapas anteriores.
- El fallo del AVD personal no se declaró resuelto. Las pruebas con otro AVD o teléfono no demuestran que se haya reparado.
- Las capturas de `CONTEXTO/DISENOS/` documentan etapas anteriores, no la aceptación visual de la APK final.
