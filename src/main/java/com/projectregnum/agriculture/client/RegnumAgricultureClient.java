package com.projectregnum.agriculture.client;

import net.fabricmc.api.ClientModInitializer;
import vectorwing.farmersdelight.client.FarmersDelightClient;

public class RegnumAgricultureClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FarmersDelightClient client = new FarmersDelightClient();
        client.onInitializeClient();
    }
}
