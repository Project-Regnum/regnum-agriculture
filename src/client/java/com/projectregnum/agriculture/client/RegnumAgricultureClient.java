// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.client;

import com.projectregnum.agriculture.networking.SeasonPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class RegnumAgricultureClient implements ClientModInitializer {
    private int lastColorKey = Integer.MIN_VALUE;
    private int ticksSinceRebuild = 40;
    @Override public void onInitializeClient() {
        SeasonalColors.configure(ClientVisualConfig.load());
        ClientPlayNetworking.registerGlobalReceiver(SeasonPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientSeasonState.receive(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientSeasonState.clear();
            SeasonalColors.clear();
            lastColorKey = Integer.MIN_VALUE;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SeasonalColors.update();
            int key = SeasonalColors.renderKey();
            ticksSinceRebuild++;
            if (client.level != null && key != lastColorKey && ticksSinceRebuild >= 40) {
                client.level.clearTintCaches();
                client.levelRenderer.allChanged();
                lastColorKey = key;
                ticksSinceRebuild = 0;
            }
        });
    }
}
