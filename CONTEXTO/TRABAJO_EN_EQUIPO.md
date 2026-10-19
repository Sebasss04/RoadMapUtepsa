# Trabajo pendiente y coordinación

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
en VALIDACION.md. Repositorios, CAS, Historial, Oferta, motor y contratos preservados.
Solo se corrigió producción en ResumenUiState; AcademicFlowTest ajusta teclado/scroll
sin retirar aserciones. Sin Firebase productivo ni cuentas reales. Publicar requiere
una autorización nueva expresa. Los apartados de integración siguientes son antecedentes.


## Integración local vigente — 18/10/2026

Sebastián autorizó traer todos los cambios de Ale y unificarlos con T1. main avanzó
de `96a400a` a `30685a6` por fast-forward; Oferta/Para ti ya están integrados en Git.
Resumen está conectado localmente, sin confirmar ni publicar. Se conservaron los
19 archivos anteriores mediante respaldo verificado y reconciliación de navegación.
Ale conserva T2/T3/T4; esta intervención no rehace sus funciones ni altera contratos.
Las cuatro pestañas comparten UID/sesión y conservan filtros/borrador/revisiones.
Ver ESTADO_ACTUAL y VALIDACION para implementación, pruebas y pendientes reales.
Los apartados siguientes sobre PR #2 abierto son registros anteriores al merge.

Validación: 257 JVM y 38 Android sintéticos correctos; lint 0 errores/39 advertencias.

## Qué se entregó hasta ahora

- Google y sesión Firebase; Perfil con lectura, alta, edición y validaciones.
- Dominio académico Kotlin, catálogo remoto de 50 materias, Malla con búsqueda/filtros/edición y controles de concurrencia.
- Historial: primera preparación, consulta posterior, edición precargada, diferencias, confirmación, posposición inicial, recuperación de diario y conflictos que conservan el borrador.
- Catálogo SAE: 28 relaciones, Taller con 45 aprobadas excluyéndose a sí mismo, 40 modulares, 9 bimodulares y una duración especial. Nombres principales y siglas secundarias.
- Optimización: revisión global académica, grupos de cinco cambios, snapshot coherente, caché de catálogo por sesión/plan de hasta cinco minutos y relectura final. Reglas compatibles ya publicadas según las verificaciones registradas.
- FCM y saludo local. La recepción de un saludo no acredita un push remoto.
- Diagnóstico del emulador: fallos de ART y `system_server` incluso sin proceso de la app. La causa exacta y estabilidad del AVD de 16 KB siguen abiertas. No hay Room en la app.

## Tareas y criterios de entrega

| ID / propuesta | Trabajo | Se considera terminado cuando… |
|---|---|---|
| T1 / Sebastián, integrado en copia local sin commit | Resumen de avance conectado a main `30685a6` | Usa datos académicos existentes; muestra cantidades PENDIENTE/EN_CURSO/APROBADA y `aprobadas / 50 × 100`; EN_CURSO no suma como aprobada; contempla carga, error, historial pendiente/incoherente y cambio de cuenta; respeta Figma y no crea escrituras ni listas paralelas de aprobadas. Incluye pruebas del cálculo/casos relevantes y evidencia visual. |
| T2 / autor, rama `feat/oferta-actual-sae` | Oferta actual SAE y exclusión de aprobadas verificadas. El estudiante aprobó sus pruebas en el teléfono y autorizó subir toda la entrega para que el compañero actualice su rama; sustituye la espera anterior de push/PR. Reglas ya publicadas con autorización independiente y relectura productiva verificada. | Selección manual por nombre/sigla sin aprobadas; una oferta editable por cuenta; guardar reemplaza la selección; diferencia ausente de guardada vacía; error conserva selección; sin horarios ni periodos. Validación y límites en VALIDACION.md. |
| T3 / autor-Codex, `feat/para-ti` | Motor Kotlin puro implementado | Prioridad de pendientes anteriores, desbloqueos completos y avances parciales; principal/secundaria/opcionales, desempates y cero; requisitos dobles, cadenas y umbral 44→45 probados. Ver VALIDACION.md. |
| T4 / autor-Codex, `feat/para-ti` | Diseño Figma y pantalla terminados; validación seleccionada correcta | Lecturas guardadas estables, gates explícitos, explicación, sesión, navegación y conservación de Oferta verificados. Cierre real en VALIDACION.md. |
| T5 / acordar después | Cuenta y autenticación completa | Correo/contraseña, registro, recuperación y vinculación conservando UID; biometría opcional por cuenta/dispositivo sobre sesión existente, con reintento/cierre. Dividir en tareas antes de tocar Auth y navegación. |
| T6 / autor con QA del compañero | Estabilidad y medición real | Comparar misma APK en físico/AVD estándar 4 KB con recursos suficientes; conservar datos del AVD original. Medir el próximo guardado necesario, sin borrar/recrear historial para fabricar benchmarks. Diferenciar fallo de framework, cierre de app y error recuperable. |
| T7 / quien entregue cada tarea | Calidad y contexto | Pruebas pertinentes, capturas sin datos personales, README/guías actualizadas y PR con alcance, validación y limitaciones. Revisión de reglas antes de una distribución amplia. |

El estado de cada entrega debe contrastarse con código y VALIDACION.md; figurar en este tablero no acredita su cierre. T1 quedó a cargo del compañero. T2 se implementó el 18/10 en `feat/oferta-actual-sae`, independiente del cálculo de Resumen. Las comprobaciones y límites actuales están en VALIDACION.md. Las prioridades pueden ajustarse entre ambos.

Para trabajar T1 y T2 en paralelo, mantener archivos propios de pantalla, ViewModel, repositorio, pruebas y recursos (por ejemplo, `offer_strings.xml` para Oferta). Reutilizar el catálogo existente sin modificar su contrato ni los escritores de Historial/Malla. Acordar quién integra los cambios de `AppNavigation`, `AppContainer` y navegación inferior: son puntos compartidos que pueden generar conflictos aun con ramas separadas. Integrarlos de forma coordinada al cerrar cada entrega. El contrato y las reglas de Oferta pertenecen a T2 y deben validarse con datos sintéticos antes de cualquier despliegue.

## Integración de T2

La petición vigente sustituye el plan preparado: autor/Codex asume T3 y T4 en
`feat/para-ti`, sin esperar a Resumen. Diseño actualizado y revisado antes de Compose;
implementación y pruebas detalladas en PRODUCTO_Y_PLAN y VALIDACION. Se consultó
`origin/main` durante esta entrega: `2f21871` integra el PR #1 de Oferta y su árbol
coincide con la base de T2. Rama local adelantada a ese merge sin perder cambios.
No se encontraron cambios T1 en main. No se ha enviado ningún mensaje al compañero.

El estudiante solicitó después implementar los ajustes de Para ti: avances
parciales, secundaria con beneficio, opcionales y simplificación visual/textual.
Autor/Codex mantiene T3/T4; T1 sigue con el compañero. El estudiante autorizó
posteriormente publicar toda la entrega el 18/10/2026. La espera anterior de
commit/push queda sustituida por publicación en `feat/para-ti` y PR hacia `main`,
sin merge automático. Conservar las reglas de PRODUCTO_Y_PLAN.md y consultar
VALIDACION.md y el inventario de continuidad en README.md.

Tras probar Oferta, el estudiante pidió excluir las materias ya aprobadas del
selector. Ajuste implementado e instalado: estados reales en `DatosOferta`, búsqueda
filtrada, EN_CURSO/pendientes disponibles y refresco al volver desde Malla conservando
borrador y revisión esperada. No requiere desplegar nuevas reglas ni cambiar
AcademicData/AcademicRepository. 199 JVM, nueve UI sintéticas y una lectura real
en el teléfono correctas; ver VALIDACION.md. Mantener este filtro al integrar T1.

Oferta añade `AppContainer.ofertaRepository`, un parámetro opcional al final de
`AppNavigation`, el acceso `onOffer` opcional a CurriculumScreen y la conexión desde
MainActivity. Pantalla, recursos, ViewModel, repositorio y pruebas son propios.
En el estado de `origin/main` consultado al iniciar no había cambios T1: se mantienen
Resumen y Para ti deshabilitados. Si T1 llega antes de integrar, incorporar `main`
a esta rama y conservar ambas pantallas al resolver navegación/contenedor/menú.
No fusionar automáticamente: el compañero debe revisar el PR. Las reglas de Oferta
se probaron en demo y ya se publicaron con autorización explícita posterior al
diagnóstico del teléfono; no repetir el despliegue como parte de la integración.

## Cómo evitar pisarse

El PR #1 de Oferta ya fue integrado en main por el equipo: consulta remota de esta
entrega confirmó `2f21871` y la rama Para ti se adelantó a ese commit. Su árbol
coincide con la base de Oferta; no había cambios nuevos de Resumen para integrar.
Las instrucciones anteriores para incorporar la rama T2 quedan sustituidas por
actualizar `origin/main` en la rama propia, preservando antes los cambios locales.

1. Mantener T1 del compañero y T3/T4 del autor en archivos propios. Para ti añade
   parámetros opcionales al final de AppNavigation, CurriculumScreen y OfertaScreen.
   Conservarlos junto a Resumen al resolver puntos compartidos (AppContainer,
   MainActivity, AppNavigation y barras), sin reemplazar archivos completos.
2. Consultar estado y diferencias antes de integrar; no resetear cambios ajenos.
3. Mantener revisión académica, CAS, diario, conflictos y relecturas de servidor.
4. No volver a publicar reglas ni migrar catálogo al actualizar el código.
5. Registrar resultados y límites en VALIDACION.md; actualizar las guías vigentes.
   Para ti tiene autorización de publicación del 18/10/2026; revisar su PR antes de integrar.

## Registro anterior: incorporar Para ti mientras Resumen seguía en desarrollo

Después de revisar el PR de `feat/para-ti`, el compañero puede incorporar esta
rama a su rama de Resumen sin esperar al merge de `main`. Conservar primero sus
cambios en un commit de su propia rama y ejecutar desde ella:

```powershell
git fetch origin
git merge origin/feat/para-ti
```

Entrega publicada: `e13ef13`,
[PR #2](https://github.com/Sebasss04/RoadMapUtepsa/pull/2), pendiente de revisión.

Resolver conservando Resumen y Para ti en `AppContainer`, `MainActivity`,
`AppNavigation` y las barras de navegación. No reemplazar estos archivos completos.
Después ejecutar las verificaciones pertinentes. Cuando el PR esté integrado en
`main`, actualizar desde `origin/main` en lugar de incorporar nuevamente la rama.
El push no habilita ni implementa Resumen y no requiere desplegar Firebase.

## Validar sin tocar datos personales

Desde la raíz, ejecutar `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` (en Windows `./gradlew.bat`). Las pruebas Python y Node sin conexión están descritas en VALIDACION.md.

No ejecutar toda la instrumentación Android a ciegas: `PersonalAcademicPersistenceTest` y `scripts/test-personal-history.ps1` escriben datos de una cuenta real. Las pruebas SDK requieren emuladores Auth/Firestore demo y opt-in; las UI en memoria no acreditan Firebase real. Los runners PowerShell existentes tienen rutas/AVD del autor: adaptar parámetros a un entorno sintético adecuado, conservando sus guardas.

Publicar código no vuelve a migrar el catálogo ni despliega reglas. El catálogo SAE y el contrato de revisión ya están publicados según el estado vigente; no repetir sus publicadores como parte del onboarding.
