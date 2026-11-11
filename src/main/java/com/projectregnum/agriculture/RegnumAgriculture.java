// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture;

import com.projectregnum.agriculture.agriculture.CropGrowthService;
import com.projectregnum.agriculture.command.SeasonCommand;
import com.projectregnum.agriculture.networking.SeasonPayload;
import com.projectregnum.agriculture.season.SeasonManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RegnumAgriculture implements ModInitializer {
    public static final String MOD_ID = "regnum_agriculture";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static boolean initialized;
    @Override public void onInitialize() {
        if (initialized) return;
        initialized = true;
        PayloadTypeRegistry.playS2C().register(SeasonPayload.TYPE, SeasonPayload.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> SeasonCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SeasonManager.get(server).tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(SeasonManager::stop);
        ServerTickEvents.END_SERVER_TICK.register(server -> SeasonManager.get(server).tick());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SeasonManager.get(server).sync(handler.player));
        if (!CropGrowthService.ownsCropTicks()) LOGGER.warn("Regnum World detected: seasonal crop tick modification disabled to prevent stacking. See docs/REGNUM_WORLD_COMPATIBILITY.md.");
        LOGGER.info("Regnum Agriculture seasonal foundation initialized");
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
}
