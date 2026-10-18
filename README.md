# RoadMap UTEPSA

Aplicación Android con Kotlin, Jetpack Compose y Firebase para registrar el historial académico y orientar la elección de materias de Ingeniería de Sistemas (RM 615/2016). No inscribe ni sincroniza con SAE.

## Empezar aquí

El contexto necesario para continuar desde otra computadora está versionado:

- [Guía de incorporación, enlaces y primer encargo](CONTEXTO/README.md).
- [Estado actual, arquitectura, avances y problemas conocidos](CONTEXTO/ESTADO_ACTUAL.md).
- [Requisitos y decisiones del producto](CONTEXTO/PRODUCTO_Y_PLAN.md).
- [Trabajo pendiente y reparto propuesto](CONTEXTO/TRABAJO_EN_EQUIPO.md).
- [Validación de esta publicación](CONTEXTO/VALIDACION.md).

Implementados: acceso Google, Perfil, Malla, Historial con coherencia, persistencia, recuperación y conflictos; catálogo SAE con duración y guardado optimizado. T2 añade Oferta actual SAE con consulta, selección manual y reemplazo transaccional en su rama de entrega (ver validación y PR). Pendientes: integrar Resumen del compañero, Recomendador y funciones completas de Cuenta. Las reglas de Oferta aún requieren publicación productiva autorizada. Los mockups no prueban implementación.

## Preparar el entorno

```powershell
git clone https://github.com/Sebasss04/RoadMapUtepsa.git
cd RoadMapUtepsa
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Abrir la raíz en Android Studio. Configuración actual: JDK 25, Gradle 9.5.0, AGP 9.3.3, compile/target SDK 37 y minSdk 35. Configurar el SDK local en Android Studio o en `local.properties` (ignorado). En una máquina nueva se necesita Internet para descargar herramientas y dependencias; `--offline` solo sirve con la caché preparada.

Firebase: `roadmap-utepsa-c9409`. `google-services.json` es configuración del cliente ya versionada; no concede acceso administrativo. Para Google Sign-In con otra clave debug se deben registrar sus huellas SHA-1/SHA-256 mediante un administrador autorizado. Solicitar acceso al repositorio y Firebase al responsable si faltan permisos; no compartir claves privadas, tokens ni cuentas personales. Abrir la app no requiere publicar reglas ni recargar el catálogo.

APK: `app/build/outputs/apk/debug/app-debug.apk`. El AVD Pixel_7 API35 de 16 KB presentó fallos nativos de ART y reinicios del framework; su causa exacta sigue abierta. Preferir un dispositivo de pruebas estable y consultar el diagnóstico en el estado actual.
