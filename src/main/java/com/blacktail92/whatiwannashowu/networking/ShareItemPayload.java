package com.blacktail92.whatiwannashowu.networking;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ShareItemPayload(UUID senderUUID, byte[] nbt) implements CustomPacketPayload {
    public static final Type<ShareItemPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("whatiwannashowu", "share_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShareItemPayload> STEAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ShareItemPayload::senderUUID,
            // 2MB max
            ByteBufCodecs.byteArray(2 * 1024 * 1024), ShareItemPayload::nbt,
            ShareItemPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
