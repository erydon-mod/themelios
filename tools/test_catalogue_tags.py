"""Exhaustive source coverage plus semantic guards for browser and gameplay tags."""
import json
import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
import sync_catalogue_tags as catalogue


class CatalogueTagsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ids = catalogue.catalogue()
        cls.config = catalogue.read(catalogue.ROOT / "config/catalogue-tags.json")
        cls.tags = catalogue.expected_tags(cls.ids, cls.config)

    def members(self, name):
        return catalogue.resolve(self.tags, name)

    def test_every_player_block_has_material_and_exact_shape(self):
        shapes = [self.members(n if n != "layer" else "layer_horizontal") for n in catalogue.SHAPES]
        self.assertEqual(self.ids, set.union(*shapes))
        self.assertEqual(len(self.ids), sum(map(len, shapes)))
        self.assertEqual(self.ids, set.union(*(self.members(m) for m in self.config["materials"])))
        self.assertEqual(self.ids, self.members("stone"))

    def test_partial_shapes_never_enter_full_block_or_vanilla_shape_tags(self):
        self.assertEqual({p for p in self.ids if p.endswith("_block")}, self.members("block"))
        for name in ("slab", "stairs", "wall"):
            self.assertEqual({p for p in self.ids if p.endswith("_" + name)}, self.members(name))
        self.assertEqual(self.members("wall"), self.members("walls"))

    def test_weaves_include_both_stones_and_existing_shared_inlays(self):
        for path in self.ids:
            if "_weave_" in path:
                for material in path.split("_weave_", 1)[0].split("_"):
                    self.assertIn(path, self.members(material))
        for source in ("kelastrion", "latmion", "psamatheon"):
            for path in self.ids:
                if path.startswith(source + "_herringbone_"):
                    for name in ("borealis", "labradorite", "gemstone"):
                        self.assertIn(path, self.members(name))

    def test_finish_and_vocabulary_aliases_keep_their_meaning(self):
        for path in self.members("polished"):
            self.assertFalse(set(path.split("_")).intersection((*catalogue.FINISHES, *catalogue.MOTIFS)))
        for name in ("honed", "mirror"):
            self.assertEqual(self.members("polished"), self.members(name))
        for left, right in (("rosette", "rose"), ("byzantine", "guilloche"),
                            ("horizontal_slice", "slice_horizontal"), ("vertical_slice", "slice_vertical")):
            self.assertEqual(self.members(left), self.members(right))
        self.assertEqual(self.members("layer_horizontal") | self.members("layer_vertical"), self.members("layer"))
        self.assertLessEqual(self.members("layer_horizontal"), self.members("horizontal"))

    def test_block_and_item_tags_match_in_both_version_layouts(self):
        for singular in (False, True):
            files = catalogue.paths_for(self.tags, self.config, singular)
            block, item = ("block", "item") if singular else ("blocks", "items")
            for path, contents in files.items():
                if f"/tags/{block}/" in path and "/mineable/" not in path:
                    self.assertEqual(contents, files[path.replace(f"/tags/{block}/", f"/tags/{item}/")])
            self.assertFalse(any(p.startswith("data/c/") for p in files), "Catalogue tags must not imply recipe equivalence")

    def test_missing_tags_and_cycles_fail_closed(self):
        for data in ({"a": catalogue.refs("missing")}, {"a": catalogue.refs("b"), "b": catalogue.refs("a")}):
            with self.assertRaises(ValueError):
                catalogue.resolve(data, "a")

    def test_committed_tags_are_current(self):
        self.assertFalse(catalogue.synchronise(check=True))


if __name__ == "__main__":
    unittest.main()
