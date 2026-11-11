# Third-party notices

## Fabric Seasons

- Source: https://github.com/lucaargolo/fabric-seasons
- Branch: `1.21`
- Release tag: `2.4.2-BETA+1.21`
- Revision: `e0b2dd285e5a7f0b2850871ea7a0eb5289d0937c`
- Authors credited by upstream: D4rkness_King, MokkaCicc, and Fabric Seasons contributors.
- License: Mozilla Public License, version 2.0 (SPDX `MPL-2.0`). The full license is included in [LICENSE](LICENSE).

Upstream Java files inspected did not contain individual copyright headers. Upstream authors retain their rights. Regnum Agriculture does not claim exclusive ownership of upstream work.

Selective adaptations, each identified in its source header:

| Regnum Agriculture source | Upstream origin | Changes |
| --- | --- | --- |
| `season/Season.java` | `utils/Season.java` | Package, autumn name, small internal enum, translation namespace; remove formatting/config coupling. |
| `season/SeasonCalendar.java` | `FabricSeasons.getCurrentSeason` | Extract arithmetic, equal configurable lengths, overflow-safe modulo, one-based day/progress, persisted-anchor support. |
| `climate/ClimateService.java` | `FabricSeasons.getSeasonWeather`, `utils/ModConfig.java` winter snow exceptions | Retain numeric temperature bands, remove registry mutation and dry-biome precipitation forcing; world-scoped policy. |
| `networking/SeasonPayload.java` | `payload/ConfigSyncPacket.java` | Fabric custom payload pattern; Mojang mappings, explicit typed calendar/policy snapshot instead of global JSON config replacement. |
| `client/SeasonalColors.java`, `client/mixin/BiomeVisualMixin.java` | `mixin/BiomeMixin.java` and client renderer refresh architecture | Separate client source set, vanilla/resource-pack base color blending with new numeric palettes, gradual quantized transitions, scoped precipitation. |

These files and their modifications are available under MPL-2.0. All new project Java source is also offered under MPL-2.0. The project builds a `-sources.jar` alongside the distributable JAR; distributors must make the corresponding covered source available and preserve these notices in accordance with MPL-2.0. The canonical source location is https://github.com/Project-Regnum/regnum-agriculture; until the repository is published, distribute the matching sources JAR with the binary.

No upstream textures, colormap PNGs, sounds, icons, model assets, resource packs, or localization files are copied. The audit did not establish separate asset permissions, and this project makes no blanket claim about their licenses. Seasonal numeric tints and English/Ukrainian text are newly authored. Minecraft's own assets remain provided by Minecraft or users' resource packs and are not redistributed here.

## Original destination template

The original template was supplied under CC0-1.0. Its original license text is preserved at [docs/TEMPLATE_LICENSE_CC0.txt](docs/TEMPLATE_LICENSE_CC0.txt). The Gradle wrapper and unchanged template infrastructure retain their original terms. The baseline commit preserves the original files.

## Dependencies

Fabric Loader, Fabric API, Minecraft, Gson, JUnit, and their transitive dependencies are obtained through Gradle under their respective licenses. JUnit is test-only. This project does not bundle Minecraft, Regnum World, REI, Mod Menu, or upstream Fabric Seasons. Fabric Seasons must not be installed alongside this adaptation; metadata rejects that combination to avoid two seasonal engines acting on the same world.
