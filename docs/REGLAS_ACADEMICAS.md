# Reglas académicas y persistencia

## Plan y estados

El MVP contempla Ingeniería de Sistemas, plan RM 615/2016: 50 materias, 9 semestres y 28 relaciones de prerrequisito según el [pénsum SAE](../CONTEXTO/Pensum%20Ingenieria%20de%20Sistemas.pdf). El [catálogo validado](../firebase/catalogo/README.md) conserva las identidades UUID. La aplicación obtiene el catálogo desde Firestore.

Los estados son `PENDIENTE`, `EN_CURSO` y `APROBADA`. Solo una aprobación satisface un requisito y suma al porcentaje de avance: `aprobadas / 50 × 100`. La ausencia de un documento de estado significa pendiente después de una lectura completa correcta. Volver a pendiente elimina el documento.

Marcar una materia en curso exige que esté habilitada. La corrección de aprobaciones no puede introducir faltantes nuevos en otras materias activas. No se cambian requisitos o materias dependientes automáticamente. Taller de Graduación exige 45 materias aprobadas distintas de sí mismo; Proyecto de Grado II exige sus dos prerrequisitos aprobados.

El semestre es curricular y no determina por sí solo la habilitación. Modular, Bimodular y Duración especial son etiquetas de duración; no se usan para inferir horarios ni prioridades.

## Oferta y recomendaciones

Cada cuenta conserva una sola oferta actual ingresada manualmente. Una oferta ausente se distingue de una guardada vacía. Al editar se excluyen las materias aprobadas; las pendientes y en curso siguen disponibles. Elegir una materia en Oferta no cambia su estado académico.

El recomendador considera las ofertadas que estén pendientes y habilitadas. Prioriza pendientes de semestres anteriores al más alto con alguna materia aprobada o en curso. Después ordena por desbloqueos completos, objetivos con avance parcial, semestre y código. Simula cada aprobación de forma independiente; no permite cursar simultáneamente una materia bloqueada y su requisito.

La pantalla explica la opción principal, una secundaria cuando tiene beneficio y las alternativas restantes. Puede mostrar una oferta sin candidatas. Un error de lectura se muestra como error, nunca como una lista vacía inferida.

Implementación: `domain/academic/ReglasAcademicas.kt`, `ReglasHistorial.kt` y `domain/recommendation/MotorRecomendaciones.kt`.

## Identidad y almacenamiento

El UID Firebase identifica cada cuenta. Vincular Google y contraseña conserva el UID y los datos; no fusiona cuentas por coincidencia de correo. El acceso por contraseña requiere verificación. El perfil completo precede al contenido académico.

| Ruta Firestore | Contenido |
|---|---|
| `materias/{id}` y `prerrequisitos` | Catálogo y relaciones. |
| `usuarios/{uid}`, `personas/{uid}`, `estudiantes/{uid}` | Identidad de usuario, perfil y plan asignado. |
| `estudiantes/{uid}/estados/{idMateria}` | Estado académico declarado. |
| `estudiantes/{uid}/configuracion_academica/historial` | Decisión de preparación del historial. |
| `estudiantes/{uid}/operaciones_historial/actual` | Diario recuperable de guardado del historial. |
| `estudiantes/{uid}/control_academico/revision` | Revisión global para detectar cambios concurrentes. |
| `estudiantes/{uid}/ofertas/actual` | Oferta única con revisión propia. |

Las escrituras académicas comparan la revisión y los estados esperados; los conflictos conservan el borrador. La oferta se reemplaza mediante transacción. Los resultados inciertos se reconcilian con una lectura del servidor antes de repetir o anunciar éxito. Cerrar o cambiar sesión retira los datos de pantalla y descarta respuestas de la sesión anterior.

La biometría desbloquea una sesión existente y es opcional por cuenta/dispositivo. Las preferencias de bloqueo y avisos se guardan en DataStore, excluidas de respaldo y transferencia. Las reglas de autorización están en [firestore.rules](../firebase/firestore.rules).
