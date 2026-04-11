package com.blacktail92.whatiwannashowu.networking;

import com.blacktail92.whatiwannashowu.WhatIWannaShowU;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;


public class ModMessages {
    private static SimpleChannel CHANNEL;
    private static int PACKET_ID = 0;

    private static int id() {
        return PACKET_ID++;
    }

    public static void register() {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(WhatIWannaShowU.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        CHANNEL.messageBuilder(ShareItemPayload.class, id())
                .decoder(ShareItemPayload::decode)
                .encoder(ShareItemPayload::encode)
                .consumerMainThread(ShareItemPayload::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG payload) {
        if (CHANNEL != null)
            CHANNEL.sendToServer(payload);
    }

    public static <MSG> void sendToPlayer(MSG payload, ServerPlayer player) {
        if (CHANNEL != null)
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
