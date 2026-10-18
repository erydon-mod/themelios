#!/usr/bin/env python3
"""Complete canonical catalogue translations and verify all three language files.

Existing translations are retained. New block names use the shared terminology in
config/language-terms.json; registry aliases remain search terms, never duplicate
display-name keys. This tool has no dependency on another mod's checkout.
"""
from __future__ import annotations

import argparse
import csv
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANGUAGES = ("en_us", "de_de", "es_es")
TAG_NAMESPACES = ("themelios", "material", "color", "style")
PREFIX = "block.themelios."
FORMS = (
    "slice_horizontal", "slice_vertical", "layer_vertical", "cylinder_small",
    "cylinder_large", "cornice_small", "cornice_large", "block", "slab", "stairs",
    "wall", "post", "layer",
)
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdif]")


def read(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def localized_name(path: str, language: str, terms: dict) -> str:
    if language not in ("de_de", "es_es"):
        raise ValueError("Unsupported generated language: " + language)
    form = next((form for form in FORMS if path.endswith("_" + form)), None)
    if form is None:
        raise ValueError("Unclassified block form: " + path)
    family = path[: -len(form) - 1]
    local = terms["languages"][language]
    materials = terms["materials"]
    if "_weave_" in family:
        stones, finish = family.split("_weave_", 1)
        components = stones.split("_")
        if len(components) != 2 or any(stone not in materials for stone in components):
            raise ValueError("Unclassified weave materials: " + path)
        material = f" {local['and']} ".join(materials[stone] for stone in components)
        descriptor = local["patterns"].get("weave_" + finish)
    else:
        material_id, separator, descriptor_id = family.partition("_")
        if material_id not in materials:
            raise ValueError("Unclassified material: " + path)
        material = materials[material_id]
        if not separator:
            descriptor = ""
        elif descriptor_id in local["finishes"]:
            descriptor = local["finishes"][descriptor_id]
        elif descriptor_id in local["patterns"]:
            descriptor = local["patterns"][descriptor_id]
        else:
            motif, separator, metal = descriptor_id.rpartition("_")
            if not separator or motif not in local["motifs"] or metal not in local["metals"] or form != "block":
                raise ValueError("Unclassified block finish: " + path)
            return " ".join((material, local["metals"][metal], local["motifs"][motif], local["inlay"]))
    if descriptor is None:
        raise ValueError("Unclassified weave finish: " + path)
    return " ".join(word for word in (material, descriptor, local["forms"][form]) if word)


def complete(english: dict, localized: dict, language: str, terms: dict) -> dict:
    result = dict(localized)
    extra = set(result) - set(english)
    if extra:
        raise ValueError(f"Unexpected {language} language keys: {sorted(extra)[:5]}")
    missing = set(english) - set(result)
    ui_missing = {key for key in missing if not key.startswith(PREFIX)}
    if ui_missing:
        raise ValueError(f"Translate new UI/search text explicitly in {language}: {sorted(ui_missing)}")
    for key in english:
        if key in missing:
            result[key] = localized_name(key.removeprefix(PREFIX), language, terms)
    return result


def item_tag_keys(root: Path) -> dict[str, str]:
    version = (root / "gradle.properties").read_text(encoding="utf-8")
    folder = "item" if "minecraft_version=1.21.1" in version else "items"
    result = {}
    for namespace in TAG_NAMESPACES:
        base = root / "src/main/resources/data" / namespace / "tags" / folder
        for path in sorted(base.rglob("*.json")):
            name = path.relative_to(base).as_posix().removesuffix(".json")
            result[f"tag.item.{namespace}.{name.replace('/', '.')}"] = name
    return result


def localized_tag_name(name: str, language: str, terms: dict) -> str:
    if name in terms["materials"]:
        return terms["materials"][name]
    if name.endswith("_weave") and name.removesuffix("_weave") in terms["materials"]:
        material = terms["materials"][name.removesuffix("_weave")]
        weave = terms["tag_groups"]["weave"][language]
        return material + " " + weave
    try:
        return terms["tag_groups"][name][language]
    except KeyError as error:
        raise ValueError(f"Translate exported item tag explicitly: {language}:{name}") from error


def search_keys(root: Path) -> set[str]:
    source = root / "src/main/java/com/oliver/erydonthemelios/client/migration/IdAliasSearchVocabulary.java"
    text = source.read_text(encoding="utf-8")
    groups = text.split("private enum AliasGroup", 1)[1].split("private final String key", 1)[0]
    names = re.findall(r'^\s*[A-Z_]+\("([a-z_]+)"', groups, re.MULTILINE)
    if not names:
        raise ValueError("No live search synonym groups found")
    return {"search.themelios." + name for name in names}


def validate_documents(documents: dict, registered: set[str], aliases: set[str],
                       expected_search: set[str], expected_tags: set[str] = frozenset()) -> None:
    english = documents["en_us"]
    expected_blocks = {PREFIX + path for path in registered}
    if {key for key in english if key.startswith(PREFIX)} != expected_blocks:
        raise ValueError("English block names do not match the registered catalogue")
    for language in LANGUAGES:
        document = documents[language]
        missing, extra = set(english) - set(document), set(document) - set(english)
        if missing or extra:
            raise ValueError(f"Language key mismatch in {language}: missing={sorted(missing)[:5]}, extra={sorted(extra)[:5]}")
        actual_search = {key for key in document if key.startswith("search.themelios.")}
        if actual_search != expected_search:
            raise ValueError("Language/search vocabulary mismatch in " + language)
        if {key for key in document if key.startswith("tag.item.")} != expected_tags:
            raise ValueError("Exported item-tag label mismatch in " + language)
        if {PREFIX + alias for alias in aliases}.intersection(document):
            raise ValueError("Legacy alias became a display-name key in " + language)
        for key, value in document.items():
            if not isinstance(value, str) or not value.strip():
                raise ValueError(f"Empty/non-text translation: {language}:{key}")
            if "\ufffd" in value or re.search(r"[\x00-\x08\x0b\x0c\x0e-\x1f]|\\x[0-9a-fA-F]{2}|[\u00c2\u00c3][\u0080-\u00bf]", value):
                raise ValueError(f"Corrupt translation text: {language}:{key}")
            if PLACEHOLDER.findall(value) != PLACEHOLDER.findall(english[key]):
                raise ValueError(f"Translation placeholder mismatch: {language}:{key}")
            if key.startswith("search.") and any(not token.strip() for token in value.split("|")):
                raise ValueError(f"Empty search term: {language}:{key}")


def validate_languages(root: Path = ROOT) -> dict:
    assets = root / "src/main/resources/assets/themelios"
    documents = {language: read(assets / "lang" / (language + ".json")) for language in LANGUAGES}
    registered = {path.stem for path in (assets / "blockstates").glob("*.json")}
    manifest = root / "src/main/resources/data/themelios/id_migration.tsv"
    with manifest.open(encoding="utf-8-sig", newline="") as stream:
        aliases = {row["old_path"] for row in csv.DictReader(stream, delimiter="\t")}
    validate_documents(documents, registered, aliases, search_keys(root), set(item_tag_keys(root)))
    return {"registered_blocks": len(registered), "language_keys": len(documents["en_us"]),
            "languages": list(LANGUAGES), "search_groups": len(search_keys(root)),
            "exported_item_tags": len(item_tag_keys(root))}


def synchronise(root: Path = ROOT, *, check: bool = False) -> dict:
    assets = root / "src/main/resources/assets/themelios"
    terms = read(root / "config/language-terms.json")
    english = read(assets / "lang/en_us.json")
    tags = item_tag_keys(root)
    for language in LANGUAGES:
        path = assets / "lang" / (language + ".json")
        current = read(path)
        updated = dict(current)
        for key, name in tags.items():
            updated.setdefault(key, localized_tag_name(name, language, terms))
        if language == "en_us":
            english = updated
        else:
            updated = complete(english, updated, language, terms)
        if updated != current:
            if check:
                raise ValueError("Missing catalogue translations in " + language)
            path.write_text(json.dumps(updated, ensure_ascii=False, indent=4 if language == "en_us" else 2) + "\n", encoding="utf-8")
    return validate_languages(root)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    print(json.dumps(synchronise(check=args.check), ensure_ascii=False))


if __name__ == "__main__":
    main()
