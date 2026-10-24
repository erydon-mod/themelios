# ERYDON Themelios for Fabric 1.20.1

![ERYDON Themelios animated logo](docs/branding/themelios-logo.gif)

ERYDON Themelios provides ERYDON's compact catalogue of basic architectural shapes
for Minecraft 1.20.1.

Version 1.6.0 uses native 16x textures. The unified ERYDON Collection 32x and
64x resource packs support ERYDON Themelios, ERYDON, and Daedalon together.
Connected textures are rendered by the built-in Synapheia engine; Continuity
is neither required nor bundled.

Install with Fabric Loader 0.16.10 or newer and Fabric API for Minecraft
1.20.1, using Java 17.
Sodium and Iris are optional. If you use Sodium 0.5.x, also install a compatible
Indium release so connected textures work.

This repository is source-available under Oliver's restricted licence. It is
not open-source software; see `LICENSE`. Source build-tool licences and
attributions are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
Publishing this source does not change those licence terms.

Source and issue tracking are available at
[erydon-mod/themelios](https://github.com/erydon-mod/themelios).
This branch targets Minecraft 1.20.1. Choose
[Minecraft 1.20.1 (`main`)](https://github.com/erydon-mod/themelios/tree/main) or
[Minecraft 1.21.1 (`codex/fabric-1.21.1`)](https://github.com/erydon-mod/themelios/tree/codex/fabric-1.21.1)
for the matching source.

## Development checks

Use Java 17 and Python 3 to run source validation without packaging a JAR:

```text
python -m pip install Pillow
bash ./gradlew --no-daemon check
bash ./gradlew --no-daemon runGameTest
```

`check` verifies the catalogue, translations, loot resources, ID aliases and
texture packaging. `runGameTest` tests real server loot in an isolated
`build/gametest` world, including stacked quantities and a data-pack override;
it writes `build/reports/gametest/loot.xml`. The test mod is separate from
production sources and does not open player saves.

On Windows PowerShell, use `.\gradlew.bat` instead of `bash ./gradlew`.
GitHub Actions runs these same source and server checks without packaging a JAR.
To build the release JAR after validation, run:

```text
bash ./gradlew --no-daemon remapJar
```

The output is
`build/libs/themelios-fabric-mc1.20.1-compat2-1.6.0.jar`.
