// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.season;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** Stored once in the overworld; only anchors change, so no per-tick disk writes. */
public final class SeasonState extends SavedData {
    private long anchorTime;
    private long anchorPhase;
    private int lengthDays;

    public SeasonState(int lengthDays) { this.lengthDays = lengthDays; setDirty(); }

    public static SeasonState load(CompoundTag tag, HolderLookup.Provider registries) {
        int days = tag.getInt("LengthDays");
        if (days < 1 || days > SeasonCalendar.MAX_LENGTH_DAYS) days = 24;
        SeasonState state = new SeasonState(days);
        state.anchorTime = tag.getLong("AnchorTime");
        state.anchorPhase = Math.floorMod(tag.getLong("AnchorPhase"), 4 * SeasonCalendar.seasonTicks(days));
        return state;
    }

    public long phase(long time) { return SeasonCalendar.cycleTick(time, anchorTime, anchorPhase, lengthDays); }
    public SeasonCalendar.Snapshot snapshot(long time) { return SeasonCalendar.fromCycleTick(phase(time), lengthDays); }
    public int lengthDays() { return lengthDays; }

    public void setSeason(long time, Season season) {
        anchorTime = time;
        anchorPhase = season.ordinal() * SeasonCalendar.seasonTicks(lengthDays);
        setDirty();
    }

    public void resize(long time, int days) {
        if (lengthDays == days) return;
        anchorPhase = SeasonCalendar.resize(phase(time), lengthDays, days);
        anchorTime = time;
        lengthDays = days;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Version", 1);
        tag.putLong("AnchorTime", anchorTime);
        tag.putLong("AnchorPhase", anchorPhase);
        tag.putInt("LengthDays", lengthDays);
        return tag;
    }
}
