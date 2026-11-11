# Regnum World compatibility

## Inspected implementation

Repository: https://github.com/Project-Regnum/regnum-world

Revision: `aeaf32470e2b42d1aaba7f8333cb14fbbd4c354b` (`main`, audited 2026-11-12).
The actual Fabric mod ID is **`regnum-world`**, including the hyphen.

Relevant files:

- `src/main/java/zooller/regnumworld/mixin/CropBlockMixin.java`
- `src/main/java/zooller/regnumworld/growth/GrowthHelper.java`
- `src/main/java/zooller/regnumworld/config/RegnumConfig.java`
- `src/main/java/zooller/regnumworld/mixin/BoneMealItemMixin.java`

`CropBlockMixin` injects at the head of vanilla `CropBlock.randomTick`. `GrowthHelper` checks underground/sky restrictions, looks up biome/block multipliers, applies an altitude multiplier, then either cancels some ticks or calls `state.randomTick` for extra ticks. Its `ThreadLocal<Boolean>` prevents its own reentry. It does not prevent another mod from applying a second modifier to those calls. It also uses the incoming block state for extra calls. Two independent tick multipliers are not an exact combined growth probability.

## v0.1.0 boundary

When Loader reports `regnum-world` present, **Agriculture bypasses its crop probability modification completely**. Regnum World retains control of crop ticks and bonemeal. Agriculture still provides the calendar, commands, colors, snowfall, and queryable seasonal crop multipliers. There is no mandatory dependency, reflection into Regnum World, or mixin priority trick to claim ownership.

This conservative mode prevents seasonal and regional tick modifiers from stacking. It does **not** implement combined regional and seasonal crop growth. It does not repair Regnum World's own extra-tick behavior. Presence detection is conservative even if that mod's growth options are disabled. A startup message explains the selected behavior.

Standalone, Agriculture changes the single vanilla `RandomSource.nextInt(bound) == 0` growth roll to a Bernoulli trial with `p = min(1, seasonalMultiplier / bound)`. Light, soil, age, and beetroot's independent gate remain vanilla. At multiplier 1 the exact vanilla RNG call is retained. At zero growth is disabled; large rates saturate at one age per opportunity. No recursive ticks, extra ticks, age jumps, or bonemeal interception are introduced.

## Required future change in Regnum World

A combined implementation needs an explicit ownership handshake before either crop hook runs:

1. Extract a side-effect-free regional calculation from `GrowthHelper` that returns the biome/altitude factor and regional eligibility (underground and sky checks included).
2. When an Agriculture integration is active, Regnum World's `CropBlockMixin` must skip its cancel/extra-tick path for the supported crops. Preserve unrelated crops and bonemeal policies separately.
3. Register a single regional provider with Agriculture on the server. Agriculture's one vanilla probability interception applies `min(1, seasonalFactor * regionalFactor / vanillaBound)` after eligibility. Neither mod invokes another random tick.
4. Agree a versioned handshake and test both provider availability and actual ownership. Only then may Agriculture enable its hook while Regnum World is present.

No unused provider framework is shipped in v0.1.0. `CropGrowthService.getCropGrowthMultiplier`, position-aware arguments, per-crop config overrides, and `GrowthProbability` keep the future change small. Rye, rice, cabbage, grapes, fertility, irrigation, greenhouses, fertilizer, and rotation are future consumers; none are implemented.

## Verification scope

Unit tests verify the owner-selection policy. Dedicated GameTests verify the standalone probability hook and inspect Loader-selected ownership. A combined runtime can be exercised by supplying a built Regnum World JAR using the development-only `-PregnumWorldJar=/absolute/path/mod.jar` option. This is not an end-user dependency. Actual execution status is recorded in [VERIFICATION.md](VERIFICATION.md).

The Regnum World repository was inspected in a temporary read-only checkout. Its tracked source was not modified.
