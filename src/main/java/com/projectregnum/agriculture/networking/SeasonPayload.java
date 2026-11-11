// SPDX-License-Identifier: MPL-2.0
// Fabric custom-payload pattern adapted from Fabric Seasons payload/ConfigSyncPacket.java, revision e0b2dd285e5a7f0b2850871ea7a0eb5289d0937c.
// Original authors: D4rkness_King and Fabric Seasons contributors. See THIRD_PARTY_NOTICES.md.
package com.projectregnum.agriculture.networking;

import com.projectregnum.agriculture.RegnumAgriculture;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SeasonPayload(String dimension, long referenceTime, long phase, int lengthDays,
        boolean active, boolean colors, boolean weather) implements CustomPacketPayload {
    public static final Type<SeasonPayload> TYPE = new Type<>(RegnumAgriculture.id("season_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SeasonPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.dimension, 256);
                buffer.writeLong(payload.referenceTime);
                buffer.writeLong(payload.phase);
                buffer.writeVarInt(payload.lengthDays);
                buffer.writeBoolean(payload.active);
                buffer.writeBoolean(payload.colors);
                buffer.writeBoolean(payload.weather);
            }, buffer -> new SeasonPayload(buffer.readUtf(256), buffer.readLong(), buffer.readLong(),
                    buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
