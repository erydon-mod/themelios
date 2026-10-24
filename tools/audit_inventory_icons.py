"""Audit Themelios inventory textures against each resource set's own artwork.

The icon parents and geometry stay authored. Plain/patterned previews use their
own CTM tile 0; composed metal-inlay previews keep their dedicated artwork.
Use --resources to check a physical optional pack against this mod's item models.
"""
from __future__ import annotations

import argparse
from functools import lru_cache
import json
from pathlib import Path
import re
import shutil

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = REPO_ROOT / "src/main/resources"
COMPOSED_INLAY = re.compile(r"^[a-z0-9_]+_block_(?:bronze|silver)(?:trim|guilloche|quatrefoil|rose)$")


@lru_cache(maxsize=None)
def model_textures(model_id: str) -> dict[str, str]:
    namespace, path = model_id.split(":", 1) if ":" in model_id else ("minecraft", model_id)
    source = MODEL_ROOT / "assets" / namespace / "models" / (path + ".json")
    if not source.is_file():
        if namespace == "minecraft":
            return {}
        raise ValueError(f"Missing inventory model parent: {model_id}")
    model = json.loads(source.read_text(encoding="utf-8-sig"))
    textures = dict(model_textures(model["parent"])) if model.get("parent") else {}
    textures.update(model.get("textures", {}))
    return textures


def icon_textures() -> tuple[int, list[str]]:
    used: set[str] = set()
    items = 0
    for model in sorted((MODEL_ROOT / "assets/themelios/models/item").glob("*.json")):
        if model.stem == "emblem":
            continue
        textures = model_textures("themelios:item/" + model.stem)
        found = set()
        for texture in textures.values():
            visited = set()
            while texture.startswith("#"):
                if texture in visited:
                    raise ValueError(f"Cyclic inventory texture in {model.stem}")
                visited.add(texture)
                texture = textures[texture[1:]]
            if texture.startswith("erydon:block/"):
                found.add(texture)
        if not found:
            raise ValueError(f"No representative stone texture for {model.stem}")
        used.update(found)
        items += 1
    if not used:
        raise ValueError("No Themelios inventory textures found")
    return items, sorted(used)


def pixels(path: Path) -> tuple[tuple[int, int], bytes]:
    with Image.open(path) as image:
        return image.size, image.convert("RGBA").tobytes()


def audit(resources: Path, write: bool = False) -> dict[str, object]:
    items, textures = icon_textures()
    changes: list[tuple[Path, Path]] = []
    composed = 0
    plain = 0
    for texture in textures:
        name = texture.removeprefix("erydon:block/")
        icon = resources / "assets/erydon/textures/block" / (name + ".png")
        icon_pixels = pixels(icon)
        if COMPOSED_INLAY.fullmatch(name):
            composed += 1
            continue
        if name.endswith("_block_aged"):
            family = name.removesuffix("_block_aged") + "_aged"
        elif name.endswith("_block"):
            family = name.removesuffix("_block")
        else:
            raise ValueError(f"Unclassified inventory texture: {texture}")
        tile = resources / "assets/minecraft/textures/optifine/ctm" / family / "0.png"
        if icon_pixels != pixels(tile):
            changes.append((icon, tile))
        plain += 1
    if changes and not write:
        raise ValueError("Inventory previews differ from their own CTM tile 0: "
                         + ", ".join(icon.name for icon, _ in changes))
    for icon, tile in changes:
        shutil.copyfile(tile, icon)
        if icon.read_bytes() != tile.read_bytes():
            raise ValueError(f"Inventory texture copy failed: {icon.name}")
    return {"status": "ok", "item_models": items, "icon_textures": len(textures),
            "ctm_previews": plain, "preserved_composed_inlays": composed,
            "updated": len(changes)}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--resources", type=Path, default=MODEL_ROOT,
                        help="Physical resource root to audit; defaults to the native mod resources.")
    parser.add_argument("--write", action="store_true",
                        help="Copy mismatched plain/patterned icons from this resource set's own tile 0.")
    args = parser.parse_args()
    print(json.dumps(audit(args.resources.resolve(), args.write), sort_keys=True))


if __name__ == "__main__":
    main()
