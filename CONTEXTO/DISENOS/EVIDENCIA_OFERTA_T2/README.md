# Evidencia visual T2 — 18/10/2026

Capturas de OfertaScreen en AVD aislado `RoadMap_Offer_Test_API35` API35 (4 KB),
1080×2400. Autenticación/repositorios UI en memoria y catálogo como fixture solo
en androidTest; sin perfil ni estados personales. La integración SDK real se probó
por separado con Auth/Firestore demo: estas imágenes no acreditan producción.

| Captura | Estado |
|---|---|
| [01](01-ausente.png) | Documento no registrado; seleccionar primera oferta. |
| [02](02-vacia.png) | Oferta guardada sin materias; distinta de ausencia. |
| [03](03-editor.png) | Búsqueda por sigla, selección y duración. |
| [04](04-guardando.png) | Guardado en curso; controles de envío deshabilitados. |
| [05](05-error.png) | Error conserva selección y permite reintentar. |
| [06](06-guardada.png) | Consulta después de confirmar guardado. |
| [07](07-conflicto.png) | Borrador local y selección remota con decisión explícita. |
| [08](08-fuente-ampliada.png) | Fuente 1,5; nombre largo íntegro y controles accesibles. |
| [09](09-error-lectura.png) | Fallo de lectura, sin inferir ausencia. |
| [10](10-carga.png) | Lectura en curso. |

Comparadas con Figma 15:431/15:494. Se conservan los iconos y tokens existentes;
se adapta el orden a nombre principal + sigla/duración por el contrato vigente.
Las acciones de cancelar, refrescar y cerrar sesión completan el flujo solicitado;
Resumen y Para ti siguen deshabilitados porque no están integrados en este checkout.
Barras del sistema y hora corresponden al AVD. No se altera la UI de otras pantallas.
Resultados y límites vigentes: [VALIDACION.md](../../VALIDACION.md).
