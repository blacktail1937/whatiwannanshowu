package com.blacktail92.whatiwannashowu.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public record ShareItemPayload(UUID senderUUID, byte[] nbt) {

    public static void encode(ShareItemPayload payload, FriendlyByteBuf buf) {
        buf.writeUUID(payload.senderUUID);
        buf.writeByteArray(payload.nbt);
    }

    public static ShareItemPayload decode(FriendlyByteBuf buf) {
        return new ShareItemPayload(buf.readUUID(), buf.readByteArray());
    }

    public static void handle(ShareItemPayload payload, Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                var server = Objects.requireNonNull(context.getSender()).getServer();
                if (server != null) {
                    var relay = new ShareItemPayload(context.getSender().getUUID(), payload.nbt);
                    for (var p : server.getPlayerList().getPlayers())
                        ModMessages.sendToPlayer(relay, p);
                }
            } else {
                ClientPacketHandler.handleShareItem(payload);
            }
        });
        context.setPacketHandled(true);
    }
}
