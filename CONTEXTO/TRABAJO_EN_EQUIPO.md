# Trabajo pendiente y coordinación

## Ajustes de interfaz del 23 de octubre de 2026

El estudiante confirmó que los ajustes quedaron bien y autorizó subirlos al
repositorio. Es conformidad reportada; no añade una matriz física ni capturas
Android nuevas. Publicación preparada en `fix/cursando-amarillo-salida-perfil`.

Ampliación solicitada: el amarillo EN_CURSO también está aplicado a Oferta
guardada y su selector, con etiqueta «Cursando». No modificar los estados
académicos al seleccionar o guardar Oferta.

Los dos ajustes solicitados están implementados localmente en la rama
`fix/cursando-amarillo-salida-perfil`: amarillo pastel EN_CURSO y salida habitual
solo en Cuenta. Se conservan las salidas de acceso acordadas y el alta inicial.
Se adaptaron las pruebas UI existentes a la salida desde Cuenta. No reponer los
botones retirados al integrar. Conservar los cambios documentales previos del
informe. Continuar con capturas definitivas; la conformidad del estudiante está registrada arriba;
resultados nuevos y límites en [VALIDACION.md](VALIDACION.md). La revisión del
21/10 que figura debajo conserva su fecha; el amarillo ya no está por implementar.




Actualizado el 19/10/2026, hora de Bolivia. Base compartida: `main`, `0192056`.
El [estado](ESTADO_ACTUAL.md), los [contratos](PRODUCTO_Y_PLAN.md) y la
[validación](VALIDACION.md) se mantienen en sus archivos; este documento contiene
solo reparto, próximos trabajos y reglas de integración.

## Entregas cerradas

| Entrega | Responsable | Resultado |
|---|---|---|
| T1 — Resumen | Sebastián | Publicado e integrado con las cuatro pestañas; conteos/porcentaje, avisos y pruebas. |
| T2 — Oferta SAE | Alexandre/Codex | Publicada e integrada, incluida exclusión de aprobadas; reglas habilitadas. |
| T3/T4 — Motor y pantalla Para ti | Alexandre/Codex | Publicados e integrados, incluidos avances parciales y jerarquía principal/secundaria/opcionales. |

Las pruebas manuales posteriores del estudiante fueron satisfactorias. No queda
pendiente recibir Resumen ni volver a fusionar Oferta/Para ti.

## Cierre de entrega del 19/10

Cuenta, correo, vinculación, biometría, avisos locales, calidad, reorganización
y revisión del lenguaje quedan integrados en main desde `0192056`, con
conformidad final del estudiante y autorización explícita para publicar. Se
conserva la rama de origen `feat/cuenta-autenticacion`. No quedan pantallas ni
funciones acordadas por implementar en esta entrega. Resultados en VALIDACION;
mapa de código en el README. La aceptación física previa fue satisfactoria.

La confirmación final no enumera nuevos casos ni identifica un hash de APK.
No repetir despliegues: reglas y proveedor de correo ya fueron publicados y
releídos con autorización. Publicar código no instala la app ni modifica datos.

## Límites y trabajo futuro

| Tema | Situación y continuación |
|---|---|
| SDK demo | 6/7 por EMAIL_EXISTS de Google→contraseña; éxito físico reportado. Conservar el diagnóstico sin cambiar la vinculación para sortear el emulador ni pedir repetir la aceptación física ya realizada. |
| AVD personal | Fallo ART/system_server no declarado resuelto. PC limitada; priorizar dispositivo físico y conservar userdata. No es un defecto nuevo demostrado de la app ni bloquea esta entrega. |
| Push remoto | Recepción externa no acreditada; avisos locales sí probados. Verificar solo si se acuerda ese alcance. |
| Seguridad y medición | Mantener revisión de reglas antes de distribución amplia. Medir guardados solo en condiciones comparables y sin borrar/recrear historial personal. |

## Cómo trabajar sin pisarse

1. Revisar `git status` y conservar cambios existentes antes de actualizar.
2. Para tareas nuevas partir de main actualizado y acordar una rama propia; no volver a integrar las funciones ya publicadas.
3. Mantener pantalla, recursos y pruebas propios cuando sea posible.
4. Resolver juntos cambios de AppNavigation, AppContainer, MainActivity y barras;
   no reemplazar archivos completos ni perder destinos existentes.
5. Mantener UID, revisión académica, CAS, diario, conflictos y relecturas finales.
6. Ejecutar verificaciones pertinentes, actualizar las guías y preparar PR para revisión.

Para incorporar avances a una rama propia, confirmar primero sus cambios locales
y después ejecutar desde esa rama:

```powershell
git fetch origin
git merge origin/main
```

No usar instrucciones antiguas para fusionar otra vez `feat/oferta-actual-sae` o
`feat/para-ti`; ambas ya están en main. Un push de código no instala aplicaciones,
publica reglas, migra catálogo ni autoriza escrituras en cuentas reales.

## Verificación segura

Comandos y entorno en [VALIDACION.md](VALIDACION.md). Usar datos sintéticos y
Auth/Firestore demo para integración. No ejecutar toda la instrumentación a ciegas:
`PersonalAcademicPersistenceTest` y `scripts/test-personal-history.ps1` escriben
estados reales y no forman parte de la validación rutinaria.

## Arquitectura que conservar

Dominio, datos, presentación y plataforma se agrupan por función; los ViewModels
están junto a sus pantallas. Mantener AcademicViewModel compartido, claves por
UID/versión, comprobaciones tardías, CAS/diario y relecturas. Los textos usan
recursos por función y los fallos de acceso categorías propias. La regresión,
los avisos y las capturas finales están documentados en VALIDACION, incluidos
los intentos fallidos y el ajuste de teclado/desplazamiento de la prueba de Perfil
sin quitar aserciones. No convertir el reporte de seguridad anterior en una
auditoría nueva ni el SDK 6/7 en 7/7 por el éxito manual productivo.
