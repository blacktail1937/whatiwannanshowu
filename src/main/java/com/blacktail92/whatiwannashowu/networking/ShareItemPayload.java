package com.blacktail92.whatiwannashowu.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record ShareItemPayload(UUID senderUUID, ItemStack stack) {

    public static void encode(ShareItemPayload payload, FriendlyByteBuf buf) {
        buf.writeUUID(payload.senderUUID);
        buf.writeItem(payload.stack);
    }

    public static ShareItemPayload decode(FriendlyByteBuf buf) {
        return new ShareItemPayload(buf.readUUID(), buf.readItem());
    }

    public static void handle(ShareItemPayload payload, Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                var server = context.getSender().getServer();
                if (server != null) {
                    var relay = new ShareItemPayload(context.getSender().getUUID(), payload.stack);
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
