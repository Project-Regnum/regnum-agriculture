# Changelog

## 0.1.0 — 2026-11-12

- Adapt the verified Fabric Seasons 1.21.1 calendar and climate foundation under MPL-2.0.
- Add persistent season anchors, safe manual changes, phase-preserving length reloads, dimension policy, and server transition events.
- Add server-authoritative typed synchronization and separate client visual preferences.
- Add biome grass/foliage tints and gradual transitions without importing upstream assets or mutating biome registry instances.
- Add scoped seasonal snowfall with persistent ownership tracking and thaw during vanilla column ticks.
- Add a single, non-recursive vanilla crop probability hook for wheat, carrots, potatoes, and beetroot, with per-crop overrides.
- Defer crop ticking to Regnum World when installed; document the required combined integration boundary.
- Add permission-checked `/regnumagriculture` commands with English and Ukrainian feedback.
- Add unit and dedicated-server GameTests, technical audits, licensing notices, and development documentation.
- Remove template examples and unrelated upstream modules/dependencies.

### Planned direction for 0.2.0

Agree an explicit growth ownership handshake with Regnum World; verify multiplayer visuals/resource packs and renderer compatibility; consider crop-specific preferences and a modest first crop expansion after the foundation is validated. No future crop, cooking, fertility, irrigation, greenhouse, or economy features are included in this release.
