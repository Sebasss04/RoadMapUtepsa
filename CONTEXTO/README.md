# Contexto compartido del proyecto

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


## Integración local — 18/10/2026

main local actualizado por fast-forward a `30685a6`. Los PR #1 y #2 están integrados.
T1 se unificó localmente con Oferta/Para ti, sin commit. No volver a fusionar sus
ramas. Estado, respaldo y límites en ESTADO_ACTUAL; comprobaciones en VALIDACION.
La integración fue autorizada expresamente después del plan; sin push ni Firebase.

Validación: 257 JVM y 38 Android sintéticos correctos; lint 0 errores/39 advertencias.

## Orden de lectura

1. [ESTADO_ACTUAL.md](ESTADO_ACTUAL.md): qué existe y qué se comprobó.
2. [PRODUCTO_Y_PLAN.md](PRODUCTO_Y_PLAN.md): alcance y reglas que deben conservarse.
3. [TRABAJO_EN_EQUIPO.md](TRABAJO_EN_EQUIPO.md): tareas con criterios de entrega.
4. [VALIDACION.md](VALIDACION.md): comprobaciones de esta publicación y sus límites.

Las dos guías principales se incorporaron desde la documentación local vigente y se adaptaron para el clon. Sus secciones de despliegues, auditorías y pruebas Android narran verificaciones anteriores; los archivos de evidencia bajo `app/build/reports/` permanecen locales. Los cierres «sin commits ni push» describen esas sesiones previas y quedan superados por esta entrega a Git. Los pendientes antiguos no deben reejecutarse como instrucciones actuales.

T2 (Oferta) incorpora código y contrato en `feat/oferta-actual-sae`
(commit inicial de implementación `269aa92` y correcciones posteriores). El
18/10/2026, tras aprobar sus pruebas en el teléfono, el estudiante autorizó subir
toda la entrega al repositorio para que el compañero actualice su rama. Esta
instrucción sustituye la espera anterior de push/PR; la integración a `main` se
revisa mediante PR, sin merge automático. Leer VALIDACION.md. Las reglas de Oferta
ya se publicaron con autorización independiente y relectura productiva verificada;
no repetir el despliegue al descargar estos cambios.
El contrato de documento único está en PRODUCTO_Y_PLAN.md. Resumen sigue el trabajo
independiente del compañero y deberá conservarse al integrar ambas ramas.
El ajuste posterior del selector excluye APROBADA del historial real y conserva
EN_CURSO/pendientes; ya está instalado y verificado en el teléfono sin modificar
datos personales. Detalles y pruebas en VALIDACION.md.

Entrega de Oferta subida a `origin/feat/oferta-actual-sae` e integrada mediante el
[PR #1](https://github.com/Sebasss04/RoadMapUtepsa/pull/1) en `main` (`2f21871`).
Incluye `269aa92` (implementación) y `18056b0` (correcciones y contexto).
El compañero puede actualizar su rama con los comandos de TRABAJO_EN_EQUIPO.md.

Para ti: el estudiante asignó motor y pantalla al autor/Codex en `feat/para-ti`;
Resumen continúa con el compañero. Diseño de 13 estados terminado antes de Compose;
código conectado y pruebas detalladas en VALIDACION.md. El plan anterior y la
propuesta de T3 para el compañero quedan sustituidos por esta asignación.

## Registro anterior de publicación de Para ti

Correcciones solicitadas e implementadas el 18/10/2026: avances parciales,
principal/secundaria/opcionales y contenido breve con nueva jerarquía de colores.
El alcance vigente está en PRODUCTO_Y_PLAN.md; las comprobaciones en VALIDACION.md.

Leer las cuatro guías indicadas arriba. La rama es `feat/para-ti`, sobre
`2f21871`. El 18/10/2026 el estudiante autorizó publicar todos los cambios de
Para ti, incluidos ajustes, pruebas, diseños y documentación. Esta autorización
sustituye la espera anterior de commit/push. La entrega está publicada en esta rama
con PR hacia `main`, sin merge automático; consultar VALIDACION.md para su estado.
Push confirmado en `origin/feat/para-ti`, implementación `e13ef13`;
[PR #2](https://github.com/Sebasss04/RoadMapUtepsa/pull/2) abierto para revisión.
Antes de continuar, revisar `git status` y conservar cualquier cambio local nuevo.

| Parte | Archivos principales, relativos a la raíz del repositorio |
|---|---|
| Motor y resultado | `app/src/main/java/com/example/roadmap_utepsa/domain/recommendation/MotorRecomendaciones.kt` |
| Lectura conjunta | `data/RecomendacionesRepository.kt` y adaptador `data/EstadosVersionados.kt`, dentro del mismo paquete |
| Estado y sesión | `presentation/viewmodel/RecomendacionesViewModel.kt` |
| Pantalla | `presentation/recommendation/RecomendacionesScreen.kt`, `res/values/recommendation_strings.xml` y cuatro vectores nuevos |
| Integración compartida | `AppContainer.kt`, `MainActivity.kt`, `presentation/navigation/AppNavigation.kt`, barras de `CurriculumScreen.kt` y `OfertaScreen.kt` |
| Pruebas | `MotorRecomendacionesTest`, `RecomendacionesRepositoryTest`, `RecomendacionesViewModelTest`, `RecomendacionesFlowTest` y `RecomendacionesSdkEmulatorTest` |
| Ejecución y evidencia | `scripts/test-recommendations.ps1`, `app/build/reports/recommendations/adjustments-*` y `DISENOS/08_PARA_TI_AJUSTES_20261018.png` |

Revisión actual de ajustes: **232 JVM y 15 UI correctas**, compilación principal y
androidTest correctas; lint con cero errores y 39 advertencias previas. Capturas
con fuente 1,5 y contraste revisados. Ver la primera sección de VALIDACION.md.
No se repitió SDK demo, pues los contratos y la lectura no cambiaron.

Entrega inicial, antes de los ajustes: 222 JVM, 13 UI seleccionadas y 1 SDK demo correctas;
compilación correcta y lint con 0 errores/39 advertencias. El capturador de
androidTest se ajustó después y se volvió a verificar el detalle con fuente 1,5.
Consultar VALIDACION.md para el alcance exacto, comandos, hash de APK y el ANR
de System UI del AVD sintético. Los informes bajo `app/build/` son locales e
ignorados: un clon distinto no los tendrá automáticamente. Los servicios demo y
el AVD de pruebas quedaron detenidos; no se instaló esta entrega en el teléfono.

El próximo chat debe preservar los ajustes implementados, contrastando código,
Figma y evidencia sin rehacer Para ti ni asumir que Resumen ya llegó. El compañero conserva Resumen. No repetir despliegues, migraciones ni
pruebas personales por encontrar sus registros históricos.

## Fuentes y enlaces

| Recurso | Ubicación y uso |
|---|---|
| Repositorio del equipo | [Sebasss04/RoadMapUtepsa](https://github.com/Sebasss04/RoadMapUtepsa). Fuente del código actual. |
| Figma | [Interfaces RoadMap UTEPSA](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=17-482). Ver [índice por pantalla](DISENOS/INDICE_Y_ESPECIFICACIONES.md) y exportaciones en esta carpeta. |
| Ejemplo de la ingeniera | [PatriciaAG/proyecto-final-autenticador](https://github.com/PatriciaAG/proyecto-final-autenticador). Referencia didáctica; no es el código del producto. El informe local de FCM cita el commit `279ba7e439838c34dcc47ca3c89557e01a39f86f`; no se ha vuelto a verificar su rama remota en esta publicación. |
| Fuente curricular actual | [Pénsum SAE, 17/10/2026](Pensum%20Ingenieria%20de%20Sistemas.pdf). 50 materias, 28 relaciones, Taller con umbral 45. |
| Malla anterior | [PDF anterior](REFERENCIAS/ing.-de-sistemas-mallas-pensum-2.pdf). Complementa nombres completos/áreas; no sustituye los requisitos SAE actuales. |
| Catálogo técnico | [JSON SAE](../firebase/catalogo/sistemas-rm-615-2016.json), [baseline anterior](../firebase/catalogo/sistemas-rm-615-2016-v1.json), [contrato](../firebase/catalogo/README.md). |
| Firebase | Proyecto `roadmap-utepsa-c9409`, Firestore `(default)`; [reglas](../firebase/firestore.rules). Pedir acceso por cuenta propia. |
| Arquitectura y datos | [Arquitectura](REFERENCIAS/Arquitectura%20Roadmap%20Utepsa.mmd), [base conceptual](REFERENCIAS/Base%20de%20datos%20RoadMap%20Utepsa.mmd), [Firestore](REFERENCIAS/Diagrama%20de%20BD%20Firestore.mmd). Son propuestas que también incluyen funciones futuras; contrastar con código. |

Las exportaciones B1/B2 conservan README históricos como evidencia visual. Para implementación y pendientes prevalecen las guías de esta carpeta. No se incluyen logs personales, capturas con tokens, cachés, claves ni respaldos completos del autor.

## Primer uso desde el Codex del compañero

Clonar el repositorio y abrir la raíz del clon como proyecto. El archivo raíz `AGENTS.md` indica dónde leer el contexto. No hace falta recrear la estructura de carpetas de la computadora original.

Mensaje sugerido para su primer chat:

> Lee AGENTS.md, CONTEXTO/README.md, CONTEXTO/ESTADO_ACTUAL.md, CONTEXTO/PRODUCTO_Y_PLAN.md y CONTEXTO/TRABAJO_EN_EQUIPO.md. Contrasta el estado con el código. Mi primer encargo es implementar Resumen según la tarea T1: revisa el Figma y conserva las reglas de avance y sesión existentes. Trabaja en una rama propia, verifica los cambios y actualiza el contexto con lo entregado y pendiente. Si faltan permisos o decisiones, indícalos sin inventar resultados ni tocar datos personales.

El reparto es una propuesta para coordinarse; no implica que el compañero ya haya aceptado, comenzado o completado trabajo. Este envío tampoco le manda mensajes ni inicia su Codex.
