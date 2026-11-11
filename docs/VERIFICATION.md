# Verification record

Executed locally on 2026-11-12 (Europe/Kyiv). Target: Minecraft **1.21.1**, Fabric Loader **0.20.1**, Fabric API **0.116.18+1.21.1**, Temurin **21.0.12.1**. Final tooling: Gradle **9.8.2**, Loom **1.14.10**. `JAVA_HOME` was set to the installed Java 21 JDK because this workstation defaults to Java 25.

## Executed checks

| Check | Result |
| --- | --- |
| `./gradlew clean build` with Java 21 | Passed; distributable and sources JARs generated. |
| JUnit suite | **13 passed**, zero failures/errors. |
| `./gradlew runGameTest` with Java 21 | **7 required GameTests passed** on a real headless dedicated game-test server. |
| Regnum World compatibility GameTests | **7 required GameTests passed** with the actual Regnum World JAR and a plains regional factor of 2.0. |
| Normal `runServer --console=plain --no-configuration-cache --args='nogui'` | Started Minecraft 1.21.1 successfully, reached `Done`, accepted commands, saved all dimensions, and stopped cleanly. |
| Actual server-process restart | Set winter, queried winter/day 1, saved, stopped; restarted a new server process and queried **winter/day 1 of 24** again; clean shutdown. |
| Packaged JAR inspection | Mod ID/version correct; main/client mixin configs present; production mixin targets remapped to intermediary names; all classfiles major **65** (Java 21); no template example/test classes. |
| Translation/notice inspection | EN/UK have matching nine keys; JSON parses; LICENSE, THIRD_PARTY_NOTICES, and original CC0 text included. |
| `git diff --check` | Passed. |

## Coverage against requested cases

| Requirement | Evidence |
| --- | --- |
| Initial season, progression, boundaries, configurable length | Pure calendar tests cover spring/day one, every boundary across three years, negative time, extreme long values, length resizing; live GameTest advances world time into summer/day two. |
| Persistence after restart | SavedData disk round-trip in a fresh DimensionDataStorage instance, plus actual stop/start of a normal server retaining manual winter. |
| Manual changes and permissions | Actual Brigadier set/next commands; level-zero set rejected; day reset and unchanged daylight asserted; transition event observed. |
| Invalid configuration | Invalid types, nulls, negative/huge/non-finite multipliers and fractional lengths; partial defaults; malformed reload retains active policy and preserves user file; live reload changes season length without losing phase. |
| Vanilla crop modifiers and winter | Actual `BlockState.randomTick` samples for wheat, carrots, potatoes, and beetroot; seeded summer/winter comparisons; winter still grows; zero stops actual growth; wheat override permits growth; disabling seasonal growth restores neutral policy. |
| Vanilla light gate | Sealed dark fixture asserts actual light below nine and no wheat growth over 1,000 random ticks. Open sky at night is not treated as a dark fixture. |
| Dimensions | Actual Overworld seasonal policy and Nether/End neutral growth assertions. Unknown dimensions excluded by allowlist; custom dimension travel not exercised. |
| Server/client synchronization | Registry-aware packet codec round-trip preserves dimension, phase, reference time, season length and server visual flags. Real network join/travel remains untested. |
| Regnum World ownership | Loader-presence policy and seeded identical summer/winter results when the regional hook owns ticks. The final combined run status is recorded above. |
| No Agriculture recursion | Counting RNG bounds draws to one crop roll (plus beetroot's independent gate) in standalone tests; no production call back into randomTick; every opportunity advances at most one age. |
| No duplicate registration | Invoke the initializer again on a running test server; no duplicate payload exception and one namespaced command tree. |
| Dedicated-server safety | Both standalone and combined GameTest servers and a normal dedicated server initialize without client class loading failures. |
| Weather | Actual winter snow placement, ownership serialization, player-placement ownership transfer, removal cleanup, seasonal thaw, preservation of untracked snow, and hot-climate temperature checks. |

## Regnum World fixture

Inspected and built unchanged tracked source at `aeaf32470e2b42d1aaba7f8333cb14fbbd4c354b`. Its own `bash gradlew clean build` succeeds under Java 21 with its declared Loom 1.14.10. No tracked Regnum World source was modified.

Compatibility command:

```bash
./gradlew runGameTest -PregnumWorldJar=/tmp/regnum-world-audit/build/libs/regnum-world-1.0.0.jar
```

The path is a local verification artifact, not a dependency or shipped file. In the test run's `build/gametest/config/regnum_world.json`, underground restrictions and altitude debuffs are disabled to allow the flat underground test structures to grow crops. Plains use `defaultMultiplier: 2.0` under `biome_growth_modifiers`, deliberately exercising Regnum World's extra-tick path. Agriculture must leave the resulting seeded growth identical across seasons and crop seasonal overrides. This verifies conservative coexistence; it does not claim combined regional/seasonal balancing.

## Logs and remaining checks

The malformed-configuration warning and non-admin command rejection are intentional test cases. Fabric API emits a duplicate `package-info` remapping warning between its tag modules; this is external metadata, not duplicate Agriculture registration. Loom launch tasks opt out of configuration caching to regenerate argument files after clean builds; run build and launch commands separately. Loom emits Gradle deprecation notices; Gradle 10 is not supported by this pinned build. No critical mod errors occurred in successful final runtime runs. Earlier development runs exposed test-fixture/report-path issues, which were corrected before the final checks.

Client source compilation and packet encoding were exercised. **Rendered seasonal colors, live multiplayer delivery, dimension travel, reconnect behavior, resource-pack appearance, and renderer-mod compatibility were not runtime-tested.** No graphical client validation was executed; a headless virtual-display setup was unavailable. Those checks remain manual release validation. Normal dedicated-server startup and gameplay GameTests were available and executed; they are not substitutes for visual inspection.

Reports produced by Gradle: `build/test-results/test/` and `build/gametest/reports/gametest-results.xml`. A standalone and compatibility report copy is retained under `build/verification/` after final packaging. Build outputs and local test worlds are ignored by Git.
