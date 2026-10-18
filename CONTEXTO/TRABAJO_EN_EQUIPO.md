# Trabajo pendiente y coordinación

Coordinación actualizada el 18/10/2026, hora de Bolivia. El estudiante confirmó que su compañero tomó T1 (Resumen de avance), trabajando a distancia con su propia cuenta de Codex. Las demás asignaciones siguen siendo propuestas; no se acredita implementación ni una rama de T1 por este acuerdo.

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
| T1 / compañero, asignación confirmada | Resumen de avance | Usa datos académicos existentes; muestra cantidades PENDIENTE/EN_CURSO/APROBADA y `aprobadas / 50 × 100`; EN_CURSO no suma como aprobada; contempla carga, error, historial pendiente/incoherente y cambio de cuenta; respeta Figma y no crea escrituras ni listas paralelas de aprobadas. Incluye pruebas del cálculo/casos relevantes y evidencia visual. |
| T2 / autor, rama `feat/oferta-actual-sae` | Oferta actual SAE y exclusión de aprobadas verificadas. El estudiante aprobó sus pruebas en el teléfono y autorizó subir toda la entrega para que el compañero actualice su rama; sustituye la espera anterior de push/PR. Reglas ya publicadas con autorización independiente y relectura productiva verificada. | Selección manual por nombre/sigla sin aprobadas; una oferta editable por cuenta; guardar reemplaza la selección; diferencia ausente de guardada vacía; error conserva selección; sin horarios ni periodos. Validación y límites en VALIDACION.md. |
| T3 / compañero tras T1, coordinado con T2 | Motor de recomendaciones en Kotlin puro | Candidatas ofertadas, pendientes y habilitadas. Simula cada aprobación sin persistir; cuenta desbloqueos inmediatos, incluido umbral 45; no cuenta cadenas, materias ya habilitadas ni activas. Orden: desbloqueos descendente, semestre/código ascendente. Explica requisitos y desbloqueos. Prueba dobles requisitos, umbrales y desempates. Puede comenzar con fixtures mientras se define Oferta. |
| T4 / integración conjunta tras T2 y T3 | Pantalla Recomendaciones | Usa historial coherente/oferta real; maneja oferta ausente/vacía, ninguna candidata, carga y error; no modifica estados; sigue Figma y mantiene separación por sesión. |
| T5 / acordar después | Cuenta y autenticación completa | Correo/contraseña, registro, recuperación y vinculación conservando UID; biometría opcional por cuenta/dispositivo sobre sesión existente, con reintento/cierre. Dividir en tareas antes de tocar Auth y navegación. |
| T6 / autor con QA del compañero | Estabilidad y medición real | Comparar misma APK en físico/AVD estándar 4 KB con recursos suficientes; conservar datos del AVD original. Medir el próximo guardado necesario, sin borrar/recrear historial para fabricar benchmarks. Diferenciar fallo de framework, cierre de app y error recuperable. |
| T7 / quien entregue cada tarea | Calidad y contexto | Pruebas pertinentes, capturas sin datos personales, README/guías actualizadas y PR con alcance, validación y limitaciones. Revisión de reglas antes de una distribución amplia. |

El estado de cada entrega debe contrastarse con código y VALIDACION.md; figurar en este tablero no acredita su cierre. T1 quedó a cargo del compañero. T2 se implementó el 18/10 en `feat/oferta-actual-sae`, independiente del cálculo de Resumen. Las comprobaciones y límites actuales están en VALIDACION.md. Las prioridades pueden ajustarse entre ambos.

Para trabajar T1 y T2 en paralelo, mantener archivos propios de pantalla, ViewModel, repositorio, pruebas y recursos (por ejemplo, `offer_strings.xml` para Oferta). Reutilizar el catálogo existente sin modificar su contrato ni los escritores de Historial/Malla. Acordar quién integra los cambios de `AppNavigation`, `AppContainer` y navegación inferior: son puntos compartidos que pueden generar conflictos aun con ramas separadas. Integrarlos de forma coordinada al cerrar cada entrega. El contrato y las reglas de Oferta pertenecen a T2 y deben validarse con datos sintéticos antes de cualquier despliegue.

## Integración de T2

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

El estudiante terminó primero T2 y autorizó compartirlo el 18/10/2026 tras aprobar
las pruebas manuales. Para incorporar Oferta en la rama existente del compañero,
primero guardar sus cambios propios en un commit y, desde esa misma rama, ejecutar:

```powershell
git fetch origin
git merge origin/feat/oferta-actual-sae
```

Resolver conflictos conservando Resumen y Oferta, sobre todo en `AppNavigation`,
`AppContainer`, `MainActivity` y la navegación inferior. No reemplazar archivos
completos por una sola versión. Repetir compilación/pruebas pertinentes y comprobar
que ambas pantallas sigan accesibles antes de subir T1. Mientras el PR de Oferta
no se integre, actualizar solo `main` no incorpora esta entrega. Si el PR ya fue
integrado, usar `git merge origin/main` en su lugar. No volver a publicar reglas,
migrar catálogo ni ejecutar pruebas personales de escritura al actualizar la rama.

1. Antes de empezar, indicar en el PR o en este documento quién toma la tarea y su rama. Comprobar `git status`; no resetear cambios ajenos.
2. Partir de `main` actualizado. Ejemplo para el compañero: `git switch -c feat/resumen-avance`. Una rama por entrega; integrar por PR revisado por el otro integrante.
3. T1 puede reutilizar `AcademicRepository`, `AcademicViewModel` y las reglas de dominio. Coordinar cambios compartidos de `AppNavigation`, `AppContainer`, recursos y navegación inferior. T2/T3 deben acordar el contrato de Oferta antes de integrarse.
4. Mantener contrato de revisión para cualquier escritor de estados/preparación/diario, incluso un futuro Admin. No eliminar comparaciones para acelerar el guardado.
5. Al cerrar cada entrega, actualizar las secciones vigentes de ESTADO_ACTUAL y este tablero: realizado, pruebas, limitaciones, responsable y siguiente paso. No acumular nuevos relevos por chat.

## Validar sin tocar datos personales

Desde la raíz, ejecutar `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` (en Windows `./gradlew.bat`). Las pruebas Python y Node sin conexión están descritas en VALIDACION.md.

No ejecutar toda la instrumentación Android a ciegas: `PersonalAcademicPersistenceTest` y `scripts/test-personal-history.ps1` escriben datos de una cuenta real. Las pruebas SDK requieren emuladores Auth/Firestore demo y opt-in; las UI en memoria no acreditan Firebase real. Los runners PowerShell existentes tienen rutas/AVD del autor: adaptar parámetros a un entorno sintético adecuado, conservando sus guardas.

Publicar código no vuelve a migrar el catálogo ni despliega reglas. El catálogo SAE y el contrato de revisión ya están publicados según el estado vigente; no repetir sus publicadores como parte del onboarding.
