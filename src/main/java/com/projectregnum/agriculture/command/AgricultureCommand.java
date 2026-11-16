package com.projectregnum.agriculture.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.projectregnum.agriculture.sync.RegnumCropSync;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.lang.reflect.Method;

public class AgricultureCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerLiteral(dispatcher, "agriculture");
        registerLiteral(dispatcher, "agri");
        registerLiteral(dispatcher, "ra");
        registerLiteral(dispatcher, "farmersdelight");
        registerLiteral(dispatcher, "fd");
        registerLiteral(dispatcher, "season");
        registerLiteral(dispatcher, "seasons");
    }

    private static void registerLiteral(CommandDispatcher<CommandSourceStack> dispatcher, String literal) {
        dispatcher.register(
            Commands.literal(literal)
                .executes(AgricultureCommand::executeStatus)
                .then(Commands.literal("status")
                    .executes(AgricultureCommand::executeStatus))
                .then(Commands.literal("season")
                    .executes(AgricultureCommand::executeSeason))
                .then(Commands.literal("check")
                    .executes(AgricultureCommand::executeCheck))
                .then(Commands.literal("info")
                    .executes(AgricultureCommand::executeCheck))
                .then(Commands.literal("simtick")
                    .executes(AgricultureCommand::executeSimTick))
                .then(Commands.literal("grow")
                    .executes(ctx -> executeGrow(ctx, 1))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1, 16))
                        .executes(ctx -> executeGrow(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))
        );
    }

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        boolean isRegnumLoaded = RegnumCropSync.isRegnumWorldLoaded();

        source.sendSuccess(() -> Component.literal("=== [Regnum Agriculture Status] ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("Mod Version: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal("1.0.0+refabricated").withStyle(ChatFormatting.GREEN)), false);

        source.sendSuccess(() -> Component.literal("Regnum World Connection: ").withStyle(ChatFormatting.GRAY)
            .append(isRegnumLoaded
                ? Component.literal("CONNECTED (Active)").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                : Component.literal("STANDALONE (Regnum World not detected)").withStyle(ChatFormatting.YELLOW)), false);

        source.sendSuccess(() -> Component.literal("Current Season: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(getSeasonInfo(source.getLevel())).withStyle(ChatFormatting.AQUA)), false);

        source.sendSuccess(() -> Component.literal("Underground Growth Restrictions: ").withStyle(ChatFormatting.GRAY)
            .append(isRegnumLoaded
                ? Component.literal("ACTIVE (Enforced)").withStyle(ChatFormatting.AQUA)
                : Component.literal("INACTIVE (Standard)").withStyle(ChatFormatting.DARK_GRAY)), false);

        source.sendSuccess(() -> Component.literal("Regional Biome Multipliers: ").withStyle(ChatFormatting.GRAY)
            .append(isRegnumLoaded
                ? Component.literal("ACTIVE (Scaled)").withStyle(ChatFormatting.AQUA)
                : Component.literal("INACTIVE (Standard 1.0x)").withStyle(ChatFormatting.DARK_GRAY)), false);

        source.sendSuccess(() -> Component.literal("Commands: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal("/agri season, /agri check, /agri simtick, /agri grow [radius]").withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int executeSeason(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        boolean hasSeasonMod = FabricLoader.getInstance().isModLoaded("seasons") || FabricLoader.getInstance().isModLoaded("sereneseasons");

        source.sendSuccess(() -> Component.literal("=== [Season & Climate Check] ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("Current Season: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(getSeasonInfo(level)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)), false);

        source.sendSuccess(() -> Component.literal("Season Mod Status: ").withStyle(ChatFormatting.GRAY)
            .append(hasSeasonMod
                ? Component.literal("INSTALLED (Using mod data)").withStyle(ChatFormatting.GREEN)
                : Component.literal("NOT INSTALLED (Calculated by world day. Calendar item requires Fabric Seasons or Serene Seasons mod)").withStyle(ChatFormatting.YELLOW)), false);

        long totalDays = level.getDayTime() / 24000L;
        long seasonIndex = (totalDays / 8L) % 4L;
        if (seasonIndex == 0) {
            source.sendSuccess(() -> Component.literal("Fertile Crops in Season: Cabbage, Onions, Rice, Carrots, Potatoes").withStyle(ChatFormatting.AQUA), false);
        } else if (seasonIndex == 1) {
            source.sendSuccess(() -> Component.literal("Fertile Crops in Season: Tomatoes, Melons, Wheat, Sugar Cane").withStyle(ChatFormatting.YELLOW), false);
        } else if (seasonIndex == 2) {
            source.sendSuccess(() -> Component.literal("Fertile Crops in Season: Pumpkins, Beetroot, Apples, Onions").withStyle(ChatFormatting.GOLD), false);
        } else {
            source.sendSuccess(() -> Component.literal("Fertile Crops in Season: Winter Rye, Sweet Berries, Mushrooms").withStyle(ChatFormatting.BLUE), false);
        }

        return 1;
    }

    public static String getSeasonInfo(ServerLevel level) {
        if (FabricLoader.getInstance().isModLoaded("sereneseasons")) {
            try {
                Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
                Method getSeasonState = helper.getMethod("getSeasonState", Level.class);
                Object state = getSeasonState.invoke(null, level);
                Method getSubSeason = state.getClass().getMethod("getSubSeason");
                Object subSeason = getSubSeason.invoke(state);
                return String.valueOf(subSeason) + " (Serene Seasons)";
            } catch (Throwable ignored) {}
        }

        long totalDays = level.getDayTime() / 24000L;
        long seasonIndex = (totalDays / 8L) % 4L;
        long dayOfSeason = (totalDays % 8L) + 1L;

        String seasonName;
        if (seasonIndex == 0) {
            seasonName = "Spring (Весна)";
        } else if (seasonIndex == 1) {
            seasonName = "Summer (Лето)";
        } else if (seasonIndex == 2) {
            seasonName = "Autumn (Осень)";
        } else {
            seasonName = "Winter (Зима)";
        }

        return seasonName + " [Day " + dayOfSeason + "/8, World Day " + totalDays + "]";
    }

    private static BlockPos getTargetPos(ServerPlayer player) {
        HitResult hit = player.pick(5.0D, 0.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hit).getBlockPos();
        }
        BlockPos playerPos = player.blockPosition();
        if (!player.level().isEmptyBlock(playerPos)) {
            return playerPos;
        }
        return playerPos.below();
    }

    private static int executeCheck(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Entity entity = source.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be executed by a player."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos targetPos = getTargetPos(player);
        BlockState state = level.getBlockState(targetPos);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        Holder<Biome> biomeHolder = level.getBiome(targetPos);
        ResourceLocation biomeId = biomeHolder.unwrapKey().map(k -> k.location()).orElse(null);

        int skyLight = level.getBrightness(LightLayer.SKY, targetPos);
        int blockLight = level.getBrightness(LightLayer.BLOCK, targetPos);
        boolean canSeeSky = level.canSeeSky(targetPos);

        boolean undergroundBlocked = RegnumCropSync.isUndergroundBlocked(level, targetPos);
        double multiplier = RegnumCropSync.getMultiplier(level, targetPos, state);

        source.sendSuccess(() -> Component.literal("=== [Crop & Environment Check] ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("Target: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(blockId.toString()).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" at [" + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ() + "]").withStyle(ChatFormatting.DARK_AQUA)), false);

        source.sendSuccess(() -> Component.literal("Season: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(getSeasonInfo(level)).withStyle(ChatFormatting.AQUA)), false);

        source.sendSuccess(() -> Component.literal("Biome: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(biomeId)).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" | Dimension: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(level.dimension().location().toString()).withStyle(ChatFormatting.WHITE)), false);

        source.sendSuccess(() -> Component.literal("Lighting: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal("Sky=" + skyLight + ", Block=" + blockLight + ", CanSeeSky=" + canSeeSky).withStyle(ChatFormatting.WHITE)), false);

        MutableComponent undergroundComp = undergroundBlocked
            ? Component.literal("BLOCKED (No growth underground)").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            : Component.literal("ALLOWED").withStyle(ChatFormatting.GREEN);

        source.sendSuccess(() -> Component.literal("Underground Status: ").withStyle(ChatFormatting.GRAY).append(undergroundComp), false);

        ChatFormatting multColor = multiplier > 1.0 ? ChatFormatting.GREEN : (multiplier < 1.0 ? ChatFormatting.RED : ChatFormatting.WHITE);
        source.sendSuccess(() -> Component.literal("Growth Multiplier: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.2fx", multiplier)).withStyle(multColor))
            .append(Component.literal(RegnumCropSync.isRegnumWorldLoaded() ? " (from Regnum World)" : " (Standalone)").withStyle(ChatFormatting.DARK_GRAY)), false);

        IntegerProperty ageProp = findAgeProperty(state);
        if (ageProp != null) {
            int currentAge = state.getValue(ageProp);
            int maxAge = ageProp.getPossibleValues().stream().max(Integer::compareTo).orElse(currentAge);
            source.sendSuccess(() -> Component.literal("Growth Stage: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(currentAge + " / " + maxAge).withStyle(ChatFormatting.LIGHT_PURPLE)), false);
        }

        BlockPos soilPos = targetPos.below();
        BlockState soilState = level.getBlockState(soilPos);
        ResourceLocation soilId = BuiltInRegistries.BLOCK.getKey(soilState.getBlock());
        source.sendSuccess(() -> Component.literal("Soil Below: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(soilId.toString()).withStyle(ChatFormatting.GOLD)), false);

        return 1;
    }

    private static int executeSimTick(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Entity entity = source.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be executed by a player."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos targetPos = getTargetPos(player);
        BlockState state = level.getBlockState(targetPos);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        source.sendSuccess(() -> Component.literal("=== [Simulate Growth Tick] ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("Target: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(blockId.toString()).withStyle(ChatFormatting.AQUA)), false);

        if (level.dimension() != Level.OVERWORLD) {
            source.sendSuccess(() -> Component.literal("Outcome: Dimension is not Overworld. Vanilla FD growth applies.").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        if (RegnumCropSync.isUndergroundBlocked(level, targetPos)) {
            source.sendSuccess(() -> Component.literal("Outcome: TICK CANCELLED. Underground crop growth is strictly prohibited.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            return 1;
        }

        double multiplier = RegnumCropSync.getMultiplier(level, targetPos, state);
        if (multiplier <= 0.0) {
            source.sendSuccess(() -> Component.literal("Outcome: TICK CANCELLED. Climate multiplier is 0.0x for this biome.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            return 1;
        }

        boolean willCancel = RegnumCropSync.shouldCancelGrowth(level, targetPos, state, level.random);
        if (willCancel) {
            source.sendSuccess(() -> Component.literal("Outcome: TICK CANCELLED by regional climate penalty (multiplier " + String.format("%.2fx", multiplier) + ").").withStyle(ChatFormatting.RED), false);
        } else {
            source.sendSuccess(() -> Component.literal("Outcome: TICK PASSED. Plant successfully passed climate & underground filters.").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
        }

        return 1;
    }

    private static int executeGrow(CommandContext<CommandSourceStack> ctx, int radius) {
        CommandSourceStack source = ctx.getSource();
        Entity entity = source.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be executed by a player."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos centerPos = getTargetPos(player);
        int grownCount = 0;

        for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-radius, -radius, -radius), centerPos.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock bonemealable) {
                if (bonemealable.isValidBonemealTarget(level, pos, state)) {
                    bonemealable.performBonemeal(level, level.random, pos, state);
                    grownCount++;
                }
            } else {
                IntegerProperty ageProp = findAgeProperty(state);
                if (ageProp != null) {
                    int currentAge = state.getValue(ageProp);
                    int maxAge = ageProp.getPossibleValues().stream().max(Integer::compareTo).orElse(currentAge);
                    if (currentAge < maxAge) {
                        level.setBlock(pos, state.setValue(ageProp, currentAge + 1), 2);
                        grownCount++;
                    }
                }
            }
        }

        final int count = grownCount;
        source.sendSuccess(() -> Component.literal("Grew " + count + " plant(s) within radius " + radius + ".").withStyle(ChatFormatting.GREEN), true);
        return grownCount;
    }

    private static IntegerProperty findAgeProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof IntegerProperty intProp && prop.getName().toLowerCase().contains("age")) {
                return intProp;
            }
        }
        return null;
    }
}
