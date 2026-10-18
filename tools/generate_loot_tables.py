#!/usr/bin/env python3
"""Generate canonical native block loot; no runtime hooks or packaging are needed."""
from __future__ import annotations

import argparse
import csv
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_BLOCKS = 2940
SHAPES = ("slice_horizontal", "slice_vertical", "layer_vertical", "cylinder_small",
          "cylinder_large", "cornice_small", "cornice_large", "block", "slab",
          "stairs", "wall", "post", "layer")
STACKABLE = frozenset(("layer", "layer_vertical", "slice_horizontal", "slice_vertical", "post"))
CORNERS = ("north_east", "east_south", "south_west", "west_north")


def loot_directory(root: Path) -> Path:
    properties = dict(line.split("=", 1) for line in (root / "gradle.properties").read_text(
        encoding="utf-8-sig").splitlines() if "=" in line and not line.lstrip().startswith("#"))
    version = properties["minecraft_version"].strip()
    if version not in {"1.20.1", "1.21.1"}:
        raise ValueError(f"Review the loot directory for Minecraft {version} before generating")
    folder = "loot_tables" if version == "1.20.1" else "loot_table"
    return root / "src/main/resources/data/themelios" / folder / "blocks"


def canonical_ids(root: Path = ROOT) -> list[str]:
    resources = root / "src/main/resources"
    assets = resources / "assets/themelios"
    ids = {p.stem for p in (assets / "blockstates").glob("*.json")}
    items = {p.stem for p in (assets / "models/item").glob("*.json")} - {"emblem"}
    with (resources / "data/themelios/id_migration.tsv").open(encoding="utf-8-sig") as stream:
        legacy = {row["old_path"] for row in csv.DictReader(stream, delimiter="\t")}
    if len(ids) != EXPECTED_BLOCKS or ids != items or ids.intersection(legacy):
        raise ValueError("Expected 2,940 canonical placed blocks/items without duplicate legacy aliases")
    return sorted(ids)


def shape(block_id: str) -> str:
    for suffix in SHAPES:
        if block_id.endswith("_" + suffix):
            return suffix
    raise ValueError("Unclassified loot shape: " + block_id)


def count_function(block_id: str, property_name: str, value: str, count: int) -> dict:
    return {
        "function": "minecraft:set_count", "count": count,
        "conditions": [{"condition": "minecraft:block_state_property",
                        "block": "themelios:" + block_id, "properties": {property_name: value}}],
    }


def loot_table(block_id: str) -> dict:
    form = shape(block_id)
    functions = []
    if form in STACKABLE:
        functions = [count_function(block_id, "layers", str(count), count) for count in range(2, 9)]
    elif form == "slab":
        functions = [count_function(block_id, "type", "double", 2)]
    elif form in {"cornice_small", "cornice_large"}:
        functions = [count_function(block_id, "shape", corner, 2) for corner in CORNERS]
    entry = {"type": "minecraft:item", "name": "themelios:" + block_id}
    if functions:
        entry["functions"] = functions
    return {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [entry],
        "conditions": [{"condition": "minecraft:survives_explosion"}],
    }]}


def synchronize(root: Path = ROOT, check: bool = False) -> int:
    ids = canonical_ids(root)
    output = loot_directory(root)
    expected = {name + ".json" for name in ids}
    stale = {p.name for p in output.glob("*.json")} - expected
    wrong_folder = "loot_table" if output.parent.name == "loot_tables" else "loot_tables"
    wrong = list((output.parent.parent / wrong_folder).rglob("*.json"))
    if stale or wrong:
        raise ValueError(f"Unexpected or version-incorrect native loot files: {sorted(stale)}; {wrong}")
    changed = []
    for name in ids:
        path = output / (name + ".json")
        text = json.dumps(loot_table(name), indent=2, ensure_ascii=False) + "\n"
        if path.exists() and path.read_text(encoding="utf-8") == text:
            continue
        changed.append(name)
        if not check:
            output.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8", newline="\n")
    if check and changed:
        raise ValueError(f"Native loot is missing or stale for {len(changed)} blocks; first: {changed[0]}")
    print(f"Themelios loot: {len(ids)} canonical tables; {len(changed)} {'stale' if check else 'written'}; "
          f"shapes={dict(sorted(Counter(map(shape, ids)).items()))}")
    return len(changed)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    try:
        synchronize(check=args.check)
    except (ValueError, KeyError) as error:
        parser.exit(1, str(error) + "\n")
