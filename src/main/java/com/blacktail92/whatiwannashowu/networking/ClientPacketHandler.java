package com.blacktail92.whatiwannashowu.networking;

import com.blacktail92.whatiwannashowu.client.ClientKeyEvents;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ForgeHooksClient;

public class ClientPacketHandler {
    public static void handleShareItem(ShareItemPayload payload) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var player = mc.level.getPlayerByUUID(payload.senderUUID());
        var senderName = (player != null) ? player.getName().getString() : "Player";

        var link = ClientKeyEvents.createItemLink(payload.stack());
        var message = Component.literal("<")
                .append(Component.literal(senderName))
                .append(Component.literal("> "))
                .append(link);

        mc.gui.getChat().addMessage(message, null, GuiMessageTag.system());
    }
}
