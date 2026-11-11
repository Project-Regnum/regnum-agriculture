# Regnum Agriculture

Regnum Agriculture **0.1.0** is the seasonal foundation for Project Regnum's future agriculture and cooking expansion. It selectively adapts the MPL-2.0 Fabric Seasons engine for **Minecraft Java 1.21.1 / Fabric / Java 21**.

Four seasons change crop growth, grass and deciduous foliage colors, and snowfall. The server owns the calendar and growth policy. Wheat, carrots, potatoes, and beetroot are supported; no new crops, cooking, soil, irrigation, world generation, or economy systems are added.

## Installation

1. Install Minecraft 1.21.1 and Fabric Loader **0.16.5 or newer** with Java 21. This build is verified with Loader 0.20.1.
2. Install Fabric API **0.105.0+1.21.1 or newer for 1.21.1**. This build uses 0.116.18+1.21.1.
3. Put `regnum-agriculture-0.1.0.jar` from `build/libs/` in the server and clients' `mods/` directories. Do not install the `-sources.jar` as a mod.
4. Start the server once to generate `config/regnum_agriculture.json`. Client visual preferences are generated separately on client startup.

Regnum World is optional. Fabric Seasons (`seasons`) cannot be installed together with this mod: metadata rejects the duplicate seasonal engine. No Mod Menu, REI, or other configuration library is required. Fabric clients without Agriculture may connect, but receive vanilla visuals; installing the mod on clients is recommended for seasonal visuals and translated feedback.

## Calendar and dimensions

The default season length is **24 Minecraft days**, starting with spring, then summer, autumn, winter. A Minecraft day is 24,000 daylight ticks. Sleeping and `/time` changes affect progression; disabling daylight progression also stops the calendar. Time jumps emit a transition from the last observed season to the resulting season rather than replaying every skipped season.

The calendar uses overworld day time and saved anchors in `world/data/regnum_agriculture_seasons.dat`. Manual season changes reset the day to one without changing daylight. Restarts retain the season. Changing season length preserves the current season and fractional progress, including when edited while the server is stopped. Existing worlds initially derive their season from their existing day time.

Only `minecraft:overworld` is seasonal by default. Additional dimensions must be explicitly listed and have a natural dimension type, skylight, and no ceiling. Enabled surface dimensions follow the overworld calendar. Nether, End, and unlisted dimensions retain vanilla growth, weather, and colors; their query returns neutral spring/day one with `isSeasonal` false.

## Configuration

Server file: `config/regnum_agriculture.json`.

```json
{
  "seasonLengthDays": 24,
  "enableSeasonalColors": true,
  "enableSeasonalWeather": true,
  "enableSeasonalCropGrowth": true,
  "cropGrowthMultipliers": {
    "spring": 1.15,
    "summer": 1.25,
    "autumn": 0.85,
    "winter": 0.35
  },
  "cropOverrides": {
    "minecraft:wheat": { "winter": 0.5 }
  },
  "seasonalDimensions": ["minecraft:overworld"]
}
```

`cropOverrides` is empty by default; the wheat entry above is an example. Missing override seasons inherit the global factors. Overrides currently apply only to the four supported crops. Position-aware service methods provide a small extension point for later crop preferences and environmental mechanics.

Season length must be an integer from 1 to 36,500. Multipliers must be finite numbers from 0 to 16. Invalid individual values receive safe defaults and log a warning. Missing fields use defaults. A malformed file on reload leaves the current policy active and preserves the file for correction; on first startup the safe defaults remain active. Reload does not rewrite user files. Zero stops natural growth; bonemeal remains vanilla.

Client file: `config/regnum_agriculture-client.json`.

```json
{
  "enableSeasonalColors": true,
  "smoothTransitions": true
}
```

Client preferences are loaded at client startup. Server policy can disable seasonal colors; client preferences cannot enable server-disabled effects or change crop growth. Visual snapshots are sent on join/dimension change, season changes, config reload, and every five seconds.

## Commands

| Command | Permission | Effect |
| --- | --- | --- |
| `/regnumagriculture season` | Everyone | Show the season and day, or report an inactive dimension. |
| `/regnumagriculture season set <spring\|summer\|autumn\|winter>` | Operator level 2 | Set the shared calendar to day one of a season. |
| `/regnumagriculture season next` | Operator level 2 | Advance to day one of the next season. |
| `/regnumagriculture reload` | Operator level 2 | Validate and reload server configuration. |

Season mutation commands require an enabled seasonal dimension. Feedback is localized in English (`en_us`) and Ukrainian (`uk_ua`). The dedicated console uses the server's standard English fallback. The command root is separate from Regnum Nations and Regnum World commands.

## Growth, visuals, and weather

Standalone growth modifies one vanilla probability: `p = min(1, seasonalMultiplier / vanillaRandomBound)`. Vanilla light, farmland, hydration, maximum age, and beetroot's additional growth gate remain intact. At multiplier one, the original RNG call is preserved. Growth advances at most one age per vanilla opportunity; high multipliers saturate. No extra random ticks or recursive tick calls are made.

With **Regnum World installed**, Agriculture disables its crop tick modification to prevent unintended stacking with that mod's recursive tick mechanism. Seasons and other effects remain available. Combined seasonal and regional crop rates require changes in Regnum World; see [the compatibility audit](docs/REGNUM_WORLD_COMPATIBILITY.md).

Seasonal tints blend with vanilla/resource-pack biome colors. Changes blend from the previous season during the first quarter of the new season in sixteen steps. Color values are cached once per client tick; mesh/tint-cache refreshes occur only when a step changes, at most once per two seconds. Resource-pack base colormaps remain effective. Fixed-color evergreen/birch block handlers retain their standard colors.

Weather uses the upstream temperature bands and winter snow exceptions for plains, sunflower plains, and stony peaks. It changes eligible rain to snow while respecting dry biomes and dimension policy. Natural precipitation can accumulate seasonal snow in air while retaining vanilla light, support, and snow-accumulation checks. Only tracked seasonal snow thaws as climate warms, during vanilla precipitation-column ticks even when it is not raining. Snow placed or stacked by players relinquishes seasonal tracking. Vanilla/worldgen snow is preserved. No vegetation replacement, additional ice formation, forced rainstorms, or worldgen changes are introduced.

## Development

Use Java 21 for the supplied Gradle 9.8.2/Loom 1.14.10 tooling, compilation, tests, and Minecraft. Set `JAVA_HOME` to your Java 21 JDK if your system default is a newer Java release; Gradle's toolchain discovery selects Java 21 for the game and compiled classes. If it is not discovered, set `org.gradle.java.installations.paths` in your user Gradle properties. Compiled classes target Java 21.

```bash
./gradlew clean build
./gradlew runGameTest
./gradlew runClient
./gradlew runServer
```

Run build and game-launch commands separately, as shown above. Launch tasks opt out of configuration caching so Loom regenerates launch arguments after a clean build.

`build` runs the isolated JUnit calculations/configuration tests and emits the distributable and sources JARs. `runGameTest` is the configured headless dedicated-server test task; it exercises disk persistence, packets, dimensions, command permissions, live config reload, crop growth, non-recursion, and snow/thaw. Reports: `build/test-results/test/` and `build/gametest/reports/gametest-results.xml`.

To check with an independently built Regnum World JAR:

```bash
./gradlew runGameTest -PregnumWorldJar=/absolute/path/regnum-world.jar
```

For predictable flat-world test fixtures, set `block_underground_crop_growth` and `mountain_altitude_debuff_enabled` to false in the **test run's** `build/gametest/config/regnum_world.json` before launching, and set `biome_growth_modifiers` explicitly (`{}` gives neutral regional rates). The recorded compatibility run also exercises a plains regional factor of 2.0. This does not change production configs or the Regnum World repository.

Internal services are `SeasonManager`, `SeasonCalendar`, `SeasonState`, `SeasonEvents.TRANSITION`, `ClimateService`, and `CropGrowthService`. Calls that access worlds or managers belong on the server thread. There is no additional public API dependency or large unused framework.

## Attribution and limitations

Derived from [Fabric Seasons](https://github.com/lucaargolo/fabric-seasons/tree/e0b2dd285e5a7f0b2850871ea7a0eb5289d0937c), tag `2.4.2-BETA+1.21`, by D4rkness_King and contributors. Covered source and modifications remain under MPL-2.0. No upstream image, model, sound, or translation assets are included. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), [LICENSE](LICENSE), and [UPSTREAM_AUDIT.md](docs/UPSTREAM_AUDIT.md).

Limitations: equal season lengths; shared overworld calendar for enabled surface dimensions; no real-world clock/hemisphere mode; no seasonal ice or global snow scanning; no combined Regnum World growth integration yet. Snow in unloaded chunks waits until normal column ticks resume. Renderer replacement mods have not been verified. Live multiplayer networking and rendered visual appearance still require manual client verification. Executed tests and remaining checks are listed in [VERIFICATION.md](docs/VERIFICATION.md).
