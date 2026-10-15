# RoadMap UTEPSA

Proyecto Android de RoadMap UTEPSA. Este repositorio contiene la versión del proyecto de Android Studio publicada el 15 de octubre de 2026, con los cambios de acceso con Google, navegación y perfil conectado a Firestore.

## Clonar y ejecutar

```sh
git clone https://github.com/Sebasss04/RoadMapUtepsa.git
cd RoadMapUtepsa
```

1. Abrir esta carpeta en Android Studio (la que contiene `settings.gradle.kts`).
2. Usar una versión de Android Studio compatible con Android Gradle Plugin **9.3.3**. El wrapper incluido utiliza Gradle **9.5.0** y `gradle/gradle-daemon-jvm.properties` solicita **JDK 25**. Permitir las descargas de Gradle y sus dependencias al sincronizar.
3. Instalar **Android SDK Platform 37** desde SDK Manager y dejar que Android Studio configure la ruta local del SDK. `local.properties` se genera en cada máquina y no se comparte.
4. Crear un emulador con **API 35 o superior**, preferiblemente con Google Play para probar el acceso con Google, o conectar un dispositivo compatible. El proyecto tiene `minSdk = 35` y `targetSdk = 37`.
5. Sincronizar Gradle, seleccionar el módulo `app` y pulsar **Run**. Se necesita conexión a Internet para acceder a Firebase.

## Firebase y acceso con Google

Se incluye `app/google-services.json` del proyecto **roadmap-utepsa-c9409**, con el identificador de aplicación `com.example.roadmap_utepsa`. La app utiliza ese mismo backend de Firebase; clonar el repositorio no copia las sesiones ni los datos locales del dispositivo original.

Cada máquina genera su propia clave de depuración. Para probar Google Sign-In, obtener las huellas de la variante `debug`:

```powershell
.\gradlew.bat :app:signingReport
```

El administrador del proyecto Firebase debe registrar las huellas **SHA-1 y SHA-256** de esa máquina en la aplicación Android. Si Firebase genera una configuración actualizada, descargar `google-services.json`, reemplazar el archivo de `app` y volver a sincronizar. No se comparte la clave de firma privada de otra máquina.

La pantalla de perfil lee las referencias `carreras/ingenieria-sistemas` y `planes_estudio/sistemas-rm-615-2016`. Se incluyen las reglas en `firebase/firestore.rules` y las referencias en `firebase/profile-references.json`. Ejecutar la app contra el backend existente no requiere desplegar reglas ni volver a cargar referencias. Los scripts administrativos de `firebase/scripts` no son necesarios para abrir o compilar la app.

## Comprobar la compilación

En Windows, con Java configurado:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

En macOS/Linux:

```sh
sh ./gradlew :app:assembleDebug :app:testDebugUnitTest
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Archivos compartidos

Se versionan el código, los recursos gráficos, la configuración de compilación, el wrapper de Gradle, la configuración cliente de Firebase, las reglas y las pruebas. Las carpetas de compilación, las cachés y las preferencias locales de Android Studio se regeneran en cada equipo.
