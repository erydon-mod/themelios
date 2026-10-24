# Portorium branding

The polished stone plaques, metal inlays and Cinzel lettering form the shared
ERYDON family branding. Use an animated full logo in wide spaces and an animated
emblem in compact spaces wherever the surface supports animation. Use static
square emblems for external icon slots that do not support animation, including
Mod Menu and Minecraft's resource-pack selector. Preserve the artwork's aspect
ratio. Collection 32x and 64x retain separate resolution lettering.

Use purpose-sized 8-bit exports for runtime artwork. Full-resolution 16-bit
marketing masters belong with the marketing sources, outside runtime resources.
Choose the export for the surface instead of stretching a compact emblem into a
wide header or packaging a large master for a small display slot.

## Written names

Use **ERYDON**, **ERYDON Themelios**, **ERYDON Daedalon** and
**ERYDON Metrakron** for official mod names, listings and full titles.
Themelios, Daedalon and Metrakron may be shortened in descriptive prose.
Always write ERYDON in capitals. Pack names are **ERYDON Collection 32x**
and **ERYDON Collection 64x**.

The approved plaque artwork retains its engraved capital-letter typography.
Written names follow the convention above. Technical mod IDs, namespaces,
commands, URLs and artifact filenames retain their existing spelling and case.

Oliver owns the source photograph of real Portoro marble. GPT was used to
upscale that photograph; it was not generated as a new stone texture. Oliver
confirmed this provenance on 13 September 2026. The approved plaque geometry,
lettering and lighting were assembled and rendered in Blender with Codex
assistance. Exported images retain the approved appearance and texture crops.

Lettering uses Cinzel SemiBold by the Cinzel Project Authors (Natanael Gama),
under the SIL Open Font License 1.1: https://github.com/NDISCOVER/Cinzel.
These branding exports contain rendered lettering, not an embedded font file.
Existing repository asset licences and third-party notices continue to apply.

Creative-tab animation uses transparent 64-pixel square PNG frames and one
Minecraft tick per frame, without interpolation. GUI logos use a filtered
20-fps sheet drawn only while the relevant menu is visible. Mod Menu icons
and resource-pack selection icons use static square PNGs.

## Repository and runtime artwork

The README banner at `docs/branding/themelios-logo.gif` is an unchanged copy of
the approved `themelios-logo-web-black.gif` integration export. It uses the full
logo on its intentional black backing, with 142 frames at 20 fps. Documentation
artwork stays outside `src/main/resources` and is not packaged with the mod.

The static runtime icon is `assets/themelios/icon.png`. The creative-tab emblem
uses `assets/themelios/textures/item/emblem.png` and its `.mcmeta`: 38 transparent
64-pixel square frames, one Minecraft tick per frame, without interpolation.
The full-resolution static logo has no runtime consumer and is retained only
in the canonical marketing sources.

## Branding validation

Use the approved animated full logo for wide README and custom GUI headers,
and the animated emblem for compact custom GUI slots wherever animation is
supported. Keep static square emblems in standard Fabric, Mod Menu and resource-
pack selection slots. Preserve aspect ratio and the approved playback timing.
Use purpose-sized 8-bit runtime exports; keep full-resolution artwork masters
outside runtime resource directories. Documentation GIFs stay in `docs/branding`.

Run `python -B tools/audit_branding.py` for the portable source audit. Its
`config/branding-assets.json` records approved hashes, dimensions, frame counts,
timing and consumers. Update that contract only when deliberately replacing an
approved export; never resize an entire animation atlas as if it were one frame.
The matching `tools/test_branding.py` checks rejection of damaged, mistimed,
unreferenced or oversized branding assets. Gradle source/resource checks run
these gates automatically; resource-pack CI runs the same source audit.
