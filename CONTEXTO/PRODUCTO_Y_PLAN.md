# RoadMap UTEPSA: producto y plan

Documento vigente, actualizado el 18/10/2026 para T2. El estado de implementación y las pruebas están en [ESTADO_ACTUAL.md](ESTADO_ACTUAL.md). Los documentos de ARCHIVO son registros históricos: sus pendientes, pausas y autorizaciones describen sesiones anteriores.

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
| Avance | Conteos por estado y porcentaje `aprobadas / 50 × 100`, calculados. En curso no suma como aprobada. Resumen es una pantalla futura. |
| Oferta SAE | Una sola oferta actual editable por cuenta. Selección manual por nombre/sigla; guardar reemplaza la selección. Distinguir ausente de guardada vacía. Sin periodos ni horarios. |
| Recomendaciones | Prioridad por desbloqueos inmediatos, con explicación; cálculo reproducible descrito abajo. |
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

## Recomendador pendiente

1. Usar una lectura completa y coherente del historial y la oferta actual. Si falta preparación o hay contradicciones, orientar a corregirlas antes de recomendar.
2. Candidatas: ofertadas, pendientes y habilitadas por todos los requisitos y el umbral.
3. Simular APROBADA para cada candidata por separado, sin persistir la simulación.
4. Contar otras materias pendientes que pasan de bloqueadas a habilitadas inmediatamente. Incluir umbrales; excluir las ya habilitadas y los desbloqueos en cadena. Las desbloqueadas no necesitan estar ofertadas.
5. Ordenar por desbloqueos descendente, semestre ascendente y código ascendente.
6. Explicar elegibilidad y materias desbloqueadas. Distinguir oferta ausente, oferta guardada vacía y ausencia de candidatas. No prometer optimización global ni calendario.

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

Orden sugerido después de estabilizar el flujo actual: Resumen; Oferta actual; Recomendador; Cuenta/proveedores/correo/biometría según prioridad del estudiante. La clasificación de duración ya cuenta con fuente SAE; verificar el estado de publicación en ESTADO_ACTUAL.md. No interpretar esta lista como autorización para implementar todas las unidades en una revisión de errores.
