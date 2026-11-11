// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.test;

import com.projectregnum.agriculture.RegnumAgriculture;
import com.projectregnum.agriculture.agriculture.CropGrowthService;
import com.projectregnum.agriculture.climate.*;
import com.projectregnum.agriculture.networking.SeasonPayload;
import com.projectregnum.agriculture.season.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

public class FoundationGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void savedCalendarSurvivesDiskRestart(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var directory = Files.createTempDirectory("regnum-season-persistence").toFile();
        var factory = new SavedData.Factory<>(() -> new SeasonState(24), SeasonState::load, null);
        var first = new DimensionDataStorage(directory, level.getServer().getFixerUpper(), level.registryAccess());
        SeasonState state = first.computeIfAbsent(factory, "seasons");
        state.setSeason(123_456, Season.WINTER);
        first.save();
        var restarted = new DimensionDataStorage(directory, level.getServer().getFixerUpper(), level.registryAccess());
        SeasonState loaded = restarted.computeIfAbsent(factory, "seasons");
        helper.assertTrue(loaded.snapshot(123_456 + 24_000).season() == Season.WINTER, "Manual season must survive disk reload");
        helper.assertTrue(loaded.snapshot(123_456 + 24_000).day() == 2, "Day must advance after restart");
        loaded.resize(123_456 + 24_000, 12);
        helper.assertTrue(loaded.snapshot(123_456 + 24_000).season() == Season.WINTER, "Resize must retain season");
        helper.assertTrue(loaded.snapshot(123_456 + 24_000).ticksWithinSeason() == 12_000, "Resize must retain fractional progress");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void payloadRoundTripsWithDimensionAndPolicy(GameTestHelper helper) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var original = new SeasonPayload("minecraft:overworld", 123_456, 987_654, 24, true, false, true);
            SeasonPayload.CODEC.encode(buffer, original);
            var decoded = SeasonPayload.CODEC.decode(buffer);
            helper.assertTrue(original.equals(decoded), "Packet must preserve calendar, dimension and server flags");
            helper.assertTrue(buffer.readableBytes() == 0, "Decoder must consume complete packet");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void dimensionsAndRegistration(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var manager = SeasonManager.get(server);
        helper.assertTrue(manager.isSeasonal(server.overworld()), "Overworld should be seasonal");
        for (var key : java.util.List.of(Level.NETHER, Level.END)) {
            ServerLevel level = server.getLevel(key);
            if (level != null) {
                helper.assertTrue(!manager.isSeasonal(level), "Nether and End must be neutral");
                helper.assertTrue(CropGrowthService.getCropGrowthMultiplier(level, BlockPos.ZERO,
                        Blocks.WHEAT.defaultBlockState()) == 1, "Inactive dimension growth must be vanilla");
            }
        }
        new RegnumAgriculture().onInitialize();
        var command = server.getCommands().getDispatcher().getRoot().getChild("regnumagriculture");
        helper.assertTrue(command != null && command.getChildren().size() == 2, "One namespaced command tree expected");
        helper.assertTrue(FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER,
                "Tests must load on dedicated server without client classes");
        helper.assertTrue(CropGrowthService.ownsCropTicks() == !FabricLoader.getInstance().isModLoaded("regnum-world"),
                "Exactly one mod owns crop ticks");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "growth", skyAccess = true, timeoutTicks = 200)
    public void actualCropGrowthAndManualCommands(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = SeasonManager.get(level.getServer());
        var before = manager.getCurrentSeason(level);
        long beforeTime = level.getDayTime();
        var transitions = new AtomicInteger();
        // Listener is gated to this level and stops counting after the synchronous check below.
        var observing = new java.util.concurrent.atomic.AtomicBoolean(true);
        SeasonEvents.TRANSITION.register((world, previous, current) -> {
            if (observing.get() && world == level) transitions.incrementAndGet();
        });
        manager.tick();
        var commands = level.getServer().getCommands();
        commands.performPrefixedCommand(level.getServer().createCommandSourceStack().withLevel(level).withPermission(4),
                "regnumagriculture season set winter");
        helper.assertTrue(manager.getCurrentSeason(level) == Season.WINTER, "Command must set winter");
        helper.assertTrue(manager.getCurrentDay(level) == 1, "Manual set must reset to day one");
        helper.assertTrue(level.getDayTime() == beforeTime, "Season commands must not change daylight");
        commands.performPrefixedCommand(level.getServer().createCommandSourceStack().withLevel(level).withPermission(0),
                "regnumagriculture season set summer");
        helper.assertTrue(manager.getCurrentSeason(level) == Season.WINTER, "Non-admin must not change seasons");
        commands.performPrefixedCommand(level.getServer().createCommandSourceStack().withLevel(level).withPermission(4),
                "regnumagriculture season next");
        helper.assertTrue(manager.getCurrentSeason(level) == Season.SPRING, "Next after winter must wrap to spring");
        observing.set(false);
        helper.assertTrue(transitions.get() >= 1, "Manual changes must emit transition events");
        level.setDayTime(6000);
        BlockPos cropPos = helper.absolutePos(new BlockPos(3, 2, 3));
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            level.setBlockAndUpdate(cropPos.offset(x, -1, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        helper.runAfterDelay(5, () -> {
            try {
                for (Block block : new Block[]{Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS}) {
                    CropBlock crop = (CropBlock) block;
                    manager.setSeason(Season.SUMMER);
                    int summer = sampleGrowth(level, cropPos, crop, 4096);
                    manager.setSeason(Season.WINTER);
                    int winter = sampleGrowth(level, cropPos, crop, 4096);
                    if (CropGrowthService.ownsCropTicks()) {
                        helper.assertTrue(summer > winter * 2, "Summer must outgrow winter for " + block + ": " + summer + "/" + winter);
                        helper.assertTrue(winter > 0, "Winter must still allow slow growth for " + block);
                    } else {
                        helper.assertTrue(summer == winter, "Agriculture must leave Regnum World rolls identical across seasons: " + block);
                    }
                }
            } finally {
                level.setDayTime(beforeTime);
                manager.setSeason(before);
            }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "light", skyAccess = true)
    public void darkCropsKeepVanillaLightGate(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = SeasonManager.get(level.getServer());
        var original = manager.getCurrentSeason(level);
        manager.setSeason(Season.SUMMER);
        var pos = helper.absolutePos(new BlockPos(3, 3, 3));
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++)
            level.setBlockAndUpdate(pos.offset(x, y, z), Blocks.STONE.defaultBlockState());
        level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
        level.setBlock(pos, Blocks.WHEAT.defaultBlockState(), 2);
        helper.runAfterDelay(5, () -> {
            try {
                helper.assertTrue(level.getRawBrightness(pos, 0) < 9, "Darkness fixture must block skylight and blocklight");
                helper.assertTrue(level.getBlockState(pos).is(Blocks.WHEAT), "Fixture must retain the crop");
                RandomSource random = RandomSource.create(9876);
                for (int i = 0; i < 1000; i++) level.getBlockState(pos).randomTick(level, pos, random);
                helper.assertTrue(((CropBlock) Blocks.WHEAT).getAge(level.getBlockState(pos)) == 0,
                        "Season multiplier must preserve vanilla light requirement");
            } finally { manager.setSeason(original); }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "configuration", skyAccess = true)
    public void liveReloadAndZeroGrowth(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var manager = SeasonManager.get(level.getServer());
        var configFile = FabricLoader.getInstance().getConfigDir().resolve("regnum_agriculture.json");
        String originalConfig = Files.readString(configFile);
        Season originalSeason = manager.getCurrentSeason(level);
        long originalTime = level.getDayTime();
        var commands = level.getServer().getCommands();
        try {
            manager.setSeason(Season.SPRING);
            long seasonTicks = SeasonCalendar.seasonTicks(manager.getSeasonLengthDays());
            level.setDayTime(level.getDayTime() + seasonTicks + 24_000);
            manager.tick();
            helper.assertTrue(manager.getCurrentSeason(level) == Season.SUMMER && manager.getCurrentDay(level) == 2,
                    "World time progression must advance season and day");
            float previousProgress = manager.getSeasonProgress(level);
            Files.writeString(configFile, """
                    {"seasonLengthDays":3,"cropGrowthMultipliers":{"winter":0},
                     "cropOverrides":{"minecraft:wheat":{"winter":0.75}}}
                    """);
            commands.performPrefixedCommand(level.getServer().createCommandSourceStack().withLevel(level).withPermission(4),
                    "regnumagriculture reload");
            helper.assertTrue(manager.getSeasonLengthDays() == 3, "Admin reload must apply length");
            helper.assertTrue(manager.getCurrentSeason(level) == Season.SUMMER
                    && Math.abs(manager.getSeasonProgress(level) - previousProgress) < 0.0001,
                    "Length reload must preserve season and fractional progress");
            Files.writeString(configFile, "{malformed");
            helper.assertTrue(!manager.reload() && manager.getSeasonLengthDays() == 3,
                    "Malformed reload must retain active policy");
            manager.setSeason(Season.WINTER);
            BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
            helper.assertTrue(CropGrowthService.getCropGrowthMultiplier(level, pos, Blocks.CARROTS.defaultBlockState()) == 0,
                    "Winter zero modifier must be accepted");
            helper.assertTrue(CropGrowthService.getCropGrowthMultiplier(level, pos, Blocks.WHEAT.defaultBlockState()) == 0.75f,
                    "Crop override must replace the global winter factor");
            level.setDayTime(6000);
            // Reanchor after changing daylight for the growth experiment.
            manager.setSeason(Season.WINTER);
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                level.setBlockAndUpdate(pos.offset(x, -1, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
            helper.runAfterDelay(5, () -> {
                try {
                    int carrots = sampleGrowth(level, pos, (CropBlock) Blocks.CARROTS, 1024);
                    int wheat = sampleGrowth(level, pos, (CropBlock) Blocks.WHEAT, 1024);
                    if (CropGrowthService.ownsCropTicks()) {
                        helper.assertTrue(carrots == 0, "Zero multiplier must stop actual random-tick growth");
                        helper.assertTrue(wheat > 0, "Crop override must allow actual wheat growth");
                    } else helper.assertTrue(carrots == wheat, "World ownership must bypass both seasonal crop overrides");
                    Files.writeString(configFile, "{\"enableSeasonalCropGrowth\":false}");
                    helper.assertTrue(manager.reload(), "Valid disabled policy must reload");
                    helper.assertTrue(CropGrowthService.getCropGrowthMultiplier(level, pos, Blocks.CARROTS.defaultBlockState()) == 1,
                            "Disabled seasonal growth must be neutral");
                    helper.assertTrue(sampleGrowth(level, pos, (CropBlock) Blocks.CARROTS, 1024) > 0
                            || !CropGrowthService.ownsCropTicks(), "Vanilla growth must resume when disabled");
                } catch (java.io.IOException e) { throw new RuntimeException(e); }
                finally {
                    try { Files.writeString(configFile, originalConfig); } catch (java.io.IOException e) { throw new RuntimeException(e); }
                    manager.reload();
                    level.setDayTime(originalTime);
                    manager.setSeason(originalSeason);
                }
                helper.succeed();
            });
        } catch (Exception e) {
            Files.writeString(configFile, originalConfig);
            manager.reload();
            level.setDayTime(originalTime);
            manager.setSeason(originalSeason);
            throw e;
        }
    }

    private static int sampleGrowth(ServerLevel level, BlockPos pos, CropBlock crop, int samples) {
        CountingRandom random = new CountingRandom(12345);
        int grown = 0;
        for (int i = 0; i < samples; i++) {
            var state = crop.getStateForAge(0);
            level.setBlockAndUpdate(pos, state);
            random.rolls = 0;
            state.randomTick(level, pos, random);
            if (CropGrowthService.ownsCropTicks() && random.rolls > (crop == Blocks.BEETROOTS ? 2 : 1))
                throw new AssertionError("Unexpected extra or recursive crop random rolls: " + random.rolls);
            int age = crop.getAge(level.getBlockState(pos));
            if (age > 0) grown++;
            if (age > 1) throw new AssertionError("Recursive growth detected");
        }
        return grown;
    }

    private static final class CountingRandom extends net.minecraft.world.level.levelgen.LegacyRandomSource {
        private int rolls;
        private CountingRandom(long seed) { super(seed); }
        @Override public int nextInt(int bound) { rolls++; return super.nextInt(bound); }
        @Override public double nextDouble() { rolls++; return super.nextDouble(); }
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "weather", skyAccess = true)
    public void winterSnowAndTrackedThaw(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = SeasonManager.get(level.getServer());
        var original = manager.getCurrentSeason(level);
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var biome = level.getBiome(pos).value();
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        try {
            manager.setSeason(Season.WINTER);
            helper.assertTrue(ClimateService.temperature(0.7f, Season.WINTER) < 0.15f, "Temperate winter must support snow");
            helper.assertTrue(ClimateService.temperature(2f, Season.WINTER) > 0.15f, "Hot climates must stay warm");
            helper.assertTrue(biome.hasPrecipitation() && biome.getBaseTemperature() >= 0.15f
                    && (biome.getBaseTemperature() <= 0.7f || ClimateService.forceWinterSnow(level.getBiome(pos))),
                    "Snow test requires a temperate biome; must not silently skip coverage");
            {
                helper.assertTrue(biome.shouldSnow(level, pos), "Scoped seasonal biome hook must allow winter snow");
                level.setWeatherParameters(0, 100, true, false);
                level.setRainLevel(1);
                level.tickPrecipitation(pos);
                helper.assertTrue(level.getBlockState(pos).is(Blocks.SNOW), "Precipitation must place snow");
                helper.assertTrue(SeasonalSnowState.get(level).contains(pos), "Seasonal snow must be tracked");
                var savedSnow = SeasonalSnowState.get(level).save(new net.minecraft.nbt.CompoundTag(), level.registryAccess());
                helper.assertTrue(SeasonalSnowState.load(savedSnow, level.registryAccess()).contains(pos),
                        "Seasonal snow ownership must survive serialization");
                Blocks.SNOW.setPlacedBy(level, pos, level.getBlockState(pos), null,
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SNOW));
                helper.assertTrue(!SeasonalSnowState.get(level).contains(pos), "Player stacking must relinquish seasonal ownership");
                manager.setSeason(Season.SUMMER);
                level.tickPrecipitation(pos);
                helper.assertTrue(level.getBlockState(pos).is(Blocks.SNOW), "Player-owned snow must not seasonally thaw");
                level.removeBlock(pos, false);
                manager.setSeason(Season.WINTER);
                level.tickPrecipitation(pos);
                helper.assertTrue(SeasonalSnowState.get(level).contains(pos), "Seasonal snow must be tracked again after removal");
                level.removeBlock(pos, false);
                helper.assertTrue(!SeasonalSnowState.get(level).contains(pos), "Block removal must clear tracking");
                level.tickPrecipitation(pos);
                manager.setSeason(Season.SUMMER);
                level.tickPrecipitation(pos);
                helper.assertTrue(!level.getBlockState(pos).is(Blocks.SNOW), "Tracked snow must thaw");
                level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
                level.tickPrecipitation(pos);
                helper.assertTrue(level.getBlockState(pos).is(Blocks.SNOW), "Untracked player/worldgen snow must remain");
            }
        } finally {
            level.setWeatherParameters(6000, 0, false, false);
            level.setRainLevel(0);
            manager.setSeason(original);
        }
        helper.succeed();
    }
}
