"""Catalogue/codec contracts supplement the real server loot GameTests."""
from __future__ import annotations

import csv
import json
import tempfile
import unittest
from pathlib import Path

from tools import generate_loot_tables as loot


class LootTableContracts(unittest.TestCase):
    def test_every_canonical_item_has_exactly_one_native_self_drop_table(self):
        ids = set(loot.canonical_ids())
        output = loot.loot_directory(loot.ROOT)
        self.assertEqual({name + ".json" for name in ids}, {p.name for p in output.glob("*.json")})
        for name in sorted(ids):
            with self.subTest(block=name):
                table = json.loads((output / (name + ".json")).read_text(encoding="utf-8"))
                self.assertEqual("minecraft:block", table["type"])
                self.assertEqual(1, len(table["pools"]))
                pool = table["pools"][0]
                self.assertEqual(1, pool["rolls"])
                self.assertEqual([{"condition": "minecraft:survives_explosion"}], pool["conditions"])
                self.assertEqual(1, len(pool["entries"]))
                entry = pool["entries"][0]
                self.assertEqual("minecraft:item", entry["type"])
                self.assertEqual("themelios:" + name, entry["name"])
                rules = {}
                for function in entry.get("functions", []):
                    self.assertEqual("minecraft:set_count", function["function"])
                    self.assertEqual(1, len(function["conditions"]))
                    condition = function["conditions"][0]
                    self.assertEqual("minecraft:block_state_property", condition["condition"])
                    self.assertEqual("themelios:" + name, condition["block"])
                    self.assertEqual(1, len(condition["properties"]))
                    key, value = next(iter(condition["properties"].items()))
                    self.assertNotIn((key, value), rules)
                    rules[key, value] = function["count"]
                if name.endswith(("_layer", "_layer_vertical", "_slice_horizontal", "_slice_vertical", "_post")):
                    expected = {("layers", str(n)): n for n in range(2, 9)}
                elif name.endswith("_slab"):
                    expected = {("type", "double"): 2}
                elif name.endswith(("_cornice_small", "_cornice_large")):
                    expected = {("shape", n): 2 for n in ("north_east", "east_south", "south_west", "west_north")}
                else:
                    expected = {}
                self.assertEqual(expected, rules)
                # Quantity rules must never depend on water, facing, top or post-axis state.
                self.assertTrue({key for key, value in rules} <= {"layers", "type", "shape"})

    def test_published_aliases_use_the_canonical_loot_without_duplicate_tables(self):
        output = loot.loot_directory(loot.ROOT)
        with (loot.ROOT / "src/main/resources/data/themelios/id_migration.tsv").open(encoding="utf-8-sig") as stream:
            rows = list(csv.DictReader(stream, delimiter="\t"))
        self.assertEqual(861, len(rows))
        for row in rows:
            self.assertFalse((output / (row["old_path"] + ".json")).exists(), row["old_path"])
            self.assertTrue((output / (row["canonical_path"] + ".json")).is_file(), row["canonical_path"])

    def test_both_ports_select_their_actual_vanilla_resource_directory(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for version, expected in (("1.20.1", "loot_tables"), ("1.21.1", "loot_table")):
                (root / "gradle.properties").write_text("minecraft_version=" + version, encoding="utf-8")
                self.assertEqual(root / "src/main/resources/data/themelios" / expected / "blocks", loot.loot_directory(root))
            (root / "gradle.properties").write_text("minecraft_version=unknown", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "Review the loot directory"):
                loot.loot_directory(root)


if __name__ == "__main__":
    unittest.main()
