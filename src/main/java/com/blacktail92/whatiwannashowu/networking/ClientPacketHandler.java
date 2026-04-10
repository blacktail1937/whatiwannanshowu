package com.blacktail92.whatiwannashowu.networking;

import com.blacktail92.whatiwannashowu.client.ClientEvents;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ClientPacketHandler {
    public static void handleShareItem(ShareItemPayload payload) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var info = mc.getConnection().getPlayerInfo(payload.senderUUID());
        var senderName = (info != null) ? info.getProfile().getName() : "Player";

        var link = ClientEvents.createItemLink(payload.stack());
        var message = Component.literal("<")
                .append(Component.literal(senderName))
                .append(Component.literal("> "))
                .append(link);

        mc.gui.getChat().addMessage(message, null, GuiMessageTag.system());
    }
}
