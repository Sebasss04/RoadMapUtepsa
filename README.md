# RoadMap UTEPSA

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
en CONTEXTO/VALIDACION.md. Repositorios, CAS, Historial, Oferta, motor y contratos preservados.
Solo se corrigió producción en ResumenUiState; AcademicFlowTest ajusta teclado/scroll
sin retirar aserciones. Sin Firebase productivo ni cuentas reales. Publicar requiere
una autorización nueva expresa. Los apartados de integración siguientes son antecedentes.


Aplicación Android con Kotlin, Jetpack Compose y Firebase para registrar el historial académico y orientar la elección de materias de Ingeniería de Sistemas (RM 615/2016). No inscribe ni sincroniza con SAE.

## Empezar aquí

El contexto necesario para continuar desde otra computadora está versionado:

- [Guía de incorporación, enlaces y primer encargo](CONTEXTO/README.md).
- [Estado actual, arquitectura, avances y problemas conocidos](CONTEXTO/ESTADO_ACTUAL.md).
- [Requisitos y decisiones del producto](CONTEXTO/PRODUCTO_Y_PLAN.md).
- [Trabajo pendiente y reparto propuesto](CONTEXTO/TRABAJO_EN_EQUIPO.md).
- [Validación de esta publicación](CONTEXTO/VALIDACION.md).

Implementados: acceso Google, Perfil, Malla, Historial con coherencia, persistencia, recuperación y conflictos; catálogo SAE con duración y guardado optimizado. T2 añade Oferta actual SAE con consulta, selección manual sin materias aprobadas y reemplazo transaccional en `feat/oferta-actual-sae`, cuya publicación autorizó el estudiante tras aprobar sus pruebas. Ver validación e instrucciones de integración en CONTEXTO. Para ti añade diseño Figma actualizado, motor de pendientes anteriores/desbloqueos/avances parciales, principal/secundaria/opcionales, consulta estable y pantalla en `feat/para-ti`; ver resultados y límites de validación en CONTEXTO/VALIDACION.md. Resumen del compañero está integrado en esta copia local sobre main `30685a6`, sin commit, con accesos a las cuatro pantallas. Cuenta completa sigue pendiente. Las reglas de Oferta ya fueron publicadas con autorización y relectura productiva verificada. Los mockups no prueban implementación.

## Estado local de integración

main actualizado por fast-forward a `30685a6`; PR #1 y #2 ya integrados.
Los cambios de T1/login permanecen sin confirmar. Ver la sección inicial de
CONTEXTO/VALIDACION.md para las comprobaciones y sus límites.
No repetir despliegues ni migraciones al actualizar el código.

## Preparar el entorno

```powershell
git clone https://github.com/Sebasss04/RoadMapUtepsa.git
cd RoadMapUtepsa
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Abrir la raíz en Android Studio. Configuración actual: JDK 25, Gradle 9.5.0, AGP 9.3.3, compile/target SDK 37 y minSdk 35. Configurar el SDK local en Android Studio o en `local.properties` (ignorado). En una máquina nueva se necesita Internet para descargar herramientas y dependencias; `--offline` solo sirve con la caché preparada.

Firebase: `roadmap-utepsa-c9409`. `google-services.json` es configuración del cliente ya versionada; no concede acceso administrativo. Para Google Sign-In con otra clave debug se deben registrar sus huellas SHA-1/SHA-256 mediante un administrador autorizado. Solicitar acceso al repositorio y Firebase al responsable si faltan permisos; no compartir claves privadas, tokens ni cuentas personales. Abrir la app no requiere publicar reglas ni recargar el catálogo.

APK: `app/build/outputs/apk/debug/app-debug.apk`. El AVD Pixel_7 API35 de 16 KB presentó fallos nativos de ART y reinicios del framework; su causa exacta sigue abierta. Preferir un dispositivo de pruebas estable y consultar el diagnóstico en el estado actual.
