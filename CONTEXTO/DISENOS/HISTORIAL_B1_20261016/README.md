> Referencia visual histórica. Para implementación y pendientes leer [ESTADO_ACTUAL.md](../../ESTADO_ACTUAL.md). Las instrucciones fechadas siguientes se conservan como registro, no como tareas vigentes.

# Historial · entrega visual B1

16/10/2026 · America/La_Paz. **B1 aprobada expresamente por el estudiante:**
«a ok apruebo esta primera tanda que mas seguiria entonces».
La aprobación comprende las ocho vistas H01–H08. No inicia la implementación
Android ni aprueba los estados B2 todavía pendientes. Las exportaciones y los
tableros conservan la leyenda de propuesta con la que fueron presentados;
este registro posterior acredita su aprobación.
Unidad A revisada; unidad B iniciada en una primera tanda de ocho vistas. No se
implementó Android, dominio, persistencia ni reglas nuevas. La aprobación del
15/10 no incluye estas propuestas.

## Revisión de la base (unidad A)

Inspección de los archivos locales reales, conservando todos sus cambios:

| Archivo bajo el proyecto Android | Resultado y consecuencia para el plan |
|---|---|
| `presentation/navigation/AppNavigation.kt` | Perfil completo entra directamente a Malla. La futura preparación deberá insertarse aquí tras una lectura correcta por cuenta, sin repetir el alta de Perfil. |
| `presentation/viewmodel/AcademicViewModel.kt` | Loading/Ready/Error/SessionExpired y editor individual. No hay preparación ni borrador conjunto; no reutilizar ese editor como si ya resolviera una selección múltiple. |
| `data/FirestoreAcademicRepository.kt` | Source.SERVER, rechazo de caché/pendientes, validación de sesión y perfil; saveState confirma y relee estados. No valida prerrequisitos al escribir ni guarda un conjunto. |
| `domain/academic/ReglasAcademicas.kt` | Requisitos conjuntos y umbral existentes. No hay diagnósticos de coherencia/transiciones. El total actual incluye la propia materia si está aprobada: la validación futura deberá excluirla para comprobar su umbral histórico lógico, sin inferir fechas. |
| `firebase/firestore.rules` | Protege propietario/perfil/plan/esquema. Admite EN_CURSO/APROBADA sin garantizar coherencia. Ruta de preparación todavía sin contrato implementado; no añadir campos a Perfil. |
| `firebase/catalogo/sistemas-rm-615-2016.json` | Fuente de códigos/nombres de los ejemplos: primer semestre, BMS-301 y SIS-306 con SCC-305/SIS-305. Sin modificaciones ni clasificación de duración. |

Las diferencias entre la base y el plan son trabajo previsto, no una regresión
descubierta ni una autorización para implementarlo ahora. Se conserva la política:
selección de aprobadas sin orden, validación del conjunto, EN_CURSO con todos los
requisitos, correcciones que reduzcan contradicciones, sin aprobaciones automáticas
y sin agravar dependencias al retirar aprobaciones. Confirmar/recomendar exige
coherencia completa, incluidos umbrales y exclusión de la propia materia.

Evidencia remota histórica consultada: informe de integración (informe histórico conservado localmente por el autor).
Catálogo y reglas publicados y lectura real comprobada según ese informe. Esta
entrega no hizo consultas ni escrituras Firebase ni repitió sus pruebas.
**Guardado personal real, reapertura y restauración siguen pendientes de un acuerdo
específico de materia/transición. BMS-300 no está autorizada para esa prueba.**

## Mockups y exportaciones

Mismo archivo y página existentes. Los dos tableros nuevos están a la derecha,
en x=4400; no se reemplazó ninguna pantalla aprobada. Se reutilizaron cabecera,
búsqueda, semestres, Malla y botones existentes e instancias Checkbox Material 3.

- [11A · Preparación en Figma](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=40-697) · [PNG](01_PREPARACION.png).
- [11B · Revisión y Malla en Figma](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=41-625) · [PNG](02_REVISION_Y_MALLA.png).

| Vista | Nodo Figma | Qué se revisa |
|---|---|---|
| H01 Bienvenida | [40:703](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=40-703) | Empezar, registrar aprobadas previas o posponer; ninguna entrada borra estados. |
| H02 Aprobadas / cero | [40:1050](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=40-1050) | Búsqueda, semestre, selección individual y contador cero; continuar sin inventar aprobaciones. |
| H03 Aprobadas / varias | [40:1451](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=40-1451) | Ejemplo con cinco aprobadas, conservación de selecciones existentes y edición sin orden obligatorio. |
| H04 En curso / opcional | [40:1863](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=40-1863) | Ejemplo BMS-301 habilitada; ejemplo separado SIS-306 bloqueada por ambos requisitos; posibilidad de continuar sin en curso. |
| H05 Revisión / cero | [41:631](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=41-631) | Confirmación explícita de cero aprobadas tras lectura correcta. |
| H06 Revisión / varias | [41:981](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=41-981) | Cinco aprobadas + BMS-301 en curso = 44 pendientes; resumen y condición de confirmación/relectura. |
| H07 Contradicciones | [41:1337](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=41-1337) | Ejemplo localizado SIS-306 aprobada sin ambos requisitos; añadir omisiones o corregir estado sin cascada. No afirma que sea la única contradicción de un historial completo. |
| H08 Malla / pospuesto | [41:1692](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=41-1692) | Malla conserva consulta y muestra acción para retomar; posponer no significa cero aprobadas. |

## Recorrido propuesto

Son mockups estáticos, sin prototipo conectado ni consultas/guardados ejecutables.

1. Perfil completo → lectura correcta de preparación y estados existentes → H01
   solo cuando corresponda. Una lectura fallida nunca entra como historial vacío.
2. Empezar → H02 con estados realmente leídos. Si la cuenta ya tiene estados,
   deben aparecer marcados como en H03; no vaciarla por la opción elegida.
3. Historial previo → H03. La vista muestra el primer semestre; el selector recorre
   los nueve y la búsqueda abarca el catálogo. El mockup de selección contiene las
   cinco filas del semestre visible, no un catálogo completo duplicado.
4. Selección → en curso opcional H04 → revisión H05 o H06 según el conjunto.
   H04 muestra la variante de una materia en curso; la variante visual de cero en
   curso todavía pertenece a B2. Omitir esa selección no conservará una selección
   nueva de este paso; los estados previamente guardados siempre requieren revisión
   explícita antes de cambiarlos.
5. Contradicciones → H07 → volver a las materias afectadas, corregir el borrador y
   comprobar el conjunto de nuevo. Se puede registrar un requisito omitido sin
   borrar la aprobación real de la dependiente.
6. Confirmar → guardado/confirmación/relectura → resumen final (estados B2 pendientes).
   El botón del mockup no significa éxito ya verificado ni fija el contrato técnico.
7. Posponer → H08; retomar → preparación con datos existentes. Decisión por UID
   prevista en el plan, todavía sin implementar. H08 reutiliza la Malla de ejemplo
   aprobada anteriormente; sus selecciones no son la continuación de H06.

La primera aprobación solicitada es únicamente sobre B1. No habilita Android
ni da por aprobados los elementos de B2 todavía no dibujados.

## Alcance B2 definido al cerrar B1

**Actualización posterior:** estos estados ya están diseñados en
[B2, pendiente de aprobación](../HISTORIAL_B2_20261016/README.md). La lista inferior
conserva el alcance acordado para esa segunda entrega.

- Cargando y error de lectura; variante sin materias en curso; búsqueda vacía.
- Guardando, espera, error con borrador conservado, resumen final confirmado.
- Avance parcial recuperable si el contrato D lo requiere, concurrencia, descarte
  y sesión expirada. No prometer borrador persistente tras cierre del proceso.
- Paneles de Malla: bloqueo de EN_CURSO, retirada de aprobación utilizada,
  corrección conjunta, umbral 39/40 y ayuda para volver a PENDIENTE.
- Entrada futura de Recomendaciones que dirige a completar/revisar historial.
- Detalle «Bimodular · 2 meses», como ejemplo explícitamente ilustrativo sin
  asociarlo a una materia real no confirmada. SAE sigue sin respuesta sobre lista
  o contacto. La duración no cambiará estados, habilitación, flujos ni puntajes.

## Verificación de esta entrega

- Ocho vistas de 412 × 892; tableros de 1816 × 1060 aproximadamente.
- Comprobación estructural final: 290 y 452 descendientes; TEXT, FRAME, VECTOR,
  RECTANGLE e INSTANCE; cero nodos con relleno de imagen. Interfaz editable.
- Fuente comprobada en ambos tableros: únicamente Roboto. Variables locales
  primary/ink/muted/white/border conservadas; selección Checkbox ajustada al rojo.
- Exportaciones inspeccionadas: corregidos altura de texto, selección de semestre,
  posición inferior del sistema y acciones fijas. Las listas largas se desplazan;
  el encuadre inicial puede mostrar parcialmente la siguiente fila.
- Sin compilación ni repetición de JVM/UI/reglas: no cambió código Android.
- Sin commit/push, operación Firebase, datos personales ni interacción con AVD.

B2 ya está preparada como revisión visual separada y pendiente de aprobación.
B1 sigue aprobada; C–G no se ejecutaron y las interfaces Android nuevas todavía
no se desarrollan.
