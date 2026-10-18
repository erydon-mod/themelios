#!/usr/bin/env python3
"""Maintain static catalogue tags; never performs work in the game runtime."""
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
SHAPES = (
    "block", "slab", "stairs", "wall", "post", "slice_horizontal", "slice_vertical",
    "layer_vertical", "layer", "cylinder_small", "cylinder_large", "cornice_small", "cornice_large",
)
FINISHES = ("aged", "ashlar", "hewn", "rusticated", "rock")
MOTIFS = ("herringbone", "weave", "trim", "guilloche", "quatrefoil", "rosette")


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def tag(values):
    return {"replace": False, "values": sorted(set(values))}


def refs(*names):
    return tag("#themelios:" + name for name in names)


def catalogue(resources=RES):
    assets = resources / "assets/themelios"
    ids = {p.stem for p in (assets / "blockstates").glob("*.json")}
    items = {p.stem for p in (assets / "models/item").glob("*.json")}
    for path in (assets / "lang").glob("*.json"):
        names = {k.removeprefix("block.themelios.") for k in read(path) if k.startswith("block.themelios.")}
        if names != ids:
            raise ValueError(f"Catalogue/display-name mismatch: {path}")
    if not ids or ids != items - {"emblem"}:
        raise ValueError("Placed block and player item catalogues differ")
    with (resources / "data/themelios/id_migration.tsv").open(encoding="utf-8-sig") as stream:
        aliases = {row["old_path"]: row["canonical_path"] for row in csv.DictReader(stream, delimiter="\t")}
    if ids.intersection(aliases):
        raise ValueError("Legacy aliases must not become duplicate catalogue entries")
    return ids


def shape(path):
    # Test complete suffixes, with the more specific layer_vertical first.
    for name in SHAPES:
        if path.endswith("_" + name):
            return name
    raise ValueError("Unclassified catalogue shape: " + path)


def resolve(tags, key, ancestors=()):
    if key in ancestors:
        raise ValueError("Cyclic tag: " + " -> ".join((*ancestors, key)))
    if key not in tags:
        raise ValueError("Missing tag: " + key)
    result = set()
    for value in tags[key]["values"]:
        if value.startswith("#themelios:"):
            result.update(resolve(tags, value.split(":", 1)[1], (*ancestors, key)))
        elif value.startswith("themelios:"):
            result.add(value.split(":", 1)[1])
        else:
            raise ValueError("Unexpected catalogue member: " + value)
    return result


def expected_tags(ids, config):
    tags = {}

    def members(name, paths):
        tags[name] = tag("themelios:" + p for p in paths)

    materials = config["materials"]
    for material in materials:
        matches = {p for p in ids if p.startswith(material + "_")
                   or ("_weave_" in p and material in p.split("_weave_", 1)[0].split("_"))}
        # Existing shared Herringbone artwork includes Borealis in these pieces.
        for source in config.get("shared_herringbone_materials", {}).get(material, []):
            matches.update(p for p in ids if p.startswith(source + "_herringbone_"))
        members(material, matches)
        weave = {p for p in matches if "_weave_" in p}
        if weave:
            members(material + "_weave", weave)
    for name in SHAPES:
        members(name if name != "layer" else "layer_horizontal", {p for p in ids if shape(p) == name})
    tags.update({
        "themelios_blocks": refs(*("layer_horizontal" if n == "layer" else n for n in SHAPES)),
        "stone": refs("themelios_blocks"),
        "walls": refs("wall"), "slabs": refs("slab"),
        "horizontal_slice": refs("slice_horizontal"), "vertical_slice": refs("slice_vertical"),
        "layer": refs("layer_horizontal", "layer_vertical"),
        "layers": refs("layer"), "slice": refs("slice_horizontal", "slice_vertical"),
        "slices": refs("slice"), "horizontal": refs("slice_horizontal", "layer_horizontal"),
        "vertical": refs("slice_vertical", "layer_vertical"),
        "cylinder": refs("cylinder_small", "cylinder_large"), "column": refs("cylinder"),
        "cornice": refs("cornice_small", "cornice_large"), "moulding": refs("cornice"),
        "molding": refs("moulding"), "small": refs("cylinder_small", "cornice_small"),
        "large": refs("cylinder_large", "cornice_large"),
        "structural": refs("block", "slab", "stairs", "wall", "post", "cylinder", "layer", "slice"),
        "decorative": refs("cornice", "trim", "guilloche", "quatrefoil", "rosette", "herringbone", "weave"),
    })
    for name in (*FINISHES, *MOTIFS, "bronze", "silver"):
        members(name, {p for p in ids if name in p.split("_")})
    standard = {p for p in ids if not set(p.split("_")).intersection((*FINISHES, *MOTIFS))}
    members("polished", standard)
    tags["honed"] = refs("polished")
    tags["mirror"] = refs("polished")
    tags["rose"] = refs("rosette")
    tags["byzantine"] = refs("guilloche")
    for name in ("herringbone", "weave"):
        for variant in ("bronze", "grout"):
            members(name + "_" + variant, {p for p in ids if f"_{name}_{variant}_" in p})
    for name, stones in config["geology"].items():
        tags[name] = refs(*stones)
    for name in tags:
        if resolve(tags, name) - ids:
            raise ValueError("Missing player item in tag " + name)
    if resolve(tags, "themelios_blocks") != ids or resolve(tags, "stone") != ids:
        raise ValueError("Incomplete catalogue")
    if set().union(*(resolve(tags, material) for material in materials)) != ids:
        raise ValueError("Catalogue has an unclassified material; update config/catalogue-tags.json")
    return tags


def paths_for(tags, config, singular):
    output = {}
    for kind in (("block", "item") if singular else ("blocks", "items")):
        for name, data in tags.items():
            output[f"data/themelios/tags/{kind}/{name}.json"] = data
        for name in (*config["geology"], "stone"):
            output[f"data/material/tags/{kind}/{name}.json"] = refs(name)
        for name in ("bronze", "silver"):
            output[f"data/color/tags/{kind}/{name}.json"] = refs(name)
        for name in ("guilloche", "byzantine"):
            output[f"data/style/tags/{kind}/{name}.json"] = refs(name)
        for vanilla, target in (("walls", "wall"), ("slabs", "slab"), ("stairs", "stairs")):
            output[f"data/minecraft/tags/{kind}/{vanilla}.json"] = refs(target)
    block_kind = "block" if singular else "blocks"
    output[f"data/minecraft/tags/{block_kind}/mineable/pickaxe.json"] = refs("themelios_blocks")
    return output


def synchronise(check=False):
    properties = (ROOT / "gradle.properties").read_text(encoding="utf-8-sig")
    singular = "minecraft_version=1.21." in properties
    config = read(ROOT / "config/catalogue-tags.json")
    ids = catalogue()
    tags = expected_tags(ids, config)
    output = paths_for(tags, config, singular)
    changed = []
    if singular:
        # Move only tag JSONs within this project's resource tree, never backups.
        for path in sorted((RES / "data").glob("*/tags/blocks/**/*.json")) + sorted((RES / "data").glob("*/tags/items/**/*.json")):
            relative = path.relative_to(RES)
            parts = list(relative.parts)
            parts[3] = {"blocks": "block", "items": "item"}[parts[3]]
            target = RES.joinpath(*parts)
            changed.append(str(relative))
            if not check:
                if target.exists():
                    raise ValueError("Both legacy and current tag paths exist: " + str(target))
                target.parent.mkdir(parents=True, exist_ok=True)
                path.rename(target)
    for relative, data in sorted(output.items()):
        path = RES / relative
        if path.exists() and read(path) == data:
            continue
        changed.append(relative)
        if not check:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    # Validate any pre-existing custom tag too, rather than silently deleting it.
    for kind in (("block", "item") if singular else ("blocks", "items")):
        for path in (RES / f"data/themelios/tags/{kind}").glob("*.json"):
            if path.stem not in tags:
                tags[path.stem] = read(path)
                if resolve(tags, path.stem) - ids:
                    raise ValueError("Unknown catalogue member in " + str(path))
    print(json.dumps({"catalogue_items": len(ids), "block_tags": len(tags), "item_tags": len(tags),
                      "full_blocks": len(resolve(tags, "block")), "standard_finish_items": len(resolve(tags, "polished")),
                      "singular_tag_directories": singular, "changed": len(changed)}))
    return bool(changed)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    raise SystemExit(1 if synchronise(args.check) and args.check else 0)
