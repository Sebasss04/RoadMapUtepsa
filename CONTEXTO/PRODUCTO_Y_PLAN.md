# RoadMap UTEPSA: producto y plan

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


Documento vigente, actualizado el 18/10/2026 para Para ti (T3/T4). El estado de implementación y las pruebas están en [ESTADO_ACTUAL.md](ESTADO_ACTUAL.md). Los documentos de ARCHIVO son registros históricos: sus pendientes, pausas y autorizaciones describen sesiones anteriores.

## Propósito y alcance

Ayudar a estudiantes de Ingeniería de Sistemas de UTEPSA a registrar su situación académica, introducir manualmente su oferta SAE y elegir materias mediante recomendaciones explicadas. La aplicación orienta; no inscribe ni sincroniza con la universidad.

Primera versión: una carrera y un plan, RM 615/2016, 50 materias, 9 semestres curriculares y 28 relaciones de prerrequisito según el [pénsum SAE del 17/10/2026](Pensum%20Ingenieria%20de%20Sistemas.pdf). La [malla anterior](REFERENCIAS/ing.-de-sistemas-mallas-pensum-2.pdf) conserva evidencia de nombres completos, áreas y siglas recortadas en la impresión SAE. El catálogo técnico validado está en `../firebase/catalogo/sistemas-rm-615-2016.json`; Android lee Firestore, sin catálogo alternativo de producción dentro de la APK. Catálogo SAE v2 publicado y relectura verificada el 17/10/2026; se conservan las identidades y estados personales. La evidencia de despliegue está en ESTADO_ACTUAL.md.

## Funciones y decisiones que deben conservarse

| Función | Requisito |
|---|---|
| Acceso | Google y, en una entrega futura, correo/contraseña, registro y recuperación. |
| Identidad | UID Firebase como identidad estable; vincular proveedores debe conservarlo, sin fusionar cuentas por coincidencia de correo. |
| Perfil | Nombres, apellidos, carrera y plan asignados. Alta y edición comparten formulario. Perfil completo antes del contenido académico. Error de lectura no equivale a perfil ausente. |
| Historial | Primera preparación: bienvenida, aprobadas, en curso, revisión y confirmación; permite configurar después. Accesos posteriores: consulta → edición precargada → diferencias → guardar/cancelar. Consultar o cancelar nunca pospone una confirmación anterior. |
| Malla | Nombre principal; sigla secundaria; clasificación Modular/Bimodular/Duración especial, semestre, área, requisitos y estado. Búsqueda por nombre/sigla sin distinguir tildes o mayúsculas, filtros y edición. Semestre curricular no determina habilitación. |
| Avance | Conteos por estado y porcentaje `aprobadas / 50 × 100`, calculados. En curso no suma como aprobada. Resumen está implementado e integrado en esta copia local; usa el AcademicViewModel existente. |
| Oferta SAE | Una sola oferta actual editable por cuenta. Selección manual por nombre/sigla; guardar reemplaza la selección. Distinguir ausente de guardada vacía. Sin periodos ni horarios. |
| Recomendaciones | Primero pendientes anteriores candidatas; luego desbloqueos completos y avances parciales. Principal, secundaria con beneficio y opcionales sin favorita artificial. Cálculo reproducible descrito abajo. |
| Cuenta | Edición de perfil, proveedores y biometría opcional. Pantalla completa futura. Cerrar sesión retira contenido privado. |
| Biometría | Opcional por cuenta/dispositivo; desbloquea una sesión existente con el prompt Android. Permitir reintentar o cerrar sesión. No sustituye Firebase Auth. |

Los datos académicos son autodeclarados. No registrar notas, intentos ni una lista paralela de aprobadas. No inventar matrícula, teléfono, semestre personal obligatorio ni validación institucional.

## Reglas del historial

- Estados: PENDIENTE, EN_CURSO y APROBADA. Ausencia de documento equivale a pendiente únicamente tras una lectura completa exitosa. Volver a pendiente elimina el documento.
- Solo APROBADA satisface prerrequisitos o umbrales. SIS-306 requiere SIS-305 y SCC-305; ambos deben estar aprobados. STS-300 requiere 45 aprobadas; la propia materia no cuenta para verificar su umbral.
- La selección de aprobadas permite introducir el historial sin orden. Se muestran contradicciones y se exige un conjunto coherente antes de confirmar el asistente.
- EN_CURSO exige habilitación. Retirar una aprobación se bloquea si introduce faltantes o aumenta el déficit de otras materias activas. Permitir corregir incoherencias sin exigir arreglar otras no relacionadas.
- Nunca aprobar requisitos, cambiar dependientes ni avanzar estados automáticamente. Una materia finalizada sin aprobación vuelve manualmente a pendiente.
- Conflictos remotos conservan la selección y exigen revisión; no sobrescribir silenciosamente. La optimización implementa una revisión global que deben incrementar todos los escritores de estados, preparación y diario en el mismo commit. Las reglas compatibles fueron publicadas con autorización explícita el 17/10/2026; no eliminar las comparaciones para reducir la espera.
- Posponer solo corresponde a la primera preparación y jamás degrada un historial CONFIRMADO. No confirma ni guarda la selección del borrador. El borrador no enviado vive en memoria durante la sesión; el diario permite recuperar lo ya enviado tras una interrupción.

## Oferta actual SAE: contrato T2

Implementación en `feat/oferta-actual-sae`, independiente de T1. `OfertaRepository`,
`FirestoreOfertaRepository`, `OfertaViewModel` y `OfertaScreen` tienen archivos propios;
`AcademicData` y `AcademicRepository` conservan sus contratos. Se reutiliza el catálogo
remoto validado por el repositorio académico, sin un segundo catálogo en producción.

- Documento único `estudiantes/{uid}/ofertas/actual`: campos exactos
  `version_formato: 1`, `id_plan: sistemas-rm-615-2016`, `materias: [UUID]`
  (únicos, máximo 50), `revision` (entero positivo), `actualizado_en` (timestamp de servidor).
  Ausencia confirmada del documento significa no registrada; lista vacía significa guardada vacía.
  Un error de lectura conserva un estado de error, nunca simula ausencia.
- Este contrato sustituye para Oferta la propuesta de subcolecciones de los diagramas
  conceptuales: un único documento permite reemplazar toda la selección atómicamente,
  sin guardados parciales, periodos ni horarios.
- Crear compara ausencia y escribe revisión 1. Actualizar compara la revisión esperada
  dentro de una transacción y aumenta exactamente uno. Oferta no toca estados,
  preparación, diario ni `control_academico/revision`.
- Confirmar éxito requiere commit y relectura `Source.SERVER`, sin caché ni escrituras
  pendientes. Si el resultado es incierto, la recuperación relee antes de reenviar y
  reconcilia selección + siguiente revisión. No se modifica la selección incierta antes
  de comprobar ese resultado.
- Un conflicto conserva el borrador y muestra nombres/siglas de la selección remota.
  El estudiante puede cargarla o guardar explícitamente su borrador contra esa revisión;
  otro cambio concurrente produce un nuevo conflicto. Nunca se fuerza una sobreescritura.
- Por petición del estudiante tras probar T2, el selector y su búsqueda excluyen las
  materias APROBADA según el historial real. EN_CURSO y pendientes siguen disponibles,
  incluidas pendientes bloqueadas: no se añade un filtro de prerrequisitos a Oferta.
  `DatosOferta` transporta estados del repositorio académico sin cambiar AcademicData.
  Al volver desde Malla se releen los estados conservando borrador, búsqueda y revisión
  esperada de Oferta; si falla la lectura, se conserva el borrador y se exige reintentar.
  Si una oferta anterior incluye aprobadas, al editar se retiran solo del borrador y
  se avisa: Guardar aplica la nueva selección, Cancelar conserva el documento anterior.
  Antes de enviar un guardado se relee el historial; una nueva aprobación en el
  borrador detiene el envío para revisar y guardar otra vez. Un resultado incierto
  se reconcilia antes de modificar esa selección. La búsqueda conserva la normalización
  de Malla, nombre principal, sigla secundaria y duración.
- Borrador y búsqueda viven en el ViewModel identificado por UID/versión de sesión:
  se conservan en rotación y navegación de la misma sesión, sin persistir en disco.
  Cancelar no escribe; descartar una selección modificada exige confirmación.
  Cambiar/cerrar sesión retira datos y descarta respuestas tardías.
- Reglas locales: solo propietario autenticado (proveedor no anónimo), perfil completo,
  documento exacto, plan fijo, revisión y timestamp del servidor. Sin listados ni borrados.
  Lista permitida de 50 UUID, verificada contra JSON técnico por `offer-catalog.test.cjs`;
  es una validación de identidades del servidor, no contenido académico en la APK.

Las reglas de Oferta son un prototipo validado en demo que debe revisar el equipo
antes de una distribución amplia. Tras el error reportado en el teléfono, el
estudiante autorizó su publicación: ya están activas en producción, con relectura
remota verificada y sin cambios a documentos personales. Ver VALIDACION.md.
Publicar código por sí solo no despliega reglas, migra datos ni instala aplicaciones.

## Para ti — alcance vigente del 18/10/2026

Motor y pantalla corresponden al autor/Codex en `feat/para-ti`, por asignación
expresa del estudiante. Sustituye el plan anterior y la propuesta de T3 para el
compañero; él conserva T1 (Resumen). Resumen no bloquea esta entrega.

### Diseño terminado antes de Compose

Se actualizó la sección existente de [Recomendaciones](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=11-295),
con 13 estados editables revisados visualmente: principal, detalle, oferta ausente,
oferta vacía, ninguna candidata, error, carga, pendientes anteriores, alternativa
disponible, historial pendiente, incoherente, recuperación pendiente y detalle
con cero desbloqueos. Roboto, colores, botones e iconos proceden del diseño existente.
Los datos de las tarjetas están identificados como ejemplos ilustrativos.
[Evidencia conjunta](DISENOS/08_PARA_TI_ESTADOS_20261018.png).
Nodos nuevos: prioridad `63:800`, alternativa `63:869`, historial `63:938`,
incoherencia `63:988`, recuperación `63:1038`, detalle cero `65:811`.

### Cálculo y datos

- `MotorRecomendaciones` es Kotlin puro y reutiliza `ReglasAcademicas` y
  `ReglasHistorial`. Rechaza historias incoherentes y planes/IDs incompatibles.
- Candidatas: ofertadas, pendientes y habilitadas. Excluye aprobadas y en curso.
- Referencia: semestre más alto con alguna aprobada o en curso. Pendientes de
  semestres inferiores son anteriores; sin materias activas no se infieren atrasos.
  No se denomina semestre actual ni se calculan promedios.
- Si existen candidatas anteriores, tienen prioridad. Dentro de cada grupo se
  ordena por desbloqueos completos descendente, cantidad de objetivos con avance
  parcial descendente, semestre y código ascendente. Sin candidatas
  anteriores pero con pendientes anteriores, se explica la alternativa disponible.
- Cada aprobación se simula por separado. Solo cuentan otras pendientes que pasan
  de bloqueadas a habilitadas inmediatamente, incluso no ofertadas y umbrales.
  Excluye cadenas, activas, ya habilitadas y la candidata. Conserva opciones con cero.
- Avance parcial: la candidata es requisito directo de otra pendiente bloqueada y,
  tras simular su aprobación, la otra sigue bloqueada. Se exponen requisitos que
  quedarían aprobados, los que todavía faltan (EN_CURSO no es APROBADA) y umbral
  restante. No se duplica un objetivo como desbloqueo completo y parcial. Sumar una
  aprobación hacia un umbral sin relación directa no crea avances parciales.
- Principal: primera candidata. Secundaria: primera restante con desbloqueos o
  avances parciales; opcionales: todas las otras, sin duplicados. Sin beneficio
  restante se muestran solo opciones bajo «Si quieres cursar dos», sin favorita.
  Las simulaciones son independientes; no autorizan cursar una materia bloqueada
  al mismo tiempo que su requisito. No se calculan horarios ni carga académica.
- La explicación enumera requisitos aprobados, umbral satisfecho, desbloqueadas y avances parciales.
  La prioridad orienta; no expresa una prohibición oficial de inscripción.
- `ConsultaRecomendacionesRepository` consulta Preparación y Oferta guardada entre
  lecturas de revisión antes, entre ambas y después. Exige misma identidad de sesión,
  UID, plan y catálogo; repite la pareja hasta tres intentos si cambia revisión.
  Errores de lectura nunca equivalen a oferta ausente o vacía. No expone escrituras.
- ViewModel por UID/versión de sesión: exige historial confirmado y coherente y
  ninguna operación activa. Cancela cargas sustituidas y descarta respuestas tardías.
  Retira resultados durante recarga, al salir y al cambiar sesión.

### Pantalla y navegación

Revisión posterior del 18/10: principal verde (#EAF5EF/#21664A), secundaria azul
(#EAF2FF/#244B87), opcionales gris (#F7F8FA/#20232B), siempre con etiquetas.
Las tarjetas muestran nombre, sigla/duración/semestre, motivo breve y «Ver detalle».
Los requisitos y el orden quedan en el panel; los pendientes anteriores se despliegan
a petición. «Opcional» significa alternativa de esta oferta, no materia prescindible.
No se promete evitar atrasos. Diseño revisado primero en la sección existente de
Figma; [exportación del ajuste](DISENOS/08_PARA_TI_AJUSTES_20261018.png).

Compose usa el diseño actualizado, tema e iconos existentes; los cuatro SVG exportados
(flecha, advertencia y dos iconos de estado) conservan la geometría exportada desde Figma como vectores.
Barras de Malla y Oferta habilitan Para ti; la nueva pantalla permite volver a ambas,
revisar Historial, visitar Perfil y cerrar sesión con el aviso de borrador de Oferta.
Oferta conserva ViewModel, búsqueda, borrador y revisión esperada al navegar.
Solo la selección guardada participa en el cálculo. Entrar de nuevo o regresar de
Perfil vuelve a consultar; Historial y Malla mantienen sus flujos existentes.
T1 ya está conectado localmente a las cuatro pantallas sobre main `30685a6`, sin commit; no modifica el motor ni la lectura estable.

### Verificación de la entrega inicial

Los ajustes posteriores de avances parciales, categorías y contenido tienen su
propia sección vigente en VALIDACION.md. Los siguientes conteos corresponden a
la entrega inicial, antes de esas correcciones.

Diseño terminado y código implementado. Compilación APK principal/androidTest,
222 JVM (23 nuevas), 13 UI seleccionadas (4 nuevas y 9 de Oferta) y 1 SDK demo
correctas. Lint: cero errores y 39 advertencias. Revisar evidencia y límites del
AVD en VALIDACION.md. No se requieren despliegues de reglas, migraciones ni datos personales.

## Bimodulares y límites

SAE identifica 40 modulares (85 horas), 9 bimodulares (170 horas) y Taller de
Graduación con duración especial (400 horas). Las horas son datos técnicos del
catálogo; la interfaz muestra las etiquetas, sin convertirlas en meses ni usarlas
para prioridades, requisitos o calendarios. Bimodulares: BMS-300, SCC-300, SAL-301,
SIS-302, SRF-301, SIS-303, SIS-304, SDA-304 y SIS-306.

Prevalece SAE en requisitos: SDA-303 requiere SIS-302; SOI-304 requiere SOI-303;
SRA-304, SES-302 y SES-303 no tienen prerrequisito explícito en el nuevo documento.
SIS-306 conserva SIS-305 y SCC-305. Taller exige 45 aprobadas excluyéndose a sí mismo.
Se conservan UUID y estados personales; las contradicciones se explican y se corrigen
explícitamente sin cascadas ni cambios automáticos. Las opciones del bloque electivo
no se suman a las 50 materias obligatorias. No contactar a SAE por iniciativa propia.

Fuera de v1: otras carreras/planes/roles, horarios completos, historial de notas/intentos/ofertas, integración automática con SAE, scraping, importación de fotos o audio, Storage, administración institucional e IA generativa en el recomendador. Sin Room, Supabase ni capas genéricas adicionales sin necesidad.

## Diseño y orden de continuación

[Archivo Figma de interfaces](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=17-482). Exportaciones originales en [DISENOS](DISENOS/). Sus informes contienen estados históricos; no prueban qué está implementado hoy. Nuevas pantallas o cambios visuales sustanciales deben partir de los diseños y del acuerdo con el estudiante. Las correcciones de lógica no requieren rediseñar pantallas.

El PR #2 ya fue integrado en main mediante `30685a6`; no volver a fusionarlo.
La copia local incorpora T1 sin confirmar y requiere su validación de integración.
Registro del cierre anterior: revisar el [PR #2 de Para ti](https://github.com/Sebasss04/RoadMapUtepsa/pull/2),
publicado por autorización del 18/10/2026, e integrar Resumen cuando el compañero lo entregue, preservando
Oferta y Para ti. Cuenta/proveedores/correo/biometría requiere una petición propia
del estudiante. La clasificación de duración ya cuenta con fuente SAE; verificar
el estado de publicación en ESTADO_ACTUAL.md. No interpretar esta lista como
autorización para implementar todas las unidades en una revisión de errores.
