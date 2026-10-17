# Catálogo académico SAE

Contrato vigente al 17/10/2026. El JSON actual es `sistemas-rm-615-2016.json`: 50 materias, 28 relaciones, umbral de Taller (STS-300) de 45 aprobadas distintas de sí mismo. Incluye `horas_totales`: 40 modulares de 85 horas, 9 bimodulares de 170 y Taller de duración especial de 400. La interfaz muestra etiquetas de duración, sin convertir a meses.

Fuente principal: [pénsum SAE](../../CONTEXTO/Pensum%20Ingenieria%20de%20Sistemas.pdf). La [malla anterior](../../CONTEXTO/REFERENCIAS/ing.-de-sistemas-mallas-pensum-2.pdf) conserva nombres/áreas donde SAE recorta textos. Requisitos y decisiones completos en [PRODUCTO_Y_PLAN](../../CONTEXTO/PRODUCTO_Y_PLAN.md).

`sistemas-rm-615-2016-v1.json` conserva el baseline anterior (29 relaciones y umbral 40). Es evidencia para la migración, no el catálogo vigente. Los 50 UUID y el plan se mantienen; no regenerarlos al corregir nombres o códigos.

## Persistencia y validación

Android lee el catálogo remoto en Firestore; este JSON no es un catálogo alternativo de producción en la APK. Solo la APK de pruebas lo incluye como fixture.

- Materias: `materias/{id}`, con plan, código, nombre, semestre, área, umbral y horas. El `id` es la ruta del documento.
- Requisitos: `materias/{id_objetivo}/prerrequisitos/{id_requisito}`, documento vacío. Múltiples requisitos se exigen conjuntamente.
- Estados y preparación personales están separados del catálogo.

Desde la raíz del clon, Python 3 con biblioteca estándar:

```powershell
python -B firebase/scripts/validate-academic-catalog.py
python -B -m unittest discover -s firebase/tests -p test_academic_catalog.py -v
```

El validador comprueba campos, tipos, UUID/códigos, plan, distribución, relaciones, ciclos y condiciones especiales; falla con exit 1 ante errores. Su validación estructural no vuelve a contrastar visualmente el PDF. La transcripción antigua no debe usarse para validar automáticamente el contrato SAE nuevo.

El catálogo SAE ya fue publicado y relecto según [ESTADO_ACTUAL](../../CONTEXTO/ESTADO_ACTUAL.md). `academic-catalog.cjs` conserva su contrato de creación sin sobrescritura. `migrate-sae-catalog.cjs` es una migración administrativa específica con precondiciones, simulación por defecto y commit atómico. No ejecutar publicadores para configurar un clon ni volver a aplicar migraciones antiguas por leer un informe histórico. No acceden a estudiantes ni despliegan reglas como parte de la migración SAE.
