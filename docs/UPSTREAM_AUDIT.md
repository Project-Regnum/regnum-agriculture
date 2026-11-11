# Fabric Seasons audit

Audited 2026-11-12 before implementation. Destination was a CC0 example template using Mojang mappings, split client sources, Fabric API, and Java 21; it contained no Git metadata. A local baseline commit preserves that template.

## Verified source

- Repository: https://github.com/lucaargolo/fabric-seasons
- Branch `1.21`, tag `2.4.2-BETA+1.21`, revision `e0b2dd285e5a7f0b2850871ea7a0eb5289d0937c`.
- `gradle.properties` explicitly targets **Minecraft 1.21.1**, Yarn `1.21.1+build.3`, Loader `0.16.5`, Fabric API `0.105.0+1.21.1`. Commit `edb8c34` updated to 1.21.1; subsequent commits add starting-season support and fixes. The branch name alone is insufficient evidence.
- Root LICENSE is MPL-2.0. Metadata credits D4rkness_King and MokkaCicc. No per-file copyright headers were present in the inspected Java files. Retain attribution and mark derived files MPL-2.0.
- Upstream compiles with release 17 despite the game's Java 21 requirement. Destination uses Java 21, Mojang names, and pinned Loom 1.14.10 / Gradle 9.8.2. [Fabric Maven module metadata](https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.14.10/fabric-loom-1.14.10.module) verifies Loom targets JVM 21 and was built with Gradle 9.2; the final build verifies compatibility with the supplied Gradle version. This avoids the template Loom snapshot and newer Java-25-only Loom releases. Yarn source is not directly compilable here.

## Architecture and reuse decisions

| Area | Upstream implementation | Adaptation |
| --- | --- | --- |
| Calendar | `FabricSeasons.getCurrentSeason`, `utils/Season`: four sequential seasons, modulo world day time, unequal configurable lengths, starting-season and system-clock modes | Derive small equal-length calendar and enum; use safe modular arithmetic. Keep world-time progression; omit real-time/hemisphere modes. |
| Persistence | Season inferred from saved vanilla time; commands alter all world clocks. Separate persistent states track placed/replaced snow and ice | Add overworld SavedData anchors for manual season changes without altering daylight; preserve phase when length changes. |
| Server/client | Server config and crop JSON payloads; clients compute from synchronized world time. Client config temporarily replaced | Send explicit dimension-specific authoritative calendar snapshots on join, dimension change, commands, reload, and periodic correction; separate client visuals from server config. |
| Colors | Grass/foliage colormaps, biome/hardcoded JSON, caches, biome and block color mixins; rebuild renderer on season changes | Adapt biome color interception, blend vanilla/resource-pack colors using original numeric tints, quantized gradual transitions. No upstream image assets. |
| Weather | `WorldViewMixin` overwrites biome lookup and mutates shared biome weather; temperature bands in `getSeasonWeather` and winter exceptions in `ModConfig`; snow/ice replacement and melting states | Derive temperature-band calculation, scope snow eligibility and precipitation to actual level/position. Do not mutate biome registry instances, overwrite lookup, alter worldgen, or port destructive vegetation replacement. |
| Crops | `VanillaFertilizablesMixin`, `FertilizableUtil` recursively invoke randomTick/grow with a global guard; datapack multipliers and greenhouse cache | Replace with one vanilla probability interception for four crops; bounded probability, no additional ticks, no bonemeal changes. Config crop overrides. |
| Config | Gson JSON, broad mutable singleton, safe fallback on parse failure; no thorough field validation | Immutable validated snapshot; partial invalid values fall back individually, malformed reload retains last valid config. |
| Commands | `/season` query/set/skip, level-2 mutations via TimeCommand; client debug commands | Namespaced `/regnumagriculture`; translated EN/UK messages; persistent anchor mutations. |
| Mixins | 26 mixins including models, tooltips, animals, biome weather, snow, worldgen | Keep only crop probability, scoped snowfall/precipitation, and client biome color hooks. |
| Localization | EN and other languages; no Ukrainian at this revision | New EN/UK translations; no custom tooltips. |
| Dependencies | Fabric API required; Mod Menu and REI development runtime only; publishing plugins and access widener | Require only Loader and Fabric API; no REI, Mod Menu, access widener, publishing credentials or plugins. |

## Compatibility findings

- Mojang names: `ServerLevel`, `SavedData`, `ResourceLocation`, `StreamCodec`, `CustomPacketPayload`; upstream Yarn equivalents require adaptation.
- Shared biome mutation risks dimension and integrated-server leakage; avoid it entirely.
- Recursive extra ticks are incompatible with Regnum World's independent extra-tick guard. See REGNUM_WORLD_COMPATIBILITY.md for the audited implementation and conservative boundary.
- Nether, End and unknown custom dimensions must remain neutral unless explicitly enabled and naturally support sky/weather.
- Upstream PNG colormaps and bundled resource packs have no asset-specific permission statement established by this audit. None are imported. New numeric tints retain resource-pack base colors.
- Upstream `seasons` registration, models, greenhouses, extra crops, bonemeal handling, debugging UI, compatibility popups, vegetation replacement and worldgen hooks are deliberately excluded.

This is a selective source adaptation, not a wholesale repository import. Derived source headers and THIRD_PARTY_NOTICES.md identify provenance. Runtime verification and its limits are recorded separately in VERIFICATION.md.
