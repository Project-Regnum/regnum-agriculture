# Agriculture v1.1 verification

Date: 2026-11-17. Base: `1da86a173a2cbf4f7ed7c724c54ce5e2bd5f5717`,
branch `feat/farmers-delight-integration`.

Java 21.0.12, Minecraft 1.21.1, Gradle 9.6.1, Loom 1.16.3,
Fabric Loader 0.20.1, Fabric API 0.116.11+1.21.1.

## Executed checks

- Full `build test`: distributable/source artifacts, access-widener validation,
  two JUnit tests covering stochastic rounding, neutral RNG and invalid rates.
- Dedicated-server GameTests without World: common initialization, registry and
  recipe loading, commands, natural cabbage/onion/tomato growth, and actual growth
  advancement for budding/climbing tomatoes, rice, panicles and both colonies.
- GameTests with unchanged local World JAR: one controller after repeat init,
  seeded `2×` versus two `1×` ticks (state and subsequent RNG) for cabbage, onion,
  tomatoes, climbing tomatoes, budding tomatoes, rice, panicles and both colonies;
  zero multiplier and direct bone meal denial, underground denial, replay cleanup,
  destination colony restrictions and no policy RNG for ineligible soil targets.
  Colony tests now use rich soil: mycelium supports placement but does not enable
  natural colony growth. Climbing fixtures clear the rope left on removal.
- Two synthetic incompatible fixtures (missing registration API and renamed
  internal helper): server starts without an injection crash, emits one clear
  bridge error, and GameTests confirm crop ticks, bone meal and rich soil are
  blocked. These do not exhaust every possible ABI break.
- Parsed all 1,445 resource JSON files; no missing local FD model parent/texture
  references. Ukrainian now covers all 499 English FD translation keys. This is
  structural coverage, not a complete linguistic or visual translation review.
- `git diff --check`.

World checkout: `8e363219f4cf7f03f8916ae5a0cff6532c260d9f`.
World JAR: `regnum-world-1.0.0.jar`, SHA-256
`6cd0f8767c7416e05eb4d5ea51769feea6ee5c107dd7d9e0bbbcffc6a67c724e`.
No updated v2.1 public replay/season contract exists in the inspected checkout.
The working legacy bridge is retained; see [compatibility notes](COMPATIBILITY.md).

## Graphical client

Actually launched `runClientValidation` on Java 21 using a newly created isolated
`build/client-validation` instance, without World. Observed:

- Main menu, Mod Menu entry, new creative world and Regnum Agriculture category.
- Rendered equipment/items in the category and placed cooking-pot model.
- Open cooking-pot GUI with inventory slots, English title and recipe-book panel.
  The recipe panel was empty in this fresh world; recipe contents/cooking were
  not verified interactively. Dedicated-server checks confirm recipes load.
- Restarted the client with the real World JAR after the bridge changes: bridge
  initialization succeeded, atlases loaded and main menu rendered.
- No Agriculture missing-texture/model or client mixin exception was observed.
  Environment errors occurred for missing narrator `flite` and an authentication
  service response, plus vanilla sound/shader warnings. They did not prevent launch.

Local evidence under ignored `build/`: `client-validation-menu.png`,
`client-validation-inventory.png`, `client-validation-pot-gui.png`,
`client-validation-recipe-book.png`, `client-world-menu.png` and launch logs.
Early exploratory UI commands failed; no cutting-board interaction is claimed.

## Manual playtest still required

- Cutting board recipes/drops, stove heating, complete cooking/serving cycle and
  recipe-book results; JEI/REI/EMI are not installed in this client validation.
- Food tooltips, status effects and PvP balance during actual play.
- All crop stages rendered in-world; Ukrainian UI appearance and text quality.
- Two-player concurrent equipment access, reconnects, chunk unload/reload,
  existing-save migration and custom resource/datapack overrides.
- Remapped production-JAR client/server launch outside the Loom dev environment.

No production/player saves were accessed. The newly created creative test save
and automated worlds are confined to `build/`. No deployment or merge performed.
See COMPATIBILITY.md for exact commands, failure policy, tooling limitations and
requirements for replacing the internal replay hook with a real public contract.
