# feat: seasonal agriculture foundation (v0.1.0)

## Result

Add the independently buildable Fabric 1.21.1 seasonal foundation for Regnum Agriculture. Four server-authoritative seasons affect vanilla crops, biome colors, and scoped snowfall. Manual season changes persist without changing daylight. Enabled surface dimensions follow an overworld calendar; Nether, End, and unknown dimensions remain neutral.

## Provenance and scope

Fabric Seasons source: https://github.com/lucaargolo/fabric-seasons, branch `1.21`, tag `2.4.2-BETA+1.21`, revision `e0b2dd285e5a7f0b2850871ea7a0eb5289d0937c`. Its properties explicitly target Minecraft 1.21.1. See `docs/UPSTREAM_AUDIT.md` for dependency/history verification.

Adapt the season enum/calendar calculations, temperature bands and winter snow exceptions, Fabric custom-payload pattern, biome color interception, and renderer-refresh architecture. Replace upstream global configuration replacement, shared biome weather mutation, and recursive crop ticking with isolated services and scoped hooks.

Omit upstream greenhouse handling, extra crops, bonemeal interception, animal behavior, debug/UI/tooltips, model overrides, asset packs, worldgen hooks, vegetation replacement, and ice replacement/melting. Remove template example mixins, datagen, and placeholder assets. Only Fabric Loader and Fabric API are required; REI, Mod Menu, publishing plugins, and the upstream access widener are excluded.

## Architecture

- `SeasonCalendar`: pure bounded arithmetic, one-based day and fractional progress.
- `SeasonState`: overworld SavedData anchors with phase-preserving season-length changes.
- `SeasonManager` / `SeasonEvents`: server lifecycle, dimension policy, transitions, manual changes, and synchronization.
- `ServerConfig` / `ConfigStore`: immutable validated policy, per-field defaults, malformed-reload retention, and per-crop overrides.
- `SeasonPayload` / client state: typed dimension/calendar/policy snapshots; separate client visual settings.
- `ClimateService` / seasonal snow state: upstream temperature bands, scoped snowfall, ownership-aware thaw; no shared registry mutation or worldgen changes.
- `CropGrowthService`: position-aware seasonal policy for the four vanilla crops.
- Namespaced operator commands with English/Ukrainian localization.

## Growth and Regnum World

Modify the single vanilla crop growth probability to `min(1, seasonalFactor / vanillaBound)`. Keep vanilla soil/light/age checks and beetroot's independent gate. No additional or recursive random ticks; high rates saturate at one age per opportunity. Bonemeal remains vanilla.

Inspect Regnum World revision `aeaf32470e2b42d1aaba7f8333cb14fbbd4c354b`. Its current crop hook recursively schedules extra ticks. When `regnum-world` is loaded, Agriculture leaves crop ticking entirely to it while retaining the seasonal calculation service and all other features. This prevents stacking; combined seasonal/regional growth is deliberately deferred. `docs/REGNUM_WORLD_COMPATIBILITY.md` defines the provider/ownership changes needed in Regnum World. Its tracked source was not modified.

## Licensing

Derived files carry MPL-2.0 identifiers and upstream provenance. Root LICENSE contains MPL-2.0; the original CC0 template license is retained. `THIRD_PARTY_NOTICES.md` identifies every adapted source area, authors, source URL and revision. No upstream image, model, sound, or translation assets are imported. Distributable and sources JARs carry license notices; publish the matching sources alongside the binary.

## Validation

- `./gradlew clean build` under Java 21: passed; 13 JUnit tests passed.
- `./gradlew runGameTest`: all 7 required tests passed on a dedicated server.
- GameTests with the actual Regnum World JAR and regional factor 2.0: all 7 passed; seasonal changes leave regional growth rolls identical.
- Normal dedicated-server startup, command execution, save/stop, and actual process restart retaining winter/day one: passed.
- Packaged classes target Java 21; production mixin targets are remapped; EN/UK keys and license resources verified.

See `docs/VERIFICATION.md` for exact commands, fixtures, and limits. Rendered visual appearance and live multiplayer delivery were not runtime-tested; no unexecuted checks are claimed.

## Limitations and 0.2.0

Equal season lengths; enabled dimensions share the overworld calendar; no seasonal ice, real-world clock, global snow scans, or combined Regnum World growth calculation. Vanilla/worldgen snow and fixed-color tree handlers retain vanilla behavior. Rendered visuals, renderer mods, and live multiplayer transitions still need client verification.

For 0.2.0, agree the Regnum World growth ownership handshake, finish multiplayer/resource-pack visual verification, and then consider crop-specific preferences and a modest first crop expansion. No new crop, cooking, fertility, irrigation, greenhouse, economy, kingdom, warfare, or worldgen features are part of this PR.

