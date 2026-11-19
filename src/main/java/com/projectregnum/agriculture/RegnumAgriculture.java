package com.projectregnum.agriculture;

import com.projectregnum.agriculture.command.AgricultureCommand;
import com.projectregnum.agriculture.sync.RegnumCropSync;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import vectorwing.farmersdelight.FarmersDelight;

public class RegnumAgriculture implements ModInitializer {
    public static final String MOD_ID = "regnum-agriculture";
    public static final Logger LOGGER = LogManager.getLogger("RegnumAgriculture");

    @Override
    public void onInitialize() {
        FarmersDelight fd = new FarmersDelight();
        fd.onInitialize();
        RegnumCropSync.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AgricultureCommand.register(dispatcher);
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
