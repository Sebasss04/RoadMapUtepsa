# Contexto compartido del proyecto

Actualizado el 18/10/2026 para T2, hora de Bolivia. Esta carpeta permite continuar en otra computadora sin el historial de chats del autor.

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

Entrega subida a `origin/feat/oferta-actual-sae` y abierta para revisión en el
[PR #1](https://github.com/Sebasss04/RoadMapUtepsa/pull/1), sin integrar a `main`.
Incluye `269aa92` (implementación) y `18056b0` (correcciones y contexto).
El compañero puede actualizar su rama con los comandos de TRABAJO_EN_EQUIPO.md.

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
