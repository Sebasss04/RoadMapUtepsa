> Referencia visual histórica. Para implementación y pendientes leer [ESTADO_ACTUAL.md](../../ESTADO_ACTUAL.md). Las instrucciones fechadas siguientes se conservan como registro, no como tareas vigentes.

# Historial · entrega visual B2

Diseñado el 16/10/2026 · America/La_Paz.
**Aprobado expresamente el 17/10/2026:** «Apruebo B2 completo» (H09–H32).
H17 sigue condicional al contrato y pruebas de D. La aprobación visual no autoriza
despliegues Firebase ni la prueba personal de escritura/reapertura/restauración.
Los registros inferiores conservan el estado histórico de la entrega visual.
B1 (H01–H08) sigue aprobada y se conserva intacta. Esta entrega añade **24 estados y
paneles H09–H32**, no 24 destinos nuevos de navegación, en seis tableros del mismo
archivo y la misma página Figma. No desarrolla interfaces Android ni ejecuta
operaciones Firebase. Las unidades C–G no se han iniciado.

## Tableros y exportaciones

Los tableros están a la derecha, en x=4400 y de y=2600 a y=8600. Cada uno contiene
cuatro vistas. Los mockups son estáticos y editables, sin prototipo conectado.
Botones y mensajes representan comportamiento futuro; no prueban su implementación.

| Grupo | Figma | Exportación |
|---|---|---|
| 12A · Historial — lectura y selección | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=50-643) | [PNG](01_LECTURA_Y_SELECCION.png) |
| 12B · Historial — guardado y confirmación | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=50-2077) | [PNG](02_GUARDADO.png) |
| 12C · Historial — recuperación y sesión | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=51-689) | [PNG](03_RECUPERACION_Y_SESION.png) |
| 12D · Malla — transiciones y coherencia | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=52-705) | [PNG](04_PANELES_MALLA.png) |
| 12E · Historial — corrección, umbral y acceso | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=53-745) | [PNG](05_CORRECCION_Y_UMBRAL.png) |
| 12F · Historial — avisos y duración ilustrativa | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=54-773) | [PNG](06_AVISOS_Y_DURACION.png) |

## Índice de estados

| Vista | Nodo | Comportamiento que se propone |
|---|---|---|
| H09 · Cargando historial | [50:649](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=50-649) | Lectura en curso; no mostrar aprobadas ni inferir cero. |
| H10 · Error de lectura | [50:993](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=50-993) | Error diferenciado de historial ausente; reintento sin perder estados. |
| H11 · Sin materias en curso | [50:1339](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=50-1339) | Variante de cero en curso con cinco aprobadas conservadas. |
| H12 · Búsqueda sin resultados | [50:1716](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=50-1716) | Búsqueda sin coincidencias y contador que conserva las cinco selecciones. |
| H13 · Guardando | [50:2083](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=50-2083) | Envío pendiente; guardar desactivado y sin anuncio de éxito. |
| H14 · Espera de confirmación | [50:2430](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=50-2430) | Demora de confirmación; no tratarla como fracaso ni reenviar. |
| H15 · Error con borrador | [50:2774](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=50-2774) | Error de resultado incierto; conservar borrador y releer antes del reintento. |
| H16 · Historial confirmado | [50:3123](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=50-3123) | Resumen tras confirmación y lectura correctas; no certificación SAE. |
| H17 · Guardado parcial / condicional | [51:695](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=51-695) | Alternativa condicional de progreso parcial, si el contrato requiere grupos. |
| H18 · Cambios en otro dispositivo | [51:1042](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=51-1042) | Cambio concurrente de BMS-301; comparar sin sobrescribir silenciosamente. |
| H19 · Descartar revisión | [51:1391](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=51-1391) | Confirmación de descarte antes de enviar; no revierte estados del servidor. |
| H20 · Sesión expirada | [51:1737](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=51-1737) | Invalidación por cambio/fin de sesión y nueva lectura por cuenta. |
| H21 · Bloquear En curso | [52:711](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=52-711) | SIS-306 no puede pasar a En curso si falta SIS-305, aunque SCC-305 esté aprobada. |
| H22 · Retirar aprobación utilizada | [52:1064](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=52-1064) | Bloquear retirada aislada de SCC-305 que deja SIS-306 aprobada sin requisito. |
| H23 · Volver a Pendiente | [52:1416](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=52-1416) | BMS-301 En curso → Pendiente manualmente, sin cuarto estado ni notas. |
| H24 · Aprobar con omisión | [52:1768](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=52-1768) | Permitir registrar SIS-306 Aprobada y explicar omisión; no aprobar requisitos. |
| H25 · Corrección conjunta | [53:751](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=53-751) | Revisión conjunta sin borrar aprobaciones reales para añadir SIS-305 omitida. |
| H26 · Taller / 39 aprobadas | [53:1100](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=53-1100) | Taller con 39 aprobadas distintas de sí misma: falta una; En curso bloqueado. |
| H27 · Taller / 40 aprobadas | [53:1452](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=53-1452) | Taller con 40 aprobadas distintas de sí misma y ejemplo coherente: puede guardar En curso. |
| H28 · Preparación pendiente | [53:1804](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=53-1804) | Entrada futura de recomendaciones ante preparación no confirmada. |
| H29 · Historial contradictorio | [54:779](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=54-779) | Entrada futura de recomendaciones ante contradicción, incluso si antes se confirmó. |
| H30 · Bimodular / ilustrativo | [54:1125](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=54-1125) | Detalle ilustrativo, sin código ni asignación a una materia real; solo duración visible. |
| H31 · Posponiendo preparación | [54:1478](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=54-1478) | Guardado de la decisión de posponer; conserva estados. |
| H32 · Error al posponer | [54:1822](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=54-1822) | Error al confirmar posposición, relectura previa al reintento y acceso a Malla. |

## Recorrido con B1 aprobada

1. Tras Perfil completo, H09 espera una lectura completa correcta. H10 ofrece
   reintento si falla. Solo una lectura correcta puede distinguir preparación
   ausente, pospuesta o confirmada; un fallo no significa cero aprobadas.
2. H01 → H02/H03 → H04 o H11 → H05/H06. H11 representa un conjunto sin materias en
   curso, no una acción que borre estados existentes. Todas las selecciones ya
   guardadas deben cargarse y cualquier cambio debe revisarse explícitamente.
3. H12 puede aparecer al buscar en la selección; limpiar búsqueda no limpia el
   borrador. Semestre y búsqueda conservan sus elecciones al cambiar de vista.
4. Confirmar H05/H06 → H13 → H14 si hay demora → H16 solo tras confirmación,
   relectura y comprobación del conjunto. La variante de éxito muestra 5 aprobadas,
   1 en curso y 44 pendientes; cero aprobadas también es una confirmación válida.
5. Un error incierto lleva a H15, sin afirmar que no llegó ninguna escritura. El
   reintento primero comprueba qué se guardó. H17 se aplica únicamente si D demuestra
   necesario guardar por grupos; conserva progreso confirmado e idempotencia y no
   marca preparado hasta terminar, releer y validar todo.
6. Cambios concurrentes → H18. La acción de leer y revisar abre una comparación
   explícita; no es permiso para reemplazar cambios ajenos. El borrador de la revisión
   se mantiene mientras se comparan diferencias. Una nueva diferencia exige revisar
   de nuevo antes de escribir. El contrato y las garantías reales se fijarán en D.
7. Salir con cambios no enviados → H19; cancelar conserva la revisión y descartar
   conserva el servidor. Esta confirmación no se usa para deshacer guardado parcial
   ni para abandonar silenciosamente una operación pendiente; antes se reconcilia
   el resultado con H14/H15/H17.
8. Cambio o fin de sesión → H20; bloquear guardados y descartar datos expuestos de
   la cuenta anterior. Tras acceso, releer por la cuenta actual. No se promete
   recuperar borradores tras cerrar sesión/proceso. Un eventual borrador persistente
   por UID requiere contrato explícito y diseño posterior si cambia estos mensajes.
9. H01 «Lo haré después» → H31 → confirmación y relectura → H08. Ante resultado
   incierto: H32, comprobación antes del reintento. Puede consultar Malla aunque
   no se confirme la decisión; no anunciar que se recordará si aún no se comprobó.
10. Malla: al seleccionar En curso, H21 explica el requisito faltante; H26/H27
    muestran el umbral 39/40. Se bloquea el cambio conservando el estado anterior
    si no cumple; cumplirlo no produce cambios automáticos.
11. Quitar una aprobación utilizada → H22 → H25/H07. No retirar automáticamente
    estados dependientes. Se permite corregir omisiones sin borrar aprobaciones
    reales; recalcular requisitos y umbrales de todo el conjunto antes de confirmar.
    Una corrección que reduce contradicciones no debe bloquearse por otra ajena.
12. Resultado sin aprobación: H23 permite volver manualmente a Pendiente. Aprobar
    en Malla con una omisión usa H24: puede registrar el dato autodeclarado, pero
    la contradicción impide recomendaciones. Nunca aprobar requisitos por inferencia.
13. Recomendaciones futuras: preparación no confirmada → H28; contradicción tras
    recálculo → H29 → revisión localizada H07/H25. Esto no implementa el recomendador,
    su oferta ni sus pestañas. Malla mantiene el acceso actual.

## Alcance de duración

H30 es deliberadamente **ilustrativa, sin materia asignada ni código real**. El
texto «Bimodular · 2 meses» expresa únicamente duración. No se ha clasificado ninguna
materia, modificado catálogo, agregado metadatos remotos ni contactado a SAE.
La ausencia de duración no equivale a modular de un mes. No se añade filtro,
calendario, estado, prioridad ni restricción de carga.

SAE sigue sin responder sobre la lista o un contacto alternativo; no hay fecha
prometida. La futura aplicación del diseño a una materia requiere fuente
confirmada y revisión del contrato de metadatos. No bloquea historial ni coherencia.

## Evidencia y límites

- Se conservan los diseños previos, incluidas las ocho vistas B1 aprobadas.
- 24 vistas de 412 × 892 en seis tableros de 1816 × 1060.
- [Evidencia estructural](evidencia-estructural.json): texto, frames, vectores,
  rectángulos e instancias; fuente Roboto en los seis tableros; cero nodos con
  relleno de imagen. No se insertó ninguna captura como interfaz editable.
- Reutilizados cabecera, Malla, búsqueda, semestres, botones y Checkbox Material 3.
  Conservados variables locales, tipografía y color principal del archivo.
- Se inspeccionaron las seis exportaciones. Se corrigieron el campo con consulta
  escrita y mensajes que explicaban detalles técnicos en vez de la acción del
  estudiante. Acciones fijas sin solaparse con la navegación del sistema; paneles
  con contenido desplazable y acciones independientes.
- El borrador solo se promete durante la revisión activa; su recuperación ante
  recreación y aislamiento por UID se concretarán y comprobarán en D/E. No se
  afirma persistencia tras matar el proceso.
- Los ejemplos de 39/40 y de contradicciones son escenarios ilustrativos con las
  condiciones escritas. No son consultas a la cuenta del estudiante ni afirman
  que un fragmento de historial sea coherente globalmente.
- Sin código Android nuevo, compilaciones, JVM, pruebas UI, emuladores Firestore,
  operaciones Firebase, cambios personales, AVD, commit o push.
- Catálogo/reglas publicados y lectura real corresponden a la evidencia histórica.
  **Prueba personal real de guardado/reapertura/restauración pendiente de acuerdo
  específico**, no autorizada por aprobar estos mockups.

## Cierre para revisión

**Terminado:** diseño estático B2, exportaciones e índice del recorrido.
**Aprobado:** B1 solamente. **Pendiente de aprobación:** B2 completo; H17 condicional.
**Pendiente de SAE:** clasificación real bimodular y/o contacto.
**Sin implementar:** nuevas reglas de coherencia, preparación persistida, sus
interfaces Android y Recomendaciones. Tras revisión/aprobación de B2, la siguiente
entrega técnica prevista es C: reglas Kotlin puras con diagnósticos y pruebas,
sin escrituras remotas. No iniciar C–G automáticamente al cerrar este diseño.
