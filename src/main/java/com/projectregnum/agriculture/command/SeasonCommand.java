// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.command;

import com.mojang.brigadier.CommandDispatcher;
import com.projectregnum.agriculture.season.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class SeasonCommand {
    private SeasonCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var season = Commands.literal("season").executes(context -> query(context.getSource()));
        var set = Commands.literal("set").requires(source -> source.hasPermission(2));
        for (Season value : Season.values()) {
            set.then(Commands.literal(value.id()).executes(context -> set(context.getSource(), value)));
        }
        season.then(set).then(Commands.literal("next").requires(source -> source.hasPermission(2))
                .executes(context -> set(context.getSource(),
                        SeasonManager.get(context.getSource().getServer()).getCurrentSeason(context.getSource().getLevel()).next())));
        dispatcher.register(Commands.literal("regnumagriculture").then(season)
                .then(Commands.literal("reload").requires(source -> source.hasPermission(2)).executes(context -> {
                    boolean loaded = SeasonManager.get(context.getSource().getServer()).reload();
                    if (loaded) context.getSource().sendSuccess(() -> Component.translatable("commands.regnum_agriculture.reloaded"), true);
                    else context.getSource().sendFailure(Component.translatable("commands.regnum_agriculture.reload_failed"));
                    return loaded ? 1 : 0;
                })));
    }
    private static int query(CommandSourceStack source) {
        SeasonManager manager = SeasonManager.get(source.getServer());
        if (!manager.isSeasonal(source.getLevel())) {
            source.sendSuccess(() -> Component.translatable("commands.regnum_agriculture.inactive"), false);
            return 0;
        }
        var snapshot = manager.snapshot(source.getLevel());
        source.sendSuccess(() -> Component.translatable("commands.regnum_agriculture.season",
                Component.translatable(snapshot.season().translationKey()), snapshot.day(), manager.getSeasonLengthDays()), false);
        return snapshot.season().ordinal() + 1;
    }
    private static int set(CommandSourceStack source, Season season) {
        SeasonManager manager = SeasonManager.get(source.getServer());
        if (!manager.isSeasonal(source.getLevel())) {
            source.sendFailure(Component.translatable("commands.regnum_agriculture.inactive"));
            return 0;
        }
        manager.setSeason(season);
        source.sendSuccess(() -> Component.translatable("commands.regnum_agriculture.set",
                Component.translatable(season.translationKey())), true);
        return 1;
    }
}
