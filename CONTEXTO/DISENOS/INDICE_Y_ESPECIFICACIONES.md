# Referencias visuales RoadMap UTEPSA

El estado vigente está en [README del proyecto](../../README.md) y los requisitos en [reglas académicas](../../docs/REGLAS_ACADEMICAS.md). Esta carpeta conserva los diseños; un mockup no acredita implementación.

Historial B1 y B2 son referencias visuales en sus subcarpetas; sus README históricos no son planes de ejecución. Cuenta, Registro, Recuperación y Desbloqueo tienen implementación local en la rama de autenticación; la validación del MVP está en [docs/VALIDACION.md](../../docs/VALIDACION.md).

## Pantallas y exportaciones

| Pantalla | Figma | Exportación de estados | Contenido visual |
|---|---|---|---|
| Acceso | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=3-2407) | [PNG](01_ACCESO_ESTADOS.png) | Inicial aprobado; validación; iniciando sesión; error; Google cancelado. |
| Registro | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=5-33) | [PNG](02_REGISTRO_ESTADOS.png) | Inicial; correo y contraseñas inválidos; creando cuenta; error con entradas conservadas. |
| Recuperación | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=6-115) | [PNG](03_RECUPERACION_ESTADOS.png) | Formulario; solicitud enviada; error; enviando. Ayuda para Google. |
| Perfil | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=7-153) | [PNG](04_PERFIL_ESTADOS.png) | Alta inicial; edición; guardado; error de guardado; error de lectura; guardando. |
| Resumen | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=8-253) | [PNG](05_RESUMEN_ESTADOS.png) | Ejemplo de avance; primer ingreso; carga; error de lectura. |
| Malla | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=10-261) | [PNG](06_MALLA_ESTADOS.png) | Catálogo de 50 materias; panel de cambio de estado; panel de requisitos; búsqueda vacía; error; carga. |
| Oferta actual SAE | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=15-431) | [PNG](07_OFERTA_SAE_ESTADOS.png) | Lista guardada; editor; oferta ausente; guardada vacía; guardando; error al guardar. |
| Para ti / Recomendaciones, ajustes del 18/10/2026 antes de Compose | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=11-295) | [PNG vigente](08_PARA_TI_AJUSTES_20261018.png), [entrega inicial](08_PARA_TI_ESTADOS_20261018.png) | Principal verde, secundaria azul, opcionales grises; detalle de Calidad con 1/2 requisitos de Proyecto Integrador; avisos breves de pendientes anteriores. Conserva estados de carga, error, historial y oferta. |
| Cuenta | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=12-315) | [PNG](09_CUENTA_ESTADOS.png) | Cuenta; vincular Google; añadir contraseña; vinculado; conflicto; biometría no disponible; cierre; biometría activada. |
| Desbloqueo opcional | [Abrir](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=13-343) | [PNG](10_DESBLOQUEO_ESTADOS.png) | Sesión bloqueada; intento cancelado; biometría no disponible. |

El primer Acceso también se conserva como [exportación individual](01_ACCESO_PROPUESTA_A.png). Las exportaciones muestran el encuadre inicial de cada pantalla; los catálogos largos siguen dentro de áreas desplazables en Figma, con sus filas editables. Las vistas reducidas de la tabla se inspeccionan mejor ampliándolas en Figma.

## Cuenta: estados completados para esta entrega

[09B · Verificación y reautenticación](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=73-813)
y [exportación local](09B_CUENTA_VERIFICACION_REAUTENTICACION.png): correo pendiente,
envío, comprobación, error de envío, todavía pendiente, reautenticación por
contraseña/Google, vinculación en curso y resultado incierto. Capas editables,
componentes Material existentes, Roboto y variables del archivo. Se corrigió la
ayuda de Registro y Añadir contraseña a ocho caracteres. Cuenta representa nombres
persistidos y correo de la identidad en lugar de Andrea y datos ilustrativos.
Los SVG originales de retorno, correo y huella están en CUENTA_ASSETS; Compose
usa sus vectores locales. El interruptor usa el control interactivo Material.

## Avisos de pendientes · 19/10/2026

Cuenta existente actualizada antes de Compose: [Figma](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=12-315)
y [exportación](09_CUENTA_AVISOS_20261019.png). Tarjeta editable `77:855` bajo
biometría, interruptor inicialmente desactivado, texto de utilidad y tipografía/control
existentes. El contenido sigue dentro del área desplazable.

La [captura Android con fuente 1,5](09_CUENTA_AVISOS_FUENTE_15.png) corresponde a
repositorios sintéticos y la APK final de avisos. Se comprobó el texto completo y
el control dentro del desplazamiento; no es una captura de datos personales ni
una prueba de hardware biométrico.
