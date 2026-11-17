# Regnum Agriculture

A source fork of Farmer's Delight Refabricated for Project Regnum, targeting
Minecraft **1.21.1**, Fabric and **Java 21**. Cooking pots, cutting boards,
stoves, knives, crops, wild plants, rich soil, meals, furniture, recipes and loot
are preserved. This is an independent Regnum mod, with upstream attribution.

Install the normal `regnum-agriculture-1.0.0.jar` on server and clients alongside
Fabric API for 1.21.1 and Fabric Loader 0.20.1 or newer (the latter is required by
the tested World build). Do not install the sources JAR or a separate Farmer's
Delight: Agriculture provides `farmersdelight` itself. Existing registry IDs,
network channels and `farmersdelight-common.json` / client configuration names
are retained for compatibility. Back up worlds before changing mod sets.

Regnum World is optional. Without it, ordinary Farmer's Delight growth applies.
With the supported World API, Agriculture uses World's full regional/altitude
multiplier and underground restrictions. Vanilla crop ticks remain World's
responsibility. Imported crops use bounded original gameplay ticks, including
rice's two stages, budding/climbing tomatoes and mushroom colonies. Each tick
receives the World multiplier once. Fractional rates use the server RNG; rates
above one repeat original growth opportunities, capped at 16 per incoming tick.
No parallel or wall-clock RNG is used. Current World rules are queried each time.
Bone meal items retain World's efficiency checks. Direct imported growth and
rich-soil boosts also respect hard restrictions; rich-soil boost opportunities
are reduced in regions below 1×, without multiplying them by boosted tick rates.

**Season support is blocked by the available World API.** The checked World
source and tested JAR expose regional growth rules, not a season/calendar API.
Agriculture does not invent a calendar or advertise fictitious seasonal crops.
A future World API must expose its season information and include seasonal
restrictions/rates in its authoritative growth policy before transition behavior
can be implemented and verified. Economy and professions remain future work.

Player commands (English and Ukrainian):

- `/agriculture` or `/agri`: installed version and bridge status.
- `/agri check`: targeted block's World factor and growth restrictions. The
  displayed factor does not replace light, substrate or maturity requirements.
- `/agri season`: explains that calendar information is unavailable.

Global `/season`, `/seasons`, `/fd`, `/farmersdelight` and `/ra` aliases were
removed to avoid conflicts. Free growth and RNG-consuming simulation commands
were removed. No diagnostic command changes crops or samples the server RNG.
API failures log a meaningful error once and disable the Agriculture bridge;
World may continue applying its own vanilla-plant rules.

Food retains existing Comfort and Nourishment effects. Vegetable soup now offers
10 nutrition, 0.75 saturation modifier and five minutes of Nourishment, compared
with beef stew's 12 nutrition, 0.8 modifier and three minutes. This creates a
longer-lasting vegetable meal for travel/work using the existing effect system,
without additional combat buffs or a new economy/buff framework.

Build and isolated validation, using a Java 21 JDK:

```sh
./gradlew build
./gradlew runGameTest
./gradlew runGameTest -PregnumWorldJar=/absolute/path/to/regnum-world.jar
```

GameTests run only in `build/gametest`; they do not access `run/` or a production
world. Optional client recipe viewers/configuration integrations remain
compile-only integrations with local development runtimes. Upstream publishing
IDs, publishing plugin and remote Maven credentials/destination were removed;
local Maven publication is available under Regnum coordinates.

See [verification](docs/VERIFICATION.md), [LICENSE](LICENSE) and
[third-party notices](THIRD_PARTY_NOTICES.md). Upstream namespaces are retained
for save compatibility; compatibility with all third-party addons is not implied.
