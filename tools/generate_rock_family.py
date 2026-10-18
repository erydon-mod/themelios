#!/usr/bin/env python3
"""Generate and verify Themelios's Rock texture family.

Rock mirrors Themelios's existing Hewn shapes. The approved native texture
outputs are imported byte-for-byte from the authoritative ERYDON repository;
Themelios geometry, item models, tags, and names are derived from its own Hewn
family so both Minecraft ports remain structurally consistent.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
from pathlib import Path

import sync_languages


REPO_ROOT = Path(__file__).resolve().parents[1]
RESOURCES = REPO_ROOT / "src" / "main" / "resources"
TAG_KINDS = ("block", "item") if "minecraft_version=1.21." in (REPO_ROOT / "gradle.properties").read_text() else ("blocks", "items")
JAVA_ROOT = (
    REPO_ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "oliver"
    / "erydonthemelios"
)

MATERIALS = (
    "aganite",
    "aterzon",
    "borealis",
    "brectite",
    "calacattum",
    "chalstrom",
    "chrysonyx",
    "etruscus",
    "gelastrum",
    "glacium",
    "hesperion",
    "imperium",
    "kelastrion",
    "kylorion",
    "latmion",
    "laurentium",
    "mielonyx",
    "nerium",
    "noxoplis",
    "porphyros",
    "portorium",
    "psamatheon",
    "rosinium",
    "sanguenite",
    "selenephos",
    "solistra",
    "striatus",
)

PRIMARY_SHAPES = (
    "block",
    "slab",
    "stairs",
    "wall",
    "post",
    "slice_horizontal",
    "slice_vertical",
    "layer",
    "layer_vertical",
)
CYLINDER_SHAPES = ("cylinder_small", "cylinder_large")
ALL_SHAPES = PRIMARY_SHAPES + CYLINDER_SHAPES

JSON_MIRRORS = (
    (RESOURCES / "assets" / "themelios" / "blockstates", 297),
    (RESOURCES / "assets" / "themelios" / "models" / "item", 297),
    (RESOURCES / "assets" / "themelios" / "models" / "block", 27),
    (RESOURCES / "assets" / "erydon" / "models" / "block", 1512),
)

LANGUAGE_TERMS = {
    "de_de.json": {
        "word": "Fels",
        "cylinder_small": "Kleiner Zylinder",
        "cylinder_large": "Großer Zylinder",
        "block_suffix": " Block",
    },
    "es_es.json": {
        "word": "Roca",
        "cylinder_small": "Cilindro pequeño",
        "cylinder_large": "Cilindro grande",
        "block_suffix": " Bloque",
    },
}


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write_json(path: Path, value: dict, *, indent: int = 2) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=indent) + "\n",
        encoding="utf-8",
    )


def rock_path_from_hewn(path: Path) -> Path:
    relative = path.relative_to(REPO_ROOT)
    return REPO_ROOT.joinpath(
        *(part.replace("_hewn_", "_rock_") for part in relative.parts)
    )


def rock_bytes_from_hewn(data: bytes) -> bytes:
    return data.replace(b"_hewn_", b"_rock_")


def rock_id(material: str, shape: str) -> str:
    return f"{material}_rock_{shape}"


def expected_rock_ids() -> set[str]:
    return {rock_id(material, shape) for material in MATERIALS for shape in ALL_SHAPES}


def erydon_resources(erydon_root: Path) -> Path:
    resources = erydon_root / "src" / "main" / "resources"
    metadata = resources / "fabric.mod.json"
    if not metadata.is_file() or load_json(metadata).get("id") != "erydon":
        raise RuntimeError(
            f"Not an authoritative ERYDON repository root: {erydon_root}"
        )
    return resources


def clone_hewn_json_assets() -> None:
    for root, expected in JSON_MIRRORS:
        sources = sorted(
            path for path in root.rglob("*.json") if "_hewn_" in path.name
        )
        if len(sources) != expected:
            raise RuntimeError(
                f"Expected {expected} Hewn JSON files under {root}, found {len(sources)}"
            )
        for source in sources:
            target = rock_path_from_hewn(source)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(rock_bytes_from_hewn(source.read_bytes()))


def copy_rock_textures(source_resources: Path) -> None:
    source_ctm = (
        source_resources
        / "assets"
        / "minecraft"
        / "textures"
        / "optifine"
        / "ctm"
    )
    target_ctm = (
        RESOURCES / "assets" / "minecraft" / "textures" / "optifine" / "ctm"
    )
    source_blocks = source_resources / "assets" / "erydon" / "textures" / "block"
    target_blocks = RESOURCES / "assets" / "erydon" / "textures" / "block"

    for material in MATERIALS:
        source_dir = source_ctm / f"{material}_rock"
        source_tiles = sorted(source_dir.glob("*.png"))
        if len(source_tiles) != 108:
            raise RuntimeError(
                f"Expected 108 approved Rock CTM/PBR tiles in {source_dir}, "
                f"found {len(source_tiles)}"
            )
        target_dir = target_ctm / source_dir.name
        target_dir.mkdir(parents=True, exist_ok=True)
        for source in source_tiles:
            shutil.copyfile(source, target_dir / source.name)

        for suffix in ("", "_n", "_s"):
            filename = f"{material}_rock_block{suffix}.png"
            source = source_blocks / filename
            if not source.is_file():
                raise RuntimeError(f"Missing approved ERYDON Rock texture: {source}")
            target_blocks.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target_blocks / filename)


def update_block_atlas() -> None:
    path = RESOURCES / "assets" / "minecraft" / "atlases" / "blocks.json"
    data = path.read_bytes()
    has_bom = data.startswith(b"\xef\xbb\xbf")
    text = data.decode("utf-8-sig")
    lines = text.splitlines(keepends=True)
    output: list[str] = []
    inserted: set[str] = set()
    index = 0

    while index < len(lines):
        block = lines[index : index + 5]
        is_directory_entry = (
            len(block) == 5
            and block[0].strip() == "{"
            and '"type"' in block[1]
            and '"directory"' in block[1]
            and '"source"' in block[2]
            and '"prefix"' in block[3]
            and block[4].strip() == "},"
        )
        if not is_directory_entry:
            output.append(lines[index])
            index += 1
            continue

        joined = "".join(block)
        existing_rock = next(
            (
                material
                for material in MATERIALS
                if f"optifine/ctm/{material}_rock" in joined
            ),
            None,
        )
        if existing_rock is not None:
            index += 5
            continue

        output.extend(block)
        hewn_material = next(
            (
                material
                for material in MATERIALS
                if f"optifine/ctm/{material}_hewn" in joined
            ),
            None,
        )
        if hewn_material is not None:
            output.extend(
                line.replace(
                    f"optifine/ctm/{hewn_material}_hewn",
                    f"optifine/ctm/{hewn_material}_rock",
                )
                for line in block
            )
            inserted.add(hewn_material)
        index += 5

    if inserted != set(MATERIALS):
        missing = sorted(set(MATERIALS) - inserted)
        raise RuntimeError(f"Missing Hewn atlas templates in {path}: {missing}")
    result = "".join(output).encode("utf-8")
    if has_bom:
        result = b"\xef\xbb\xbf" + result
    path.write_bytes(result)


def ctm_properties(material: str) -> str:
    tiles = " ".join(
        f"textures/optifine/ctm/{material}_rock/{index}" for index in range(36)
    )
    matches = [f"themelios:{rock_id(material, shape)}" for shape in ALL_SHAPES]
    continuation = " \\" + "\n  "
    return (
        "method=repeat\n"
        "width=6\n"
        "height=6\n"
        f"tiles={tiles}\n"
        f"matchBlocks={continuation.join(matches)}\n"
        f"matchTiles=erydon:block/{material}_rock_block\n"
        "\n"
        "faces=all\n"
        "connect=block\n"
        "innerSeams=true\n"
        "priority=10\n"
    )


def write_ctm_properties() -> None:
    root = (
        RESOURCES
        / "assets"
        / "minecraft"
        / "optifine"
        / "ctm"
        / "themelios"
    )
    for material in MATERIALS:
        target = root / f"{material}_rock" / f"a_{material}_rock_base.properties"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(ctm_properties(material), encoding="utf-8", newline="\n")


def generated_tag_counterpart(value: object) -> str | None:
    if isinstance(value, str) and "_hewn_" in value:
        return value.replace("_hewn_", "_rock_")
    return None


def update_tag_counterparts_preserving_layout(path: Path) -> None:
    data = path.read_bytes()
    has_bom = data.startswith(b"\xef\xbb\xbf")
    text = data.decode("utf-8-sig")
    document = json.loads(text)
    values = document.get("values")
    if not isinstance(values, list):
        return

    counterparts = {
        value: counterpart
        for value in values
        if (counterpart := generated_tag_counterpart(value)) is not None
    }
    if not counterparts:
        return

    generated = set(counterparts.values())
    line_pattern = re.compile(
        r'^(?P<indent>\s*)"(?P<value>[^"]+)"(?P<trailing>[ \t]*)(?P<comma>,?)(?P<ending>\r?\n?)$'
    )
    output: list[str] = []
    inserted: set[str] = set()
    for line in text.splitlines(keepends=True):
        match = line_pattern.fullmatch(line)
        if match is None:
            output.append(line)
            continue

        value = match.group("value")
        if value in generated:
            continue
        counterpart = counterparts.get(value)
        if counterpart is None:
            output.append(line)
            continue

        indent = match.group("indent")
        trailing = match.group("trailing")
        comma = match.group("comma")
        ending = match.group("ending")
        if comma:
            output.append(line)
            output.append(f'{indent}"{counterpart}"{trailing},{ending}')
        else:
            output.append(f'{indent}"{value}",{trailing}{ending}')
            output.append(f'{indent}"{counterpart}"{trailing}{ending}')
        inserted.add(counterpart)

    if inserted != generated:
        missing = sorted(generated - inserted)
        raise RuntimeError(f"Could not preserve tag layout in {path}; missing {missing}")
    result = "".join(output).encode("utf-8")
    if has_bom:
        result = b"\xef\xbb\xbf" + result
    path.write_bytes(result)


def update_tags() -> None:
    excluded: set[Path] = set()
    for kind in TAG_KINDS:
        root = RESOURCES / "data" / "themelios" / "tags" / kind
        hewn_path = root / "hewn.json"
        rock_path = root / "rock.json"
        hewn = load_json(hewn_path)
        values = hewn.get("values")
        if not isinstance(values, list) or len(values) != 297:
            raise RuntimeError(f"Expected 297 Hewn values in {hewn_path}")
        write_json(
            rock_path,
            {
                **hewn,
                "values": [
                    generated_tag_counterpart(value) or value for value in values
                ],
            },
        )
        excluded.update((hewn_path, rock_path))

    for path in sorted((RESOURCES / "data").rglob("*.json")):
        if (
            "tags" not in path.parts
            or path in excluded
            or not (set(TAG_KINDS) & set(path.parts))
        ):
            continue
        update_tag_counterparts_preserving_layout(path)


def english_rock_entries() -> dict[str, str]:
    path = RESOURCES / "assets" / "themelios" / "lang" / "en_us.json"
    document = load_json(path)
    hewn = {
        key: value
        for key, value in document.items()
        if key.startswith("block.themelios.") and "_hewn_" in key
    }
    if len(hewn) != 297:
        raise RuntimeError(f"Expected 297 English Hewn names in {path}, found {len(hewn)}")
    result: dict[str, str] = {}
    for key, value in hewn.items():
        if "Hewn" not in value:
            raise RuntimeError(f"English Hewn name does not contain 'Hewn': {key}")
        result[key.replace("_hewn_", "_rock_")] = value.replace("Hewn", "Rock")
    return result


def localized_rock_entries(
    filename: str, source_resources: Path, rock_keys: list[str]
) -> dict[str, str]:
    source = load_json(source_resources / "assets" / "erydon" / "lang" / filename)
    terms = LANGUAGE_TERMS[filename]
    result: dict[str, str] = {}

    for key in rock_keys:
        block_id = key.removeprefix("block.themelios.")
        source_key = f"block.erydon.{block_id}"
        value = source.get(source_key)
        if value is None:
            material, shape = block_id.split("_rock_", maxsplit=1)
            if shape not in CYLINDER_SHAPES:
                raise RuntimeError(f"Missing ERYDON Rock translation: {source_key}")
            base_key = f"block.erydon.{material}_rock_block"
            base = source.get(base_key)
            suffix = terms["block_suffix"]
            if not isinstance(base, str) or not base.endswith(suffix):
                raise RuntimeError(
                    f"Cannot derive localized Rock cylinder name from {base_key}"
                )
            value = base[: -len(suffix)] + " " + terms[shape]
        result[key] = value
    return result


def insert_before_messages(
    document: dict[str, str], entries: dict[str, str]
) -> dict[str, str]:
    generated_keys = set(entries)
    expanded: dict[str, str] = {}
    inserted = False
    for key, value in document.items():
        if key in generated_keys:
            continue
        if not inserted and key.startswith("message."):
            expanded.update(entries)
            inserted = True
        expanded[key] = value
    if not inserted:
        expanded.update(entries)
    return expanded


def update_languages(source_resources: Path) -> None:
    language_root = RESOURCES / "assets" / "themelios" / "lang"
    english_path = language_root / "en_us.json"
    english = load_json(english_path)
    english_entries = english_rock_entries()
    generated_keys = set(english_entries)

    expanded: dict[str, str] = {}
    for key, value in english.items():
        if key in generated_keys:
            continue
        expanded[key] = value
        rock_key = key.replace("_hewn_", "_rock_")
        if rock_key in english_entries:
            expanded[rock_key] = english_entries[rock_key]
    write_json(english_path, expanded, indent=4)

    rock_keys = list(english_entries)
    for filename in LANGUAGE_TERMS:
        path = language_root / filename
        document = load_json(path)
        entries = localized_rock_entries(filename, source_resources, rock_keys)
        write_json(path, insert_before_messages(document, entries), indent=2)
    sync_languages.synchronise(REPO_ROOT)


def generate(source_resources: Path) -> None:
    clone_hewn_json_assets()
    copy_rock_textures(source_resources)
    update_block_atlas()
    write_ctm_properties()
    update_tags()
    update_languages(source_resources)


def validate_json_mirrors() -> None:
    for root, expected in JSON_MIRRORS:
        sources = sorted(
            path for path in root.rglob("*.json") if "_hewn_" in path.name
        )
        targets = sorted(
            path for path in root.rglob("*.json") if "_rock_" in path.name
        )
        if len(sources) != expected or len(targets) != expected:
            raise RuntimeError(
                f"Unexpected Hewn/Rock JSON count under {root}: "
                f"{len(sources)}/{len(targets)}, expected {expected}/{expected}"
            )
        for source in sources:
            target = rock_path_from_hewn(source)
            if (
                not target.is_file()
                or target.read_bytes() != rock_bytes_from_hewn(source.read_bytes())
            ):
                raise RuntimeError(f"Rock JSON mirror mismatch: {target}")


def validate_textures(source_resources: Path | None) -> None:
    target_ctm = (
        RESOURCES / "assets" / "minecraft" / "textures" / "optifine" / "ctm"
    )
    target_blocks = RESOURCES / "assets" / "erydon" / "textures" / "block"
    source_ctm = (
        source_resources
        / "assets"
        / "minecraft"
        / "textures"
        / "optifine"
        / "ctm"
        if source_resources is not None
        else None
    )
    source_blocks = (
        source_resources / "assets" / "erydon" / "textures" / "block"
        if source_resources is not None
        else None
    )

    for material in MATERIALS:
        target_dir = target_ctm / f"{material}_rock"
        target_tiles = sorted(target_dir.glob("*.png"))
        if len(target_tiles) != 108:
            raise RuntimeError(
                f"Expected 108 Themelios Rock CTM/PBR tiles in {target_dir}, "
                f"found {len(target_tiles)}"
            )
        if source_ctm is not None:
            for target in target_tiles:
                source = source_ctm / target_dir.name / target.name
                if not source.is_file() or source.read_bytes() != target.read_bytes():
                    raise RuntimeError(
                        f"Themelios Rock tile differs from approved ERYDON source: {target}"
                    )

        for suffix in ("", "_n", "_s"):
            filename = f"{material}_rock_block{suffix}.png"
            target = target_blocks / filename
            if not target.is_file():
                raise RuntimeError(f"Missing Themelios Rock root texture: {target}")
            if source_blocks is not None:
                source = source_blocks / filename
                if not source.is_file() or source.read_bytes() != target.read_bytes():
                    raise RuntimeError(
                        f"Themelios Rock root texture differs from ERYDON: {target}"
                    )


def validate_ctm_properties() -> None:
    root = (
        RESOURCES
        / "assets"
        / "minecraft"
        / "optifine"
        / "ctm"
        / "themelios"
    )
    properties = sorted(root.glob("*_rock/*.properties"))
    if len(properties) != 27:
        raise RuntimeError(
            f"Expected 27 Themelios Rock CTM properties, found {len(properties)}"
        )
    for material in MATERIALS:
        path = root / f"{material}_rock" / f"a_{material}_rock_base.properties"
        data = path.read_bytes()
        if data.startswith(b"\xef\xbb\xbf") or data.decode("utf-8") != ctm_properties(material):
            raise RuntimeError(f"Invalid or BOM-prefixed Rock CTM properties: {path}")


def validate_block_atlas() -> None:
    path = RESOURCES / "assets" / "minecraft" / "atlases" / "blocks.json"
    sources = load_json(path).get("sources")
    if not isinstance(sources, list):
        raise RuntimeError(f"Missing atlas sources in {path}")
    rock_entries = [
        entry
        for entry in sources
        if isinstance(entry, dict)
        and isinstance(entry.get("source"), str)
        and entry["source"].endswith("_rock")
    ]
    expected_sources = {
        f"optifine/ctm/{material}_rock" for material in MATERIALS
    }
    actual_sources = {entry["source"] for entry in rock_entries}
    if len(rock_entries) != 27 or actual_sources != expected_sources:
        raise RuntimeError("Block atlas does not contain the exact 27 Rock directories")
    for entry in rock_entries:
        if entry.get("type") != "directory" or entry.get("prefix") != entry["source"] + "/":
            raise RuntimeError(f"Invalid Rock atlas entry: {entry}")


def validate_tags() -> None:
    excluded: set[Path] = set()
    for kind in TAG_KINDS:
        root = RESOURCES / "data" / "themelios" / "tags" / kind
        hewn_path = root / "hewn.json"
        rock_path = root / "rock.json"
        expected = [
            generated_tag_counterpart(value) or value
            for value in load_json(hewn_path).get("values", [])
        ]
        actual = load_json(rock_path).get("values")
        if len(expected) != 297 or actual != expected:
            raise RuntimeError(f"{rock_path} is not an exact 297-entry Hewn mirror")
        excluded.update((hewn_path, rock_path))

    for path in sorted((RESOURCES / "data").rglob("*.json")):
        if (
            "tags" not in path.parts
            or path in excluded
            or not (set(TAG_KINDS) & set(path.parts))
        ):
            continue
        values = load_json(path).get("values", [])
        if not isinstance(values, list):
            continue
        for value in values:
            counterpart = generated_tag_counterpart(value)
            if counterpart is not None and counterpart not in values:
                raise RuntimeError(
                    f"Missing Rock counterpart in {path}: {counterpart}"
                )


def validate_languages(source_resources: Path | None) -> None:
    sync_languages.validate_languages(REPO_ROOT)
    language_root = RESOURCES / "assets" / "themelios" / "lang"
    expected_ids = expected_rock_ids()
    english = load_json(language_root / "en_us.json")
    english_entries = english_rock_entries()

    actual_ids = {
        key.removeprefix("block.themelios.")
        for key in english_entries
    }
    if actual_ids != expected_ids:
        raise RuntimeError("English Rock names do not cover the exact 297-block family")
    for key, expected in english_entries.items():
        if english.get(key) != expected:
            raise RuntimeError(f"Incorrect English Rock name: {key}")

    rock_keys = list(english_entries)
    for filename, terms in LANGUAGE_TERMS.items():
        document = load_json(language_root / filename)
        keys = {
            key
            for key in document
            if key.startswith("block.themelios.") and "_rock_" in key
        }
        if keys != set(rock_keys):
            raise RuntimeError(
                f"{filename} does not cover the exact 297-block Rock family"
            )
        if source_resources is not None:
            expected = localized_rock_entries(filename, source_resources, rock_keys)
            for key, value in expected.items():
                if document.get(key) != value:
                    raise RuntimeError(f"Incorrect {filename} Rock name: {key}")
        elif any(terms["word"] not in document[key] for key in keys):
            raise RuntimeError(f"{filename} Rock entries do not use {terms['word']}")


def validate_java_hooks() -> None:
    ids = (JAVA_ROOT / "ModBlockIds.java").read_text(encoding="utf-8")
    required_ids = {
        "FULL_BLOCKS": "block",
        "SLABS": "slab",
        "STAIRS": "stairs",
        "WALLS": "wall",
        "POSTS": "post",
        "HORIZONTAL_SLICES": "slice_horizontal",
        "VERTICAL_SLICES": "slice_vertical",
        "LAYERS": "layer",
        "VERTICAL_LAYERS": "layer_vertical",
    }
    for field, shape in required_ids.items():
        if f"{field} = withRock(List.of(" not in ids:
            raise RuntimeError(f"Missing Rock ID expansion for {field}")
        if f'), "{shape}");' not in ids:
            raise RuntimeError(f"Missing canonical Rock shape expansion for {shape}")
    if 'material + "_rock_" + shapeName' not in ids:
        raise RuntimeError("Missing Rock ID generation helper")

    ordering = (JAVA_ROOT / "ModBlockOrdering.java").read_text(encoding="utf-8")
    required_ordering = (
        'base.equals("rock") || base.startsWith("rock_")',
        'new TextureShape(5, "rock"',
    )
    for snippet in required_ordering:
        if snippet not in ordering:
            raise RuntimeError(f"Missing Rock creative ordering hook: {snippet}")


def validate_outputs(source_resources: Path | None = None) -> None:
    validate_json_mirrors()
    validate_textures(source_resources)
    validate_ctm_properties()
    validate_block_atlas()
    validate_tags()
    validate_languages(source_resources)
    validate_java_hooks()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--erydon-root",
        type=Path,
        help="Authoritative ERYDON repository root used for approved Rock assets",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Verify committed Rock outputs without rewriting them",
    )
    args = parser.parse_args()

    source_resources = (
        erydon_resources(args.erydon_root.resolve())
        if args.erydon_root is not None
        else None
    )
    if not args.check:
        if source_resources is None:
            parser.error("--erydon-root is required unless --check is used")
        generate(source_resources)

    validate_outputs(source_resources)
    print(
        "Themelios Rock family verified: 27 materials x 11 shapes, "
        "297 blocks and 2,916 native CTM/PBR tiles."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
