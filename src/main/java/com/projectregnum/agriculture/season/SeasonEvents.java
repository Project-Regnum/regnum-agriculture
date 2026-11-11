// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.season;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;

public final class SeasonEvents {
    private SeasonEvents() {}
    /** Runs on the server thread after an observed transition, including manual changes. */
    public static final Event<Transition> TRANSITION = EventFactory.createArrayBacked(Transition.class,
            listeners -> (level, previous, current) -> {
                for (Transition listener : listeners) listener.onTransition(level, previous, current);
            });
    @FunctionalInterface
    public interface Transition { void onTransition(ServerLevel level, Season previous, Season current); }
}
