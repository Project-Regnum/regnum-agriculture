# Agriculture overhaul v1 verification

Date: 2026-11-17. Base: `663e0e8`, `feat/farmers-delight-integration`.
No production/developer world was launched, no deployment, push or merge performed.

Environment: Minecraft 1.21.1, Java 21.0.12, Gradle 9.6.1, Loom 1.16.3,
Fabric Loader 0.20.1, Fabric API 0.116.11+1.21.1.

Executed checks:

- Complete `build`: Java compilation, access-widener validation, two JUnit
  growth rounding/bounds/transition tests, remapped distributable and sources JARs.
- Isolated dedicated-server GameTest without World: initialization, retained
  cooking-pot and food registration, loaded cooking and cutting recipes,
  command roots, absence of free-growth and global season aliases, imported
  crop growth and absence of World linkage failures.
- Same isolated GameTest with World: one active Agriculture controller, repeated
  initialization is harmless; normal imported crop growth; underground blocking;
  zero-rate natural/direct-bonemeal blocking. Seeded checks over 32 seeds each for
  cabbage, onion, tomato, budding tomato, rice, rice panicles and mushroom colony:
  `2×` equals two `1×` original crop ticks, including subsequent RNG state.
  Test configuration is temporarily changed in memory and restored in `finally`.
- Parsed 1,445 resource JSON files; no malformed JSON or missing Farmer's Delight
  model-parent/texture references. Dedicated-server resource loading also passed.
- Checked packaged mod metadata, preserved `farmersdelight` alias/registries,
  and inclusion of original MIT notice, LGPL text and third-party notices in
  distributable/source artifacts.
- `git diff --check`.

World test artifact: `regnum-world-1.0.0.jar`, SHA-256
`6cd0f8767c7416e05eb4d5ea51769feea6ee5c107dd7d9e0bbbcffc6a67c724e`.
Inspected World source HEAD: `8e363219f4cf7f03f8916ae5a0cff6532c260d9f`.
These describe the local artifact and inspected checkout separately; no claim is
made that another World release exposes the same API.

Commands (set JAVA_HOME to a Java 21 JDK):

```sh
./gradlew build
./gradlew runGameTest
./gradlew runGameTest -PregnumWorldJar=/absolute/path/regnum-world-1.0.0.jar
```

Reports: `build/test-results/test/` and
`build/gametest/reports/gametest-results.xml`. Each GameTest run replaces its
previous report. Tests use only `build/gametest`.

Remaining limits:

- **World seasons are unavailable in the supplied API.** No seasonal growth,
  transitions, season UI, or seasonal crop preferences can be verified. Current
  integration implements World's regional/altitude/underground policy, without
  an independent seasonal simulation. World must expose its calendar and fold
  seasonal policy into its growth API (including bone meal) to unblock this.
- No graphical client, manual cooking interaction, resource-pack appearance,
  live multiplayer session, third-party addon matrix, or existing-save migration
  was exercised. Server startup confirms common code has no hard client linkage;
  this is not a substitute for a multiplayer client playtest.
- Three upstream deprecation warnings remain in hanging-tomato/JEI code. Fabric
  emits its development warning for untranslated item tags. Without World,
  Mixin reports the missing optional pseudo target; it does not fail startup.
- Imported licensing was checked against the retained repository-wide LICENSE
  and upstream MIT notice; individual asset authorship history was not forensically
  reconstructed. No new external game art was copied during this overhaul.
