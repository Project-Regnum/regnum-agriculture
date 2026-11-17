package com.projectregnum.agriculture.test;

import com.projectregnum.agriculture.sync.RegnumCropSync;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import vectorwing.farmersdelight.common.registry.ModBlocks;
import vectorwing.farmersdelight.common.registry.ModItems;

public class AgricultureGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE, skyAccess = true)
    public void registrationsCommandsAndGrowth(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("farmersdelight:cooking_pot")), "Cooking pot item retained");
        helper.assertTrue(ModItems.VEGETABLE_SOUP.get().getDefaultInstance().get(net.minecraft.core.component.DataComponents.FOOD) != null, "Meal food registered");
        helper.assertTrue(!level.getRecipeManager().getAllRecipesFor(vectorwing.farmersdelight.common.registry.ModRecipeTypes.COOKING.get()).isEmpty(), "Cooking recipes loaded");
        helper.assertTrue(!level.getRecipeManager().getAllRecipesFor(vectorwing.farmersdelight.common.registry.ModRecipeTypes.CUTTING.get()).isEmpty(), "Cutting recipes loaded");
        var commands = level.getServer().getCommands().getDispatcher().getRoot();
        helper.assertTrue(commands.getChild("agriculture") != null && commands.getChild("agri") != null, "Public commands registered");
        helper.assertTrue(commands.getChild("agriculture").getChild("grow") == null, "No free growth command");
        helper.assertTrue(commands.getChild("season") == null, "No conflicting season alias");
        boolean world = FabricLoader.getInstance().isModLoaded("regnum-world");
        helper.assertTrue(RegnumCropSync.isRegnumWorldLoaded() == world, "Optional World bridge must initialize");
        Object config = null;
        java.lang.reflect.Field underground = null, mountain = null;
        boolean oldUnderground = false, oldMountain = false;
        if (world) {
            Class<?> type = Class.forName("zooller.regnumworld.config.RegnumConfig");
            config = type.getMethod("getInstance").invoke(null);
            underground = type.getField("blockUndergroundCropGrowth");
            mountain = type.getField("mountainAltitudeDebuffEnabled");
            oldUnderground = underground.getBoolean(config);
            oldMountain = mountain.getBoolean(config);
            underground.setBoolean(config, false);
            mountain.setBoolean(config, false);
        }
        try {
            BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
            level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
            for (var block : new net.minecraft.world.level.block.Block[]{ModBlocks.CABBAGE_CROP.get(), ModBlocks.ONION_CROP.get(), ModBlocks.TOMATO_CROP.get()}) {
                var crop = (CropBlock) block;
                level.setBlockAndUpdate(pos, crop.defaultBlockState());
                var rng = RandomSource.create(57);
                for (int i = 0; i < 2000 && crop.getAge(level.getBlockState(pos)) == 0; i++)
                    level.getBlockState(pos).randomTick(level, pos, rng);
                helper.assertTrue(crop.getAge(level.getBlockState(pos)) > 0, "Imported crop must grow: " + BuiltInRegistries.BLOCK.getKey(block));
                helper.assertTrue(!RegnumCropSync.isReplaying(), "Replay scope must close");
            }
            if (world) {
                RegnumCropSync.init();
                var api = Class.forName("zooller.regnumworld.api.RegnumWorldApi");
                var controllers = (java.util.List<?>) api.getMethod("getControllers").invoke(null);
                helper.assertTrue(controllers.stream().filter(c -> c.toString().equals("RegnumAgricultureGrowthController")).count() == 1,
                        "Exactly one Agriculture controller");
                @SuppressWarnings("unchecked")
                var biomes = (java.util.Map<String, Object>) config.getClass().getField("biomeGrowth").get(config);
                String biomeId = level.getBiome(pos).unwrapKey().orElseThrow().location().toString();
                Object previous = biomes.get(biomeId);
                var growthType = Class.forName("zooller.regnumworld.config.BiomeGrowthConfig");
                Object policy = growthType.getConstructor(double.class).newInstance(1.0);
                var rateField = growthType.getField("defaultMultiplier");
                biomes.put(biomeId, policy);
                try {
                    for (var crop : new net.minecraft.world.level.block.Block[]{ModBlocks.CABBAGE_CROP.get(), ModBlocks.ONION_CROP.get(),
                            ModBlocks.TOMATO_CROP.get(), ModBlocks.BUDDING_TOMATO_CROP.get(), ModBlocks.RICE_CROP.get(),
                            ModBlocks.RICE_CROP_PANICLES.get(), ModBlocks.BROWN_MUSHROOM_COLONY.get()}) {
                        for (int seed = 0; seed < 32; seed++) {
                            var initial = crop.defaultBlockState();
                            var soil = crop == ModBlocks.RICE_CROP_PANICLES.get() ? ModBlocks.RICE_CROP.get().defaultBlockState()
                                    : crop == ModBlocks.BROWN_MUSHROOM_COLONY.get() ? Blocks.MYCELIUM.defaultBlockState()
                                    : Blocks.FARMLAND.defaultBlockState();
                            level.setBlockAndUpdate(pos.below(2), Blocks.DIRT.defaultBlockState());
                            level.setBlockAndUpdate(pos.below(), soil);
                            level.setBlockAndUpdate(pos, initial);
                            var firstRandom = RandomSource.create(seed);
                            rateField.setDouble(policy, 2);
                            initial.randomTick(level, pos, firstRandom);
                            var doubled = level.getBlockState(pos);
                            long afterDouble = firstRandom.nextLong();
                            rateField.setDouble(policy, 1);
                            level.setBlockAndUpdate(pos, initial);
                            var secondRandom = RandomSource.create(seed);
                            initial.randomTick(level, pos, secondRandom);
                            level.getBlockState(pos).randomTick(level, pos, secondRandom);
                            helper.assertTrue(doubled.equals(level.getBlockState(pos)) && afterDouble == secondRandom.nextLong(),
                                    "2x must equal two original ticks without stacking: " + BuiltInRegistries.BLOCK.getKey(crop) + " seed=" + seed + " doubled=" + doubled + " single=" + level.getBlockState(pos) + " rate=" + RegnumCropSync.getMultiplier(level, pos, initial));
                            rateField.setDouble(policy, 0);
                            level.setBlockAndUpdate(pos, initial);
                            initial.randomTick(level, pos, secondRandom);
                            helper.assertTrue(initial.equals(level.getBlockState(pos)), "Zero factor must stop every imported crop");
                            if (crop instanceof net.minecraft.world.level.block.BonemealableBlock meal)
                                meal.performBonemeal(level, secondRandom, pos, initial);
                            helper.assertTrue(initial.equals(level.getBlockState(pos)), "Zero factor must prevent direct imported bone meal");
                        }
                    }
                } finally {
                    if (previous == null) biomes.remove(biomeId); else biomes.put(biomeId, previous);
                }
            }
            if (world) {
                underground.setBoolean(config, true);
                var state = ModBlocks.TOMATO_CROP.get().defaultBlockState();
                level.setBlockAndUpdate(pos, state);
                level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
                for (int i = 0; i < 100; i++) state.randomTick(level, pos, RandomSource.create(i));
                helper.assertTrue(level.getBlockState(pos).equals(state), "Blocked tomato must not grow");
                helper.assertTrue(RegnumCropSync.shouldCancelGrowth(level, pos, state, level.random), "Rich soil must respect restrictions");
            }
        } finally {
            if (world) {
                underground.setBoolean(config, oldUnderground);
                mountain.setBoolean(config, oldMountain);
            }
        }
        helper.succeed();
    }
}
