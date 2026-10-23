# Estado actual de RoadMap UTEPSA

## Ajustes de interfaz del 23 de octubre de 2026

El estudiante confirmó que los ajustes quedaron bien y autorizó subirlos al
repositorio. Es conformidad reportada; no añade una matriz física ni capturas
Android nuevas. Publicación preparada en `fix/cursando-amarillo-salida-perfil`.

Oferta también muestra fondo amarillo y etiqueta «Cursando» para EN_CURSO,
tanto en la oferta guardada como en el selector; usa los estados académicos
ya leídos, con independencia de si la casilla está seleccionada.

EN_CURSO usa fondo amarillo pastel `#FFF4CC` en tarjetas de Malla, consulta,
selección y revisión de Historial, y contador Cursando en Resumen; el acento oscuro
es `#735C00`. Conserva etiquetas y símbolo de estado. La salida habitual se
concentra en Cuenta; se eliminó de las cuatro pantallas académicas y de edición
de Perfil. El alta inicial mantiene su salida. Por confirmación del estudiante,
verificación de correo y bloqueo por huella conservan las salidas de acceso.
La confirmación de Cuenta conserva la advertencia de borradores de Oferta.
Cambios locales en `fix/cursando-amarillo-salida-perfil`; ver pruebas y límites en
[VALIDACION.md](VALIDACION.md). El amarillo ya está implementado y cuenta con conformidad del estudiante; faltan capturas definitivas. Los cambios documentales previos se conservaron.




Actualizado el 19/10/2026, hora de Bolivia. Entrega de Cuenta, avisos,
reorganización y lenguaje integrada en `main` desde `0192056`, con conformidad
final del estudiante y autorización de publicación. Los contratos están
en [PRODUCTO_Y_PLAN.md](PRODUCTO_Y_PLAN.md) y los resultados de pruebas únicamente
en [VALIDACION.md](VALIDACION.md).

## Implementación

| Componente | Estado vigente |
|---|---|
| Acceso Google y sesión | Firebase Auth y Credential Manager; aislamiento por UID/versión de sesión y mensajes controlados ante errores/cancelación. |
| Perfil | Lectura, alta y edición reales en Firestore, validación y conservación del plan. |
| Catálogo SAE | Publicado y relectura verificada: 50 materias, 28 relaciones, Taller con 45 aprobadas; etiquetas Modular/Bimodular/Duración especial. |
| Historial | Primera preparación, consulta, edición precargada, diferencias, confirmación, posposición inicial, diario recuperable y conflictos. |
| Malla | Búsqueda/filtros/semestres, requisitos, estados y guardado protegido por CAS/revisión global. |
| Resumen / T1 | Publicado e integrado con las otras tres pestañas. Conteos derivados, porcentaje solo de aprobadas y avisos de historial. Comparte AcademicViewModel con Malla. |
| Oferta SAE / T2 | Una oferta actual por cuenta, editor sin aprobadas, reemplazo transaccional, conflictos y recuperación de resultado incierto. |
| Para ti / T3-T4 | Motor Kotlin puro, consulta estable y pantalla con principal/secundaria/opcionales, desbloqueos completos y avances parciales. |
| Notificaciones | Saludo eliminado. Avisos locales de Historial/Oferta, opt-in por UID/dispositivo en Cuenta, canal silencioso, deduplicación DataStore y ruta interna protegida. FCM conserva recepción; push remoto aún no acreditado. |
| Cuenta y acceso completo / T5 | Integrada desde `feat/cuenta-autenticacion`; reglas/proveedor publicados previamente. Aceptación manual satisfactoria de registro, recuperación, flujo académico, ambos sentidos de vinculación, acceso con ambos métodos, verificación de correo y biometría con cancelación/segundo plano/bloqueo de pantalla. |

Oferta se integró mediante el [PR #1](https://github.com/Sebasss04/RoadMapUtepsa/pull/1)
en `2f21871`; Para ti mediante el [PR #2](https://github.com/Sebasss04/RoadMapUtepsa/pull/2)
en `30685a6`. Resumen está en `41ef94a` y el cierre documental publicado en
`0192056`. Ambas ramas de funciones están conservadas, pero sus integraciones ya
terminaron. El núcleo académico fue contrastado con el plan vigente sin detectar
discrepancias funcionales en los bloques revisados; eso no certifica ausencia total de errores.

## Arquitectura e identidades

`RoadMapApplication → di/AppContainer → repositorios → ViewModels → Compose`.
El dominio completo es independiente de Android/Firebase y no contiene textos de interfaz.
Contratos y modelos están en `domain`, adaptadores y persistencia en `data`,
pantallas/estados/ViewModels juntos por función en `presentation` e integraciones
Android en `platform`. El [mapa del README](../README.md#mapa-del-código) detalla
los paquetes y las reglas. No existe una carpeta global de ViewModels ni clases puente.
Mantener paquete `com.example.roadmap_utepsa`, UID Firebase, UUID de materias y
claves de ViewModel por UID/versión de sesión.

AppNavigation compone acceso y AcademicFlowNavigation. Se comprueba autenticación,
verificación de correo, desbloqueo local y perfil completo; HistorialGate controla
la preparación. Con decisión guardada y sin operación activa entra a Malla.
Las cuatro pestañas tienen un destino exclusivo; conservan filtros/búsqueda/semestre
de Malla y borrador/búsqueda/revisión esperada de Oferta. Para ti relee al entrar
y al volver de Perfil. Los cuatro iconos abren Cuenta; editar, guardar o cancelar Perfil vuelve a Cuenta.
Volver desde Cuenta conserva el destino de origen, filtros y borrador de Oferta. Cambiar/cerrar sesión retira datos
privados y descarta resultados tardíos. Los avisos de borrador se conservan.

Malla y Resumen mantienen un único AcademicViewModel. El filtro común es puro;
las evaluaciones académicas y del borrador de comparación se preparan con el estado
de presentación. Compose conserva filtros, expansión y selección visual.
Las preferencias de bloqueo/avisos se separaron de sus controladores y de la entrega
Android. Los errores de autenticación se traducen en los adaptadores; la interfaz
resuelve recursos por función. EMAIL_EXISTS no afirma que exista otra cuenta.
La reorganización conserva los cambios locales de Cuenta/avisos y no modifica reglas,
algoritmo de recomendaciones, esquema Firebase ni mecanismos de concurrencia.
La verificación final y la revisión de capturas están registradas en la primera
sección de VALIDACION; se preservan los intentos fallidos y la compilación
interrumpida como evidencia. El estudiante dio su conformidad final para publicar;
no aportó nueva matriz física ni hash de APK en esa confirmación.

## Persistencia y concurrencia

Firebase: `roadmap-utepsa-c9409`, Firestore `(default)`.

| Ruta | Contenido |
|---|---|
| `materias/{id}` y subcolección `prerrequisitos` | Catálogo remoto filtrado por plan. |
| `usuarios/{uid}` | Registro de usuario y fecha de creación. |
| `personas/{uid}` | Nombres y apellidos del perfil. |
| `estudiantes/{uid}` | Plan académico asignado. |
| `estudiantes/{uid}/estados/{idMateria}` | EN_CURSO/APROBADA; volver a pendiente elimina el documento. |
| `estudiantes/{uid}/configuracion_academica/historial` | Decisión, formato y timestamp. |
| `estudiantes/{uid}/operaciones_historial/actual` | Diario, huellas de base/objetivo y cambios aplicados. |
| `estudiantes/{uid}/control_academico/revision` | Versión de formato y contador monotónico. |
| `estudiantes/{uid}/ofertas/actual` | Oferta única con revisión propia, independiente de la revisión académica. |

Los escritores de Malla/Historial incrementan la revisión académica en el mismo
commit, incluidos futuros escritores administrativos. Malla compara el conjunto
completo de estados, incluidas ausencias. El asistente aplica grupos de hasta cinco;
el último confirma marcador y diario atómicamente. Las lecturas estables comparan
revisión antes/después y las transacciones vuelven a comprobarla. Mantener CAS,
conflictos, diario, cancelación y relectura de servidor antes de anunciar éxito.

El catálogo validado se reutiliza en memoria hasta cinco minutos por sesión/plan;
perfil y estados se releen de servidor. No hay Room ni catálogo alternativo de
producción en la APK. El lector acepta el esquema anterior completo y SAE completo,
rechazando mezclas. Producción ya tiene SAE: no repetir su migración.

## Dispositivo y riesgos pendientes

La APK de la base integrada está instalada en el V2529 mediante actualización
sin borrar datos. El estudiante confirmó uso y pruebas manuales satisfactorios;
no enumeró cada escenario. Instalación, hash y validaciones automatizadas en VALIDACION.md.
El 19/10 se actualizó además la APK local de Cuenta con `--no-streaming -r`,
resultado Success y apertura correcta. El estudiante reportó funcionamiento
satisfactorio hasta ahora con huella configurada; no es una matriz exhaustiva
de aceptación exhaustiva. Después de habilitar correo, el estudiante reportó alta
con correo/contraseña, historial, Oferta, Resumen, recuperación y vinculación de
Google satisfactorios, conservando progreso. Alcance y comprobaciones pendientes
en VALIDACION.md; no se inspeccionaron cuentas personales por automatización.

Las 39 advertencias originales de lint se resolvieron (debug/release sin incidencias);
se conserva el diagnóstico abierto de ART/system_server
del AVD personal Pixel_7 API35 de 16 KB. Los AVD sintéticos de 4 KB y el teléfono
no prueban que ese AVD esté reparado. Conservar su userdata y no usar pruebas
personales para fabricar benchmarks. Medir un guardado real solo cuando sea
necesario para el estudiante.

## Cuenta: entrega integrada

La entrega anterior de Cuenta sustituyó la planificación futura: se implementaron los cuatro
bloques en `feat/cuenta-autenticacion`, conservando las modificaciones documentales
previas. Cuenta usa el perfil guardado, correo real y proveedores Firebase.
El alta inicial de Perfil permanece separada y obligatoria.

Registro crea una identidad una sola vez y después envía verificación; su fallo
permite reenviar sobre el mismo UID. Reenvío con reloj monotónico y espera de 60 s.
«Ya verifiqué» recarga usuario y fuerza token antes de publicar la identidad.
No hay contraseña en estado restaurable, preferencias, Firestore ni logs.

La vinculación reautentica con un proveedor existente y usa linkWithCredential.
Google debe coincidir con el correo actual; no hay fusión ni cambio de UID.
Los resultados inciertos exigen recargar proveedores antes de repetir.

SessionLock es independiente de los ViewModels académicos. La preferencia
DataStore es por UID/dispositivo y se excluye de respaldo y transferencia.
FragmentActivity muestra BIOMETRIC_STRONG sin PIN. Solo éxito activa la opción;
rotación y selector Google coordinado no duplican bloqueo. Un diálogo modal
seguro cubre navegación y borradores al bloquear; FLAG_SECURE protege recientes.

Se corrigió además una carrera reproducida en JVM: salir al segundo plano durante
la carga de DataStore revoca la entrada interactiva. Al llegar una preferencia
habilitada, la sesión queda bloqueada; no se pierde la excepción de rotación/Google.
Esta corrección también se instaló por actualización en V2529; el reporte manual
de huella corresponde a la APK previa, con el mismo flujo visible.

Las reglas locales restringen datos privados a Google o contraseña verificada.
CAS, revisión, diario y Oferta conservan sus reglas. Se verificó que solo cambia
isOwner frente al remoto y pasó dry-run. El 19/10 el estudiante autorizó publicar
reglas y habilitar correo: ambos pasos se ejecutaron y verificaron por relectura.
Se aplicó exactamente `firebase/account-auth-config.proposed.json`; plantillas,
privacidad y el resto de configuración Auth se conservaron. No se modificaron
cuentas ni documentos personales. Evidencia y hash remoto en VALIDACION.md.

La vinculación Google→contraseña falla en demo: SDK devuelve
EMAIL_EXISTS, reproducido nuevamente por REST local. El emulador 15.34.0 comprueba
correo existente antes de resolver el idToken; Google sigue recuperando el mismo
UID y la contraseña no queda añadida. En producción el estudiante confirmó ambos
sentidos y nuevos accesos por ambos métodos conservando datos, sin fallos.
Conservar el fallo demo como limitación local documentada; se mantiene linkWithCredential,
sin fusionar cuentas. Las dependencias se actualizaron después por solicitud nueva; diagnóstico previo y
revisión del paso remoto en VALIDACION.md. El mensaje EMAIL_EXISTS ya no afirma
que exista otra identidad, algo que ese código no demuestra en este caso.

Figma mantiene los diseños existentes y añade los estados de verificación,
reautenticación y resultado incierto en 09B (`73:813`); ayuda mínima de ocho
caracteres actualizada. Resultados y límites de pruebas en VALIDACION.md.

## Antecedente de Cuenta antes de la integración

Se retomó la pausa anterior el 19/10; el V2529 volvió a estar conectado y recibió
la APK de Cuenta sin borrar datos. Después de la autorización explícita se publicó
la configuración remota de Cuenta. En aquella etapa no se hizo commit/push ni se creó PR; el código seguía local en
`feat/cuenta-autenticacion`; conservar la consolidación documental preexistente.

Base anterior de Cuenta: 283 JVM correctos, ambas APK compiladas y lint 0/39. La regresión
UI final dio 68/68 con la APK corregida; acredita la recarga exacta de Para ti y
conservación de navegación/borradores. Se estabilizó el harness ante teclado y
filas lazy sin retirar aserciones funcionales. Intentos previos y límites en VALIDACION.md.

SDK demo: 6/7 correctos (Cuenta 3/4 más Oferta, Recomendaciones y posposición de
Historial). El caso Google→contraseña falla en la etapa link password con
EMAIL_EXISTS; también reproducido con REST local. Correo→Google sí conserva UID,
perfil y estados académicos tras acceder con ambos métodos. No ignorar la prueba
fallida ni cambiar contrato/dependencias sin resolver el diagnóstico.

El estudiante completó la verificación obligatoria explícita y los escenarios
de hardware indicados en la guía manual, sin anomalías; alcance en VALIDACION.
Alta, recuperación, ambos sentidos de vinculación y nuevos accesos por ambos
métodos conservando datos ya tienen reporte manual productivo satisfactorio. La activación
remota ya terminó con autorización y relectura; no repetirla. No
ejecutar pruebas personales ni borrar userdata de Pixel_7.

## Calidad y avisos: evidencia de la etapa anterior

El 19/10 el estudiante volvió a confirmar en dispositivo físico la vinculación
de contraseña a una cuenta creada únicamente con Google, seguida de cierre de
sesión y acceso correcto con ambos métodos, conservando los mismos datos.
El recorrido tiene aceptación manual productiva; el fallo EMAIL_EXISTS sigue
limitado a la evidencia demo disponible. No cambiar la vinculación para sortear
el emulador. Hash de esta ejecución y UID instrumental no registrados; alcance
exacto en VALIDACION.md.

Nueva solicitud del 19/10: limpieza → herramientas/dependencias por grupos → avisos
locales → regresión. Implementación local conservando Cuenta y todos los contratos
académicos. Diseño de Cuenta actualizado primero en Figma, con exportación versionada.
Versiones e inventario antes/después en VALIDACION.md. Estado automatizado de aquella etapa:
299 JVM correctos, APK debug/release y androidTest compiladas, lint debug/release 0/0.
Regresión Android final: 68 existentes y 19 comprobaciones nuevas aprobadas;
SDK actualizado 6/7, con EMAIL_EXISTS demo conservado. El estudiante ejecutó
todos los pasos de aceptación de avisos, verificación y biometría en su teléfono
y reportó funcionamiento esperado, sin anomalías. No queda pendiente esa matriz
manual. No se registró hash adicional de la APK utilizada en su ejecución.

El controlador PendingReminders reutiliza ConsultaRecomendacionesRepository;
reserva la deduplicación por UID antes de publicar, retira reservas si falla la
entrega y solo rearma tras observar una resolución estable. Oferta vacía es válida.
La ruta con UID esperado se recibe en onCreate/onNewIntent, se conserva hasta
superar acceso/verificación/biometría/perfil y se descarta si cambia la cuenta.
No reinicia los borradores de Oferta ni una edición de Historial existente.
Apagar cancela el visible conservando los marcadores; salir/cambiar sesión cancela
el aviso anterior. Sin horarios, backend ni nuevas colecciones.
