"""Release guards for canonical names, preserved translations and live search keys."""
import copy
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import sync_languages as languages


class LanguageCoverageTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.terms = languages.read(languages.ROOT / "config/language-terms.json")

    def test_all_registered_names_and_live_search_groups_have_three_languages(self):
        report = languages.synchronise(check=True)
        self.assertEqual(2940, report["registered_blocks"])
        self.assertEqual(3117, report["language_keys"])
        self.assertEqual(23, report["search_groups"])
        self.assertEqual(131, report["exported_item_tags"])

    def test_generation_preserves_existing_names_and_never_copies_ui_fallbacks(self):
        english = {"block.themelios.aganite_block": "Aganite Block",
                   "block.themelios.aganite_aged_slab": "Aganite Aged Slab", "itemGroup.themelios": "ERYDON Themelios"}
        existing = {"block.themelios.aganite_block": "Manuell übersetzter Name", "itemGroup.themelios": "ERYDON Themelios"}
        original = dict(existing)
        result = languages.complete(english, existing, "de_de", self.terms)
        self.assertEqual(original, existing)
        self.assertEqual(existing["block.themelios.aganite_block"], result["block.themelios.aganite_block"])
        self.assertEqual("Aganite Gealtert Stufe", result["block.themelios.aganite_aged_slab"])
        self.assertEqual(result, languages.complete(english, result, "de_de", self.terms))
        with self.assertRaisesRegex(ValueError, "UI/search"):
            languages.complete({"search.themelios.new": "new"}, {}, "es_es", self.terms)

    def test_weaves_finishes_and_inlays_retain_both_materials_and_motifs(self):
        expected = {
            ("de_de", "calacattum_portorium_weave_bronze_block"): "Calacattum und Portorium Bronze Geflecht Block",
            ("es_es", "calacattum_portorium_weave_grout_block"): "Calacattum y Portorium Tejido Lechada Bloque",
            ("de_de", "aganite_quatrefoil_silver_block"): "Aganite Silber Vierpass Intarsie",
            ("es_es", "aganite_rosette_bronze_block"): "Aganite Bronce Rosetón Incrustación",
            ("de_de", "aganite_ashlar_cylinder_small"): "Aganite Quader Kleiner Zylinder",
            ("es_es", "aganite_rusticated_layer_vertical"): "Aganite Rusticado Capa vertical",
        }
        for (language, path), value in expected.items():
            self.assertEqual(value, languages.localized_name(path, language, self.terms))
        for path in ("unknown_block", "aganite_future_block", "aganite_unknown_form"):
            with self.assertRaises(ValueError):
                languages.localized_name(path, "de_de", self.terms)

    def test_every_current_canonical_id_is_classified_without_english_fallback(self):
        assets = languages.ROOT / "src/main/resources/assets/themelios/blockstates"
        for path in assets.glob("*.json"):
            for language in ("de_de", "es_es"):
                self.assertTrue(languages.localized_name(path.stem, language, self.terms))

    def test_every_owned_item_tag_has_explicit_or_material_derived_labels(self):
        keys = languages.item_tag_keys(languages.ROOT)
        self.assertEqual(131, len(keys))
        self.assertFalse(any(key.startswith(("tag.item.minecraft.", "tag.item.c.")) for key in keys))
        for name in keys.values():
            for language in languages.LANGUAGES:
                self.assertTrue(languages.localized_tag_name(name, language, self.terms))
        self.assertEqual("Marmor", languages.localized_tag_name("marble", "de_de", self.terms))
        self.assertEqual("Caliza", languages.localized_tag_name("limestone", "es_es", self.terms))
        with self.assertRaises(ValueError):
            languages.localized_tag_name("future_group", "en_us", self.terms)
        english = {"block.themelios.aganite_block": "Aganite Block", "tag.item.themelios.aganite": "Aganite"}
        documents = {language: dict(english) for language in languages.LANGUAGES}
        languages.validate_documents(documents, {"aganite_block"}, set(), set(), {"tag.item.themelios.aganite"})
        del documents["de_de"]["tag.item.themelios.aganite"]
        with self.assertRaises(ValueError):
            languages.validate_documents(documents, {"aganite_block"}, set(), set(), {"tag.item.themelios.aganite"})

    def test_missing_names_search_keys_and_corrupt_text_fail_validation(self):
        english = {"block.themelios.aganite_block": "Aganite Block", "search.themelios.ashlar": "masonry"}
        docs = {language: dict(english) for language in languages.LANGUAGES}
        arguments = ({"aganite_block"}, {"aganite_old"}, {"search.themelios.ashlar"})
        languages.validate_documents(docs, *arguments)
        for key, replacement in (("block.themelios.aganite_block", None),
                                 ("search.themelios.ashlar", None),
                                 ("search.themelios.ashlar", "mamposter\ufffd\\xada"),
                                 ("search.themelios.ashlar", "Cornisa peque\u00c3\u00b1a"),
                                 ("search.themelios.ashlar", "masonry|")):
            broken = copy.deepcopy(docs)
            if replacement is None:
                del broken["es_es"][key]
            else:
                broken["es_es"][key] = replacement
            with self.assertRaises(ValueError):
                languages.validate_documents(broken, *arguments)


if __name__ == "__main__":
    unittest.main()
