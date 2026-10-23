# Iconos de Malla

Fuente: diseño aprobado RoadMap UTEPSA, Figma file YCcVIGVjbSLOKygieTbEOt,
pantalla 10:261 y panel 10:595. Descargados el 16/10/2026.

Los cinco SVG originales se conservan sin cambios. Sus PNG locales en
app/src/main/res/drawable-xxxhdpi fueron renderizados a escala 4x con sharp:

| Archivo | Tamaño original | PNG | Uso en CurriculumScreen |
|---|---|---|---|
| account.svg | 24 × 24 | 96 × 96 | Cabecera, 24dp |
| summary.svg | 21 × 21 | 84 × 84 | Navegación Resumen, 21dp |
| curriculum.svg | 21 × 21 | 84 × 84 | Navegación Malla, 21dp |
| offer.svg | 21 × 21 | 84 × 84 | Navegación Oferta, 21dp |
| recommendations.svg | 21 × 21 | 84 × 84 | Navegación Para ti, 21dp |

Android aplica los colores del componente Icon, conservando la geometría y transparencia
originales. No hay referencias de red a URLs de assets temporales en ejecución.
Oferta reutiliza estos cinco iconos en Oferta (cabecera 24dp, navegación 21dp).
El 18/10/2026 se descargaron los SVG de Figma 15:431 y se comparó su geometría y
dimensiones con estos originales: los cinco coinciden, independientemente del tinte.
Los PNG locales son no vacíos y se usan en OfertaScreen y CurriculumScreen.
La selección de materias usa Checkbox de Material 3 como control dinámico.
Las cuatro pestañas (Resumen, Malla, Oferta y Para ti) están integradas en el MVP.
