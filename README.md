# RoadMap UTEPSA

Aplicación Android para que estudiantes de Ingeniería de Sistemas de UTEPSA registren su avance académico, introduzcan su oferta SAE y consulten recomendaciones de materias. MVP cerrado en octubre de 2026 para el plan RM 615/2016: 50 materias y 9 semestres curriculares. La aplicación orienta; la inscripción se realiza fuera de ella.

## Funciones del MVP

- Acceso con Google o correo y contraseña; verificación y recuperación de cuenta.
- Perfil, vinculación de métodos de acceso y bloqueo biométrico opcional.
- Historial de materias pendientes, cursando y aprobadas.
- Malla con búsqueda, filtros, requisitos y edición del estado académico.
- Resumen del avance y porcentaje calculado solo sobre materias aprobadas.
- Oferta SAE ingresada manualmente y recomendaciones explicadas en «Para ti».
- Avisos locales opcionales para completar historial u oferta.

Las materias cursando se distinguen con amarillo pastel en Malla, Historial, Resumen y Oferta. El cierre habitual de sesión está en Cuenta, accesible desde el icono de perfil.

## Compilar y ejecutar

1. Clonar el repositorio y abrir su raíz en Android Studio.
2. Configurar un JDK 25 y el SDK Android 37. La aplicación requiere Android 15/API 35 o superior.
3. Configurar la ubicación del SDK en `local.properties`, archivo local excluido de Git.
4. Ejecutar los siguientes comandos desde la raíz:

```powershell
git clone https://github.com/Sebasss04/RoadMapUtepsa.git
cd RoadMapUtepsa
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

La APK debug se genera en `app/build/outputs/apk/debug/app-debug.apk`. Usar `--offline` únicamente si las dependencias ya están descargadas.

El proyecto utiliza Gradle 9.6.0, AGP 9.4.1 y Kotlin/Compose compiler 2.4.21; el código compila al nivel JVM 11. Las versiones se declaran en [libs.versions.toml](gradle/libs.versions.toml).

Firebase Auth y Firestore están configurados para `roadmap-utepsa-c9409`. `app/google-services.json` contiene configuración cliente. Para ejecutar Google Sign-In con otra clave debug deben registrarse sus huellas SHA-1/SHA-256 con permisos del proyecto Firebase. Compilar el repositorio no despliega reglas ni migra datos.

## Recorrido del código

Fuentes principales: [app/src/main/java/com/example/roadmap_utepsa](app/src/main/java/com/example/roadmap_utepsa).

| Carpeta o archivo | Responsabilidad |
|---|---|
| `MainActivity.kt`, `RoadMapApplication.kt` | Entrada Android, ciclo de vida e inicio de la aplicación. |
| `di/AppContainer.kt` | Construcción de repositorios y dependencias. |
| `domain/` | Modelos, contratos, validaciones, reglas académicas y motor de recomendaciones. |
| `data/` | Repositorios Firebase, transacciones, lectura estable y preferencias DataStore. |
| `presentation/` | Pantallas Compose, estados y ViewModels agrupados por función. |
| `platform/` | Google Sign-In, biometría y notificaciones Android. |
| `app/src/test/` | Pruebas JVM. |
| `app/src/androidTest/` | Pruebas instrumentadas y fixtures de integración. |
| `firebase/` | Reglas Firestore, catálogo de referencia, pruebas y herramientas administrativas. |
| `scripts/` | Comprobación de arquitectura y ejecutores de pruebas. |

La arquitectura usa MVVM en un módulo Android. `AppNavigation` organiza acceso y navegación; Malla y Resumen comparten `AcademicViewModel`. El motor `domain/recommendation/MotorRecomendaciones.kt` calcula recomendaciones sin depender de Android o Firebase. Los adaptadores de datos implementan los contratos del dominio.

## Documentación técnica

- [Reglas académicas y persistencia](docs/REGLAS_ACADEMICAS.md).
- [Validación y ejecución de pruebas](docs/VALIDACION.md).
- [Contrato del catálogo SAE](firebase/catalogo/README.md).
- [Referencias visuales](CONTEXTO/DISENOS/INDICE_Y_ESPECIFICACIONES.md), conservadas como evidencia del diseño.

Las referencias visuales son históricas y pueden diferir de los últimos ajustes. El comportamiento implementado se consulta en el código. Las guías internas de coordinación se retiraron del contenido versionado al cerrar el MVP.
