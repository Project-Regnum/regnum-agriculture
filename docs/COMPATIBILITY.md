# Agriculture v1.1 compatibility and testing

The `farmersdelight` registry namespace, item/block IDs, recipes, save data and
upstream licenses are retained. This is an intentional source fork, not an addon
requiring a second Farmer's Delight installation.

## World growth contract

The available World checkout is `8e363219f4cf7f03f8916ae5a0cff6532c260d9f`.
Its public API provides `registerGrowthController(ExternalGrowthController)`,
`isUndergroundGrowthBlocked(Level, BlockPos)` and
`getFinalCropMultiplier(ServerLevel, BlockPos, BlockState)`. The controller receives
a precomputed regional multiplier and returns PASS/HANDLED/CANCEL. There is no
public original-tick execution scope or authoritative season policy in this
checkout. No new v2.1 contract was available locally; no season calendar is fabricated.

Agriculture therefore retains the tested legacy `GrowthHelper.handleRandomTick`
replay hook. Original FD ticks are replayed with stochastic rounding (maximum 16
attempts), without drawing rounding RNG for integral rates. During replay only
World's helper is suppressed; original crop logic still executes. State is read
afresh for each attempt, replay stops at block-type changes, and the thread-local
scope is removed in `finally`. Vanilla-derived cabbage, onions and rice panicles
enter through World's controller; FD overrides enter through the source bridge.
Bone meal checks hard restrictions, and rich soil applies its own single growth
opportunity. Mushroom conversion checks the destination colony's hard policy too.
All state mutation remains on the logical server and uses normal block updates.
No new persistent state, packets or registry entries were introduced.

The optional pseudo mixin has a local `require=0`; essential FD injections retain
`defaultRequire=1`. A bytecode plugin checks the exact runtime target signature
before applying the replay mixin. Initialization also validates the helper and
public controller signature before registering exactly once. This avoids a
changed optional target crashing mixin injection or activating an unverified
replay bridge. Missing World leaves ordinary FD growth intact. An installed but
incompatible World logs one integration error and blocks Overworld crop ticks,
bone meal and rich-soil boosts, including vanilla CropBlock crops. This deliberate
failure mode preserves restrictions until the administrator installs a compatible
World or removes it; it is not a fallback to unrestricted growth.

The expected future public contract must define, without depending on World's
internal helper:

- An original-tick execution scope that bypasses World re-entry while preserving
  the original crop's RNG, conditions and stage transitions.
- Which layer applies the final multiplier exactly once, dimension behavior,
  hard denials, bounded acceleration, and controller registration ownership.
- Authoritative policy for random ticks, bone meal and rich-soil opportunities,
  including multi-block/stage plants and any real season policy.

These are requirements, not invented method names or signatures. Replacing the
legacy bridge requires inspecting and testing the actual published contract.

## Reproduce

Set `JAVA_HOME` to a Java 21 JDK. Automated worlds live only under `build/gametest`;
the client task uses `build/client-validation`. Never point these at player saves.

```sh
./gradlew build test
./gradlew runGameTest
./gradlew runGameTest -PregnumWorldJar=/absolute/path/to/regnum-world-1.0.0.jar
./gradlew runClientValidation
./gradlew runClientValidation -PregnumWorldJar=/absolute/path/to/regnum-world-1.0.0.jar
python3 scripts/make-incompatible-world-fixture.py /absolute/path/to/regnum-world-1.0.0.jar build/world-incompatible-api.jar
./gradlew runGameTest -PregnumWorldJar="$PWD/build/world-incompatible-api.jar" -PexpectWorldFailure
python3 scripts/make-incompatible-world-fixture.py /absolute/path/to/regnum-world-1.0.0.jar build/world-incompatible-helper.jar --helper
./gradlew runGameTest -PregnumWorldJar="$PWD/build/world-incompatible-helper.jar" -PexpectWorldFailure
```

The synthetic fixtures rename the public registration method or the internal
helper (including World's own call sites) and strip their Loom version metadata to test runtime API rejection; it must never be distributed.
The real World JAR was used unchanged. It advertises Loom 1.19.4, whereas this
project uses 1.16.3; the existing local remap cache allowed real-JAR tests, but a
fresh development cache may reject that tool-version mismatch. This is a build
fixture/tooling limitation, separate from game Loader compatibility.

## Food and multiplayer review

Retained meals provide 6–14 nutrition with ordinary saturation and tier-zero
Nourishment. Sandwiches offer portability; bowl meals add duration and return
containers. Existing cider absorption, salad regeneration and pastry speed remain
upstream effects, without new Strength/Resistance or attribute systems. No food
values or recipes were changed in this hardening pass. Actual PvP balance still
needs playtesting, particularly fast foods and long Nourishment durations.

Cooking/cutting recipes load on the dedicated server. Existing block updates,
menu synchronization and server recipe handling remain in use. This pass does
not prove simultaneous cooking/cutting interactions, reconnects, chunk reloads,
old-world migration, arbitrary resource packs or third-party recipe viewers.
Use disposable multiplayer worlds for those checks before release.
