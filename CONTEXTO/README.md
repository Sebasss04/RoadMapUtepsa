# Contexto compartido de RoadMap UTEPSA

## Ajustes de interfaz del 23 de octubre de 2026

El estudiante confirmó que los ajustes quedaron bien y autorizó subirlos al
repositorio. Es conformidad reportada; no añade una matriz física ni capturas
Android nuevas. Publicación preparada en `fix/cursando-amarillo-salida-perfil`.

Ampliación del 23/10: Oferta también resalta EN_CURSO con el mismo amarillo, tanto
en la consulta guardada como en el selector, con etiqueta «Cursando».

Implementados localmente en `fix/cursando-amarillo-salida-perfil`: materias EN_CURSO
con amarillo pastel en Malla e Historial y contador Cursando en Resumen. La salida
habitual queda únicamente en Cuenta (icono de perfil), con confirmación y aviso de
borradores; se retiraron los botones de Malla, Oferta, Para ti, Resumen y edición
de perfil. El alta inicial de Perfil conserva su salida previa. El estudiante
confirmó conservar las salidas de verificación de correo y bloqueo por huella.
Resultados y límites nuevos en [VALIDACION.md](VALIDACION.md). Este apartado
sustituye el pendiente de implementar amarillo de la revisión del 21/10; siguen
pendientes las capturas definitivas del informe; la conformidad del estudiante está registrada arriba.
No se actualizó el Word ni se publicaron cambios remotos.




Actualizado el 19/10/2026, hora de Bolivia. Estas guías son la continuidad vigente
para ambos integrantes; no hace falta el historial de chats ni la carpeta local
del autor para continuar en otro equipo.

## Orden de lectura

1. [ESTADO_ACTUAL.md](ESTADO_ACTUAL.md): qué existe y qué falta.
2. [PRODUCTO_Y_PLAN.md](PRODUCTO_Y_PLAN.md): reglas y contratos que conservar.
3. [TRABAJO_EN_EQUIPO.md](TRABAJO_EN_EQUIPO.md): tareas y coordinación.
4. [VALIDACION.md](VALIDACION.md): comprobaciones y límites reales.

Cada archivo tiene una función distinta. Actualizar sus secciones; no añadir
relatos repetidos de cada sesión ni tratar aprobaciones antiguas como encargos nuevos.

## Punto de continuación

Entrega del 19/10/2026 integrada en `main` desde `0192056`, con Cuenta,
correo, verificación, recuperación, vinculación, biometría, avisos locales,
reorganización por función y revisión del lenguaje. Se conserva todo el avance
de Resumen, Malla, Historial, Oferta y Para ti. La rama de origen es
`feat/cuenta-autenticacion`; no hay que volver a incorporar Oferta/Para ti.

El estudiante confirmó «está todo ok» y autorizó subir el avance y actualizar
las guías. Esta conformidad cierra la revisión para publicar; no añade una
matriz física ni un hash de APK. Las pruebas físicas anteriores, incluidos ambos
sentidos de vinculación y acceso con ambos métodos, fueron satisfactorias.

Mapa de arquitectura en el README raíz; resultados, capturas y límites en la
primera sección de VALIDACION. El SDK demo conserva 6/7 por EMAIL_EXISTS en
Google→contraseña, sin contradecir el éxito físico reportado. Push remoto no
acreditado y fallo del AVD personal siguen como límites, sin bloquear esta entrega.
La PC limitada motiva priorizar el dispositivo físico para aceptación manual.

Reglas de Cuenta y proveedor de correo ya publicados y releídos con autorización
el 19/10. La integración del código no los vuelve a desplegar, no migra datos ni
instala APK en dispositivos. Las restricciones de commit/push de las etapas
anteriores fueron sustituidas por esta autorización de publicación; no autorizan
nuevos despliegues ni cambios funcionales adicionales.

## Fuentes

| Recurso | Uso |
|---|---|
| [Repositorio](https://github.com/Sebasss04/RoadMapUtepsa) | Código del producto. |
| [Figma](https://www.figma.com/design/YCcVIGVjbSLOKygieTbEOt?node-id=17-482) e [índice visual](DISENOS/INDICE_Y_ESPECIFICACIONES.md) | Diseños y exportaciones por pantalla. Un mockup no prueba implementación. |
| [Pénsum SAE](Pensum%20Ingenieria%20de%20Sistemas.pdf) | Fuente curricular vigente: 50 materias, 28 relaciones, Taller con umbral 45. |
| [Malla anterior](REFERENCIAS/ing.-de-sistemas-mallas-pensum-2.pdf) | Nombres completos y áreas; no sustituye requisitos SAE. |
| [Catálogo técnico](../firebase/catalogo/sistemas-rm-615-2016.json) y [contrato](../firebase/catalogo/README.md) | Especificación validada y baseline; producción lee Firestore. |
| [Reglas Firebase](../firebase/firestore.rules) | Acceso y esquema; no desplegarlas por el solo hecho de actualizar código. |
| [Arquitectura](REFERENCIAS/Arquitectura%20Roadmap%20Utepsa.mmd), [modelo conceptual](REFERENCIAS/Base%20de%20datos%20RoadMap%20Utepsa.mmd), [modelo Firestore](REFERENCIAS/Diagrama%20de%20BD%20Firestore.mmd) | Propuestas con funciones futuras; prevalecen contratos implementados y PRODUCTO_Y_PLAN.md. |
| [Ejemplo de la ingeniera](https://github.com/PatriciaAG/proyecto-final-autenticador) | Referencia didáctica, no código de este producto ni repositorio que modificar. |

Los README fechados de diseños son evidencia visual histórica. ARCHIVO, RESPALDOS
y tmp no definen las tareas vigentes. Los informes bajo `app/build/reports/` son
locales e ignorados por Git: otro clon no los recibe automáticamente.
Los detalles de sesiones anteriores permanecen en el historial Git de estas guías;
no se mantienen copias duplicadas como instrucciones activas.

## Continuar desde otro equipo

Clonar, abrir la raíz y leer AGENTS.md y las cuatro guías. Consultar el estado
antes de trabajar; conservar cambios existentes. Para actualizar una rama propia,
incorporar `origin/main` y resolver los puntos compartidos conservando todas las
pantallas. Ver [coordinación](TRABAJO_EN_EQUIPO.md).
