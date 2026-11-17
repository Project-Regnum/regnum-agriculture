package vectorwing.farmersdelight.integration.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.network.chat.Component;
import vectorwing.farmersdelight.FarmersDelight;
import vectorwing.farmersdelight.common.Configuration;
import vectorwing.farmersdelight.common.registry.ModItems;
import vectorwing.farmersdelight.refabricated.mlconfigs.fabric.FabricConfigListScreen;

public class ModMenuCompat implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new FabricConfigListScreen(com.projectregnum.agriculture.RegnumAgriculture.MOD_ID, ModItems.STOVE.get().getDefaultInstance(),
                Component.literal("Regnum Agriculture"), null,
                parent, Configuration.CLIENT_CONFIG, Configuration.COMMON_CONFIG);
    }

}
