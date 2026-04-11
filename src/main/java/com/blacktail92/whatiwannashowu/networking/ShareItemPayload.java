package com.blacktail92.whatiwannashowu.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ShareItemPayload {
    private final UUID senderUUID;
    private final ItemStack stack;

    public ShareItemPayload(UUID senderUUID, ItemStack stack) {
        this.senderUUID = senderUUID;
        this.stack = stack;
    }

    public UUID senderUUID() {
        return senderUUID;
    }

    public ItemStack stack() {
        return stack;
    }

    public static void encode(ShareItemPayload paylod, FriendlyByteBuf buf) {
        buf.writeUUID(paylod.senderUUID);
        buf.writeItem(paylod.stack);
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
