"""Validate the v1 academic catalog locally. No Firebase SDK or network access."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import sys
from uuid import UUID

PLAN = "sistemas-rm-615-2016"
DEFAULT_CATALOG = Path(__file__).resolve().parents[1] / "catalogo" / f"{PLAN}.json"
AREAS = {"BASICA", "COMPLEMENTARIA", "ESPECIALIDAD", "ELECTIVA"}
AREA_LABELS = dict(zip(
    ["Básica", "Complementaria", "Especialidad", "Electiva"],
    ["BASICA", "COMPLEMENTARIA", "ESPECIALIDAD", "ELECTIVA"],
))
SEMESTERS = {1: 5, 2: 6, 3: 7, 4: 4, 5: 6, 6: 6, 7: 6, 8: 7, 9: 3}
FIELDS = {"id", "id_plan", "codigo", "nombre", "semestre", "area", "min_aprobadas", "horas_totales"}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha256(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def read_transcription(path):
    """Read the reviewed Markdown tables; never infer edges from semester."""
    rows = {}
    semester = None
    for line in Path(path).read_text(encoding="utf-8-sig").splitlines():
        heading = re.fullmatch(r"## Semestre ([1-9])", line)
        if heading:
            semester = int(heading[1])
        if not re.match(r"\| [A-Z]+-\d{3} \|", line):
            continue
        code, name, area, requirements = [s.strip() for s in line.strip("|").split("|")]
        require(code not in rows, f"Código duplicado en transcripción: {code}")
        require(semester is not None and area in AREA_LABELS, f"Fila inválida: {code}")
        if requirements == "Sin flecha entrante":
            prerequisites, minimum = [], 0
        elif requirements == "40 materias aprobadas":
            prerequisites, minimum = [], 40
        else:
            prerequisites, minimum = requirements.split(" y "), 0
            require(all(re.fullmatch(r"[A-Z]+-\d{3}", c) for c in prerequisites),
                    f"Requisito no reconocido: {requirements}")
        rows[code] = dict(nombre=name, semestre=semester, area=AREA_LABELS[area],
                          min_aprobadas=minimum, requisitos=sorted(prerequisites))
    require(len(rows) == 50, "La transcripción debe contener 50 materias")
    return rows


def validate(data, source_markdown=None, source_pdf=None):
    require(isinstance(data, dict), "El catálogo debe ser un objeto")
    require(set(data) == {"version_formato", "id_plan", "fuente", "materias", "prerrequisitos"},
            "Campos de catálogo incorrectos")
    require(type(data["version_formato"]) is int and data["version_formato"] == 2,
            "Versión de formato incorrecta")
    require(data["id_plan"] == PLAN, "Plan incorrecto")
    source = data["fuente"]
    require(isinstance(source, dict) and set(source) == {"archivo_pdf", "sha256_pdf", "resolucion"},
            "Metadatos de fuente incorrectos")
    require(source["resolucion"] == "615/2016" and
            source["archivo_pdf"] == "Pensum Ingenieria de Sistemas.pdf" and
            isinstance(source["sha256_pdf"], str) and
            re.fullmatch(r"[0-9a-f]{64}", source["sha256_pdf"]), "Fuente curricular incorrecta")
    subjects, edges = data["materias"], data["prerrequisitos"]
    require(isinstance(subjects, list) and len(subjects) == 50, "Se requieren 50 materias")
    require(isinstance(edges, list), "Prerrequisitos debe ser una lista")
    by_id, by_code = {}, {}
    for row in subjects:
        require(isinstance(row, dict) and set(row) == FIELDS, "Campos de materia incorrectos")
        identity, code = row["id"], row["codigo"]
        require(isinstance(identity, str), "ID debe ser texto")
        try:
            require(str(UUID(identity)) == identity, "ID UUID no canónico")
        except (ValueError, AttributeError) as error:
            raise ValueError(f"ID UUID inválido: {identity}") from error
        require(identity not in by_id, f"ID duplicado: {identity}")
        require(isinstance(code, str) and re.fullmatch(r"[A-Z]+-\d{3}", code), "Código inválido")
        require(code not in by_code, f"Código duplicado: {code}")
        require(row["id_plan"] == PLAN, f"Materia de otro plan: {code}")
        require(isinstance(row["nombre"], str) and row["nombre"].strip() == row["nombre"]
                and bool(row["nombre"]), f"Nombre inválido: {code}")
        require(type(row["semestre"]) is int and row["semestre"] in SEMESTERS,
                f"Semestre inválido: {code}")
        require(isinstance(row["area"], str) and row["area"] in AREAS, f"Área inválida: {code}")
        require(type(row["min_aprobadas"]) is int and
                row["min_aprobadas"] == (45 if code == "STS-300" else 0),
                f"Umbral incorrecto: {code}")
        bimodulares = {"BMS-300", "SCC-300", "SAL-301", "SIS-302", "SRF-301", "SIS-303", "SIS-304", "SDA-304", "SIS-306"}
        require(type(row["horas_totales"]) is int and row["horas_totales"] ==
                (400 if code == "STS-300" else 170 if code in bimodulares else 85), f"Horas incorrectas: {code}")
        by_id[identity], by_code[code] = row, row
    require("STS-300" in by_code, "Falta Taller de Graduación")
    require(dict(Counter(r["semestre"] for r in subjects)) == SEMESTERS,
            "Distribución por semestre incorrecta")

    prerequisites = {identity: set() for identity in by_id}
    pairs = set()
    for edge in edges:
        require(isinstance(edge, dict) and set(edge) == {"id_objetivo", "id_requisito"},
                "Campos de prerrequisito incorrectos")
        target, required = edge["id_objetivo"], edge["id_requisito"]
        require(isinstance(target, str) and isinstance(required, str), "Referencia debe ser texto")
        require(target in by_id and required in by_id, "Referencia inexistente")
        require(target != required, "Autorrequisito")
        require((target, required) not in pairs, "Prerrequisito duplicado")
        pairs.add((target, required))
        prerequisites[target].add(required)

    # DFS detects cycles regardless of semester and gives a useful diagnostic.
    done, active, order = set(), [], []

    def visit(identity):
        if identity in active:
            cycle = active[active.index(identity):] + [identity]
            raise ValueError("Ciclo: " + " -> ".join(by_id[i]["codigo"] for i in cycle))
        if identity in done:
            return
        active.append(identity)
        for required in sorted(prerequisites[identity], key=lambda i: by_id[i]["codigo"]):
            visit(required)
        active.pop()
        done.add(identity)
        order.append(by_id[identity]["codigo"])

    for identity in sorted(by_id, key=lambda i: by_id[i]["codigo"]):
        visit(identity)
    require(len(pairs) == 28, "Se requieren las 28 relaciones del PDF")
    require(not prerequisites[by_code["STS-300"]["id"]], "Taller usa umbral, no flechas")
    require("SIS-306" in by_code and
            {by_id[i]["codigo"] for i in prerequisites[by_code["SIS-306"]["id"]]} ==
            {"SIS-305", "SCC-305"}, "Proyecto Integrador requiere SIS-305 y SCC-305")
    baseline = json.loads(DEFAULT_CATALOG.with_name("sistemas-rm-615-2016-v1.json").read_text(encoding="utf-8-sig"))
    require({r["codigo"]: r["id"] for r in subjects} == {r["codigo"]: r["id"] for r in baseline["materias"]}, "Identidades del catálogo alteradas")
    ids_by_code = {r["codigo"]: r["id"] for r in subjects}
    removed = {ids_by_code[c] for c in ["SRA-304", "SES-302", "SES-303"]}
    expected_pairs = {(e["id_objetivo"], e["id_requisito"]) for e in baseline["prerrequisitos"] if e["id_objetivo"] not in removed}
    expected_pairs.update((ids_by_code[a], ids_by_code[b]) for a, b in [("SDA-303", "SIS-302"), ("SOI-304", "SOI-303")])
    require(pairs == expected_pairs, "Las relaciones difieren de SAE")
    if source_markdown:
        expected = read_transcription(source_markdown)
        actual = {
            code: dict(nombre=r["nombre"], semestre=r["semestre"], area=r["area"],
                       min_aprobadas=r["min_aprobadas"],
                       requisitos=sorted(by_id[i]["codigo"] for i in prerequisites[r["id"]]))
            for code, r in by_code.items()
        }
        require(actual == expected, "El catálogo difiere de la transcripción revisada")
    if source_pdf:
        require(sha256(source_pdf) == source["sha256_pdf"], "El SHA-256 del PDF no coincide")
    return dict(
        valido=True, materias=len(subjects), relaciones=len(pairs), sin_ciclos=True,
        materias_por_semestre=dict(sorted(Counter(r["semestre"] for r in subjects).items())),
        materias_por_area=dict(sorted(Counter(r["area"] for r in subjects).items())),
        taller_min_aprobadas=45, taller_sin_flechas=True,
        proyecto_integrador_requisitos=["SCC-305", "SIS-305"],
        comparacion_transcripcion=bool(source_markdown), sha256_pdf_comprobado=bool(source_pdf),
        orden_topologico=order,
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, default=DEFAULT_CATALOG)
    parser.add_argument("--source-markdown", type=Path)
    parser.add_argument("--source-pdf", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    try:
        data = json.loads(args.catalog.read_text(encoding="utf-8-sig"))
        result = validate(data, args.source_markdown, args.source_pdf)
        result["sha256_catalogo"] = sha256(args.catalog)
    except (ValueError, OSError, TypeError, KeyError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
