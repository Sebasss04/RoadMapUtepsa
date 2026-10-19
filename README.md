# RoadMap UTEPSA

Aplicación Android con Kotlin, Jetpack Compose y Firebase para registrar el
historial académico y orientar la elección de materias de Ingeniería de Sistemas
(RM 615/2016). La app orienta; no inscribe ni sincroniza con SAE.

## Estado

Entrega del 19/10/2026 integrada en `main`, desde la base `0192056`:
acceso Google y correo/contraseña, verificación, recuperación, vinculación,
biometría, Cuenta, Perfil, Historial, Resumen, Malla, Oferta SAE, Para ti y avisos
locales de pendientes. El estudiante dio su conformidad final y autorizó la
publicación del código y las guías. Las pruebas físicas anteriores fueron
satisfactorias; no se registró nueva matriz ni hash de APK en esa confirmación.

La reorganización mantiene un único módulo `app`, MVVM, datos y diseño. Los
paquetes separan dominio, datos, presentación y plataforma por función; los
textos visibles usan recursos. Validación: 300 JVM, 71 pruebas de regresión
Android, 18 de avisos, permiso denegado y dos fases de persistencia correctas.
Lint debug/release: cero errores y un aviso por variante sobre Gradle disponible.

SDK demo: 6/7 por EMAIL_EXISTS en Google→contraseña; ese recorrido funciona en
el dispositivo físico según las pruebas reportadas por el estudiante. No se
considera una regresión nueva ni se exige repetir esa aceptación. Push remoto
no acreditado y fallo del AVD personal conservados como límites de validación.
Evidencia y alcance en [VALIDACION.md](CONTEXTO/VALIDACION.md).
Las reglas y el proveedor de correo se publicaron previamente con autorización;
esta integración de código no ejecuta despliegues ni instalaciones.

## Mapa del código

Bajo `app/src/main/java/com/example/roadmap_utepsa/`:

| Paquete | Responsabilidad |
|---|---|
| Raíz | `MainActivity` enlaza ciclo de vida/ventana; `RoadMapApplication` inicia el proceso. |
| `di` | `AppContainer` construye repositorios y factories de Google, bloqueo y avisos. |
| `domain/{academic,auth,history,notification,offer,profile,recommendation,session}` | Modelos, reglas puras, contratos y errores propios. Sin Android, Firebase, recursos ni datos/presentación. |
| `data/{academic,auth,history,notification,offer,profile,recommendation,session}` | Adaptadores Firebase, mapeo, transacciones, lectura estable y DataStore. Sin presentación. |
| `presentation/{academic,account,auth,history,navigation,notification,offer,profile,recommendation,session}` | Pantallas, ViewModels y estados junto a cada función. Usa contratos del dominio. |
| `presentation/shared/{components,text,theme}` | Barra común de cuatro pestañas, controles compartidos, textos y tema. |
| `platform/{auth,biometric,notification}` | Credential Manager, prompt biométrico, notificaciones Android, FCM e intents. |

Malla y Resumen comparten `AcademicViewModel`. `AppNavigation` compone las puertas
de acceso, `EntryNavigation` y `AcademicFlowNavigation`; este último conserva los
propietarios y claves de sesión y compone `AcademicNavigation` y `AccountNavigation`.
El bloqueo cubre el contenido sin desmontar escrituras ni borradores.
Las preferencias de bloqueo/avisos tienen contratos en dominio, DataStore en datos,
controladores ViewModel en presentación y entrega Android en plataforma.

Los adaptadores traducen fallos de acceso a `AccessFailureKind`, conservando causas
internas y propagando cancelaciones. Los estados exponen recursos identificables;
presentación resuelve el texto. Las validaciones puras exponen categorías.
No mostrar mensajes crudos de excepciones ni registrar secretos. El filtro común
de Malla/Oferta/Historial vive en `domain/academic/CurriculumFilter.kt`.

Pruebas JVM y Android agrupadas por función. Los runners usan las clases exactas
de `scripts/test-class-names.ps1` y conservan sus guardas demo/AVD.

```powershell
python scripts/check-architecture.py
.\gradlew.bat testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest lintDebug lintRelease --max-workers=1
```

El comprobador genera el inventario de textos y el informe de dependencias en
`app/build/reports/reorganization/`. Resultados y límites reales en
[VALIDACION.md](CONTEXTO/VALIDACION.md); no usar el recuento histórico como ejecución nueva.

## Documentación

- [Inicio y fuentes](CONTEXTO/README.md).
- [Implementado, arquitectura y pendientes](CONTEXTO/ESTADO_ACTUAL.md).
- [Reglas y contratos del producto](CONTEXTO/PRODUCTO_Y_PLAN.md).
- [Coordinación y siguientes tareas](CONTEXTO/TRABAJO_EN_EQUIPO.md).
- [Pruebas, evidencia y límites](CONTEXTO/VALIDACION.md).

## Preparar el entorno

```powershell
git clone https://github.com/Sebasss04/RoadMapUtepsa.git
cd RoadMapUtepsa
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Abrir la raíz en Android Studio y configurar el SDK mediante `local.properties`
(ignorado). Entorno verificado: JDK 25, Gradle 9.6.0, AGP 9.4.1 y Kotlin/Compose compiler 2.4.21,
compile/target SDK 37 y minSdk 35. Java/Kotlin compilan al nivel JVM 11.
Usar `--offline` solo con dependencias descargadas. Comandos Android/demo y
precauciones con pruebas personales en [VALIDACION.md](CONTEXTO/VALIDACION.md).

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

Firebase: proyecto `roadmap-utepsa-c9409`, Firestore `(default)`.
`google-services.json` es configuración cliente versionada, no acceso administrativo.
Cada colaborador necesita sus propios permisos y las huellas SHA-1/SHA-256 de su
clave debug registradas para Google Sign-In. No compartir claves privadas ni tokens.
No repetir la migración del catálogo. El nuevo control de identidad en reglas y
el proveedor de correo ya se publicaron con autorización y relectura el 19/10;
no repetir el paso. El alcance de la aceptación manual está en VALIDACION.
