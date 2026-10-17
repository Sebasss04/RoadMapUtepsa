"""Safety checks for malformed catalogs, without Firebase or Android."""
import copy
import importlib.util
import json
from pathlib import Path
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "validate-academic-catalog.py"
SPEC = importlib.util.spec_from_file_location("catalog_validator", SCRIPT)
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)


class AcademicCatalogTest(unittest.TestCase):
    def setUp(self):
        self.data = json.loads(validator.DEFAULT_CATALOG.read_text(encoding="utf-8"))
        self.rows = {r["codigo"]: r for r in self.data["materias"]}

    def reject(self, text):
        with self.assertRaisesRegex(ValueError, text):
            validator.validate(self.data)

    def test_reviewed_catalog_is_valid(self):
        result = validator.validate(self.data)
        self.assertEqual((50, 28), (result["materias"], result["relaciones"]))
        self.assertTrue(result["sin_ciclos"])

    def test_missing_subject(self):
        self.data["materias"].pop()
        self.reject("50 materias")

    def test_duplicate_id(self):
        self.data["materias"][1]["id"] = self.data["materias"][0]["id"]
        self.reject("ID duplicado")

    def test_duplicate_code(self):
        self.data["materias"][1]["codigo"] = self.data["materias"][0]["codigo"]
        self.reject("Código duplicado")

    def test_wrong_plan(self):
        self.rows["BMS-300"]["id_plan"] = "otro-plan"
        self.reject("otro plan")

    def test_invalid_subject_fields(self):
        cases = [("nombre", "", "Nombre"), ("area", "OTRA", "Área"),
                 ("semestre", True, "Semestre"), ("semestre", 10, "Semestre"),
                 ("codigo", "BMA303", "Código"), ("id", "m001", "UUID")]
        original = copy.deepcopy(self.data)
        for field, value, message in cases:
            with self.subTest(field=field, value=value):
                self.data = copy.deepcopy(original)
                self.data["materias"][0][field] = value
                self.reject(message)

    def test_wrong_semester_distribution(self):
        self.rows["BMS-300"]["semestre"] = 2
        self.reject("Distribución")

    def test_rejects_unexpected_fields(self):
        self.rows["BMS-300"]["requisitos"] = []
        self.reject("Campos de materia")

    def test_taller_requires_exactly_45(self):
        for value in [0, 40, 44, 46, "45", True]:
            with self.subTest(value=value):
                self.rows["STS-300"]["min_aprobadas"] = value
                self.reject("Umbral")

    def test_other_subjects_have_no_count_threshold(self):
        self.rows["BMS-300"]["min_aprobadas"] = 40
        self.reject("Umbral")

    def test_unknown_reference(self):
        self.data["prerrequisitos"][0]["id_requisito"] = "inexistente"
        self.reject("Referencia inexistente")

    def test_duplicate_edge(self):
        self.data["prerrequisitos"].append(copy.deepcopy(self.data["prerrequisitos"][0]))
        self.reject("Prerrequisito duplicado")

    def test_self_reference(self):
        edge = self.data["prerrequisitos"][0]
        edge["id_requisito"] = edge["id_objetivo"]
        self.reject("Autorrequisito")

    def test_cycle(self):
        # BMS-301 already requires BMS-300; this creates the reverse edge.
        self.data["prerrequisitos"].append(dict(
            id_objetivo=self.rows["BMS-300"]["id"], id_requisito=self.rows["BMS-301"]["id"]))
        self.reject("Ciclo: BMS-300 -> BMS-301 -> BMS-300")

    def test_missing_edge(self):
        self.data["prerrequisitos"].pop()
        self.reject("28 relaciones")

    def test_integrator_cannot_substitute_one_requirement(self):
        for edge in self.data["prerrequisitos"]:
            if (edge["id_objetivo"] == self.rows["SIS-306"]["id"] and
                    edge["id_requisito"] == self.rows["SCC-305"]["id"]):
                edge["id_requisito"] = self.rows["BMS-300"]["id"]
        self.reject("Proyecto Integrador requiere")

    def test_taller_cannot_replace_threshold_with_edge(self):
        self.data["prerrequisitos"][0] = dict(
            id_objetivo=self.rows["STS-300"]["id"], id_requisito=self.rows["BMS-300"]["id"])
        self.reject("Taller usa umbral")


if __name__ == "__main__":
    unittest.main()
