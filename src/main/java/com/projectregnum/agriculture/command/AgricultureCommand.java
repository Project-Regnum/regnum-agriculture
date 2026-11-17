package com.projectregnum.agriculture.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.projectregnum.agriculture.sync.RegnumCropSync;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

/** Read-only farming information. World owns its own calendar and commands. */
public final class AgricultureCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.register(Commands.literal("agriculture")
                .executes(AgricultureCommand::status)
                .then(Commands.literal("status").executes(AgricultureCommand::status))
                .then(Commands.literal("season").executes(AgricultureCommand::season))
                .then(Commands.literal("check").executes(AgricultureCommand::check)));
        dispatcher.register(Commands.literal("agri").redirect(root).executes(AgricultureCommand::status));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        String version = FabricLoader.getInstance().getModContainer("regnum-agriculture").orElseThrow()
                .getMetadata().getVersion().getFriendlyString();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.regnum_agriculture.status", version), false);
        String key = RegnumCropSync.isRegnumWorldLoaded() ? "connected"
                : FabricLoader.getInstance().isModLoaded("regnum-world") ? "unavailable" : "standalone";
        ctx.getSource().sendSuccess(() -> Component.translatable("command.regnum_agriculture." + key), false);
        return 1;
    }

    private static int season(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.translatable("command.regnum_agriculture.season_unavailable"), false);
        return 1;
    }

    private static int check(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.translatable("command.regnum_agriculture.player_only"));
            return 0;
        }
        if (!(player.pick(5, 0, false) instanceof BlockHitResult hit)) {
            ctx.getSource().sendFailure(Component.translatable("command.regnum_agriculture.look_at_crop"));
            return 0;
        }
        var level = player.serverLevel();
        var pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        double rate = RegnumCropSync.getMultiplier(level, pos, state);
        boolean blocked = RegnumCropSync.isUndergroundBlocked(level, pos) || rate <= 0;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.regnum_agriculture.crop",
                BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
                String.format(java.util.Locale.ROOT, "%.2f", rate),
                Component.translatable("command.regnum_agriculture." + (blocked ? "blocked" : "allowed"))), false);
        return 1;
    }
}
