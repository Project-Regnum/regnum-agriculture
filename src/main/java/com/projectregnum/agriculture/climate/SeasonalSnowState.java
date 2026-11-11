// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.climate;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Only snow first placed by seasonal precipitation is eligible for seasonal thaw. */
public final class SeasonalSnowState extends SavedData {
    private final LongOpenHashSet positions = new LongOpenHashSet();
    public static SeasonalSnowState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(SeasonalSnowState::new,
                SeasonalSnowState::load, null), "regnum_agriculture_snow");
    }
    public static SeasonalSnowState load(CompoundTag tag, HolderLookup.Provider registries) {
        SeasonalSnowState state = new SeasonalSnowState();
        for (long pos : tag.getLongArray("Positions")) state.positions.add(pos);
        return state;
    }
    public boolean contains(BlockPos pos) { return positions.contains(pos.asLong()); }
    public void add(BlockPos pos) { if (positions.add(pos.asLong())) setDirty(); }
    public void remove(BlockPos pos) { if (positions.remove(pos.asLong())) setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("Positions", positions.toLongArray());
        return tag;
    }
}
