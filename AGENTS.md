# Continuidad de RoadMap UTEPSA

Leer primero CONTEXTO/README.md, CONTEXTO/ESTADO_ACTUAL.md y
CONTEXTO/PRODUCTO_Y_PLAN.md. Consultar CONTEXTO/TRABAJO_EN_EQUIPO.md antes de
ampliar funciones. Toda la documentación necesaria está en el repositorio;
no depender de rutas de la computadora del autor ni de otros chats.

Actualizar esas guías con los cambios, pruebas y pendientes reales; no crear un
relevo distinto por cada chat. Los informes antiguos y los mockups no son una
lista vigente de implementación ni autorización para operaciones remotas.

Conservar paquete com.example.roadmap_utepsa, UUID de materias, UID, sesión y
contrato de revisión académica. No quitar CAS, conflictos, diario ni comprobación
final desde servidor. No añadir Room, catálogo de producción en APK, dependencias
o capas genéricas sin una necesidad acordada.

Seguir el mapa de paquetes por función del README: contratos/modelos/reglas en
domain; persistencia y adaptadores en data; pantallas/estados/ViewModels juntos
en presentation; integraciones Android en platform y construcción en di.
Dominio no importa Android/Firebase/presentación, datos no importa presentación
y presentación no importa Firebase ni implementaciones de datos. Mantener el
AcademicViewModel compartido entre Malla y Resumen. Texto visible en recursos;
dominio devuelve categorías y los adaptadores traducen errores del proveedor.
Verificar con scripts/check-architecture.py y los runners protegidos documentados.

Trabajar con ramas por tarea y revisar diferencias antes de integrar. Respetar
cambios locales ajenos. Ejecutar pruebas pertinentes y registrar límites reales.
No ejecutar la suite Android completa contra una cuenta personal: hay pruebas
que escriben datos productivos y requieren autorización específica. Usar cuentas
sintéticas y emuladores demo para integración rutinaria.

Un push de código no despliega Firebase. La petición de una función no autoriza
por sí sola migraciones, publicación de reglas ni escrituras personales. No tocar
el repositorio de la ingeniera; es solo referencia. Cambios visuales sustanciales
deben partir del Figma y del acuerdo con el estudiante.
