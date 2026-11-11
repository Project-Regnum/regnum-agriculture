// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.season;

import com.projectregnum.agriculture.RegnumAgriculture;
import com.projectregnum.agriculture.config.ConfigStore;
import com.projectregnum.agriculture.config.ServerConfig;
import com.projectregnum.agriculture.networking.SeasonPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class SeasonManager {
    private static final Map<MinecraftServer, SeasonManager> INSTANCES = new IdentityHashMap<>();
    private final MinecraftServer server;
    private final ConfigStore store;
    private final SeasonState state;
    private final Map<String, Season> lastSeasons = new HashMap<>();
    private final Map<UUID, String> playerDimensions = new HashMap<>();
    private int syncTicks;

    private SeasonManager(MinecraftServer server) {
        this.server = server;
        store = new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("regnum_agriculture.json"),
                message -> RegnumAgriculture.LOGGER.warn(message));
        store.reload();
        state = server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(() -> new SeasonState(config().seasonLengthDays()), SeasonState::load, null),
                "regnum_agriculture_seasons");
        state.resize(time(), config().seasonLengthDays());
    }

    public static SeasonManager get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, SeasonManager::new);
    }
    public static void stop(MinecraftServer server) { INSTANCES.remove(server); }
    public ServerConfig config() { return store.current(); }
    private long time() { return server.overworld().getDayTime(); }

    public boolean isSeasonal(ServerLevel level) {
        return config().seasonalDimensions().contains(level.dimension().location().toString())
                && level.dimensionType().natural() && level.dimensionType().hasSkyLight()
                && !level.dimensionType().hasCeiling();
    }
    public Season getCurrentSeason(ServerLevel level) { return snapshot(level).season(); }
    public int getCurrentDay(ServerLevel level) { return snapshot(level).day(); }
    public float getSeasonProgress(ServerLevel level) { return snapshot(level).progress(); }
    public int getSeasonLengthDays() { return state.lengthDays(); }
    public SeasonCalendar.Snapshot snapshot(ServerLevel level) {
        return isSeasonal(level) ? state.snapshot(time()) : SeasonCalendar.fromCycleTick(0, state.lengthDays());
    }
    public void setSeason(Season season) {
        state.setSeason(time(), season);
        checkTransitions();
        syncAll();
    }
    public boolean reload() {
        if (!store.reload()) return false;
        state.resize(time(), config().seasonLengthDays());
        checkTransitions();
        syncAll();
        return true;
    }
    public void tick() {
        boolean changed = checkTransitions();
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            String dimension = player.serverLevel().dimension().location().toString();
            if (!dimension.equals(playerDimensions.put(player.getUUID(), dimension))) sync(player);
        }
        playerDimensions.keySet().retainAll(online);
        if (changed || ++syncTicks >= 100) { syncAll(); syncTicks = 0; }
    }
    private boolean checkTransitions() {
        boolean changed = false;
        for (ServerLevel level : server.getAllLevels()) {
            String dimension = level.dimension().location().toString();
            if (!isSeasonal(level)) { lastSeasons.remove(dimension); continue; }
            Season current = getCurrentSeason(level);
            Season previous = lastSeasons.put(dimension, current);
            if (previous != null && previous != current) {
                changed = true;
                SeasonEvents.TRANSITION.invoker().onTransition(level, previous, current);
            }
        }
        return changed;
    }
    public void syncAll() { server.getPlayerList().getPlayers().forEach(this::sync); }
    public void sync(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (ServerPlayNetworking.canSend(player, SeasonPayload.TYPE)) {
            ServerPlayNetworking.send(player, new SeasonPayload(level.dimension().location().toString(),
                    level.getDayTime(), state.phase(time()), state.lengthDays(), isSeasonal(level),
                    config().enableSeasonalColors(), config().enableSeasonalWeather()));
        }
    }
}
