// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.client;

import com.projectregnum.agriculture.networking.SeasonPayload;
import com.projectregnum.agriculture.season.*;
import net.minecraft.client.Minecraft;

public final class ClientSeasonState {
    private static SeasonPayload payload;
    private ClientSeasonState() {}
    public static void receive(SeasonPayload next) {
        if (next.lengthDays() >= 1 && next.lengthDays() <= SeasonCalendar.MAX_LENGTH_DAYS) payload = next;
    }
    public static void clear() { payload = null; }
    public static boolean active() {
        var world = Minecraft.getInstance().level;
        return payload != null && payload.active() && world != null
                && payload.dimension().equals(world.dimension().location().toString());
    }
    public static boolean colors() { return active() && payload.colors(); }
    public static boolean weather() { return active() && payload.weather(); }
    public static SeasonCalendar.Snapshot snapshot() {
        var world = Minecraft.getInstance().level;
        if (!active()) return SeasonCalendar.fromCycleTick(0, 24);
        return SeasonCalendar.at(world.getDayTime(), payload.referenceTime(), payload.phase(), payload.lengthDays());
    }
}
