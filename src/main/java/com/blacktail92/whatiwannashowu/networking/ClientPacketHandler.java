package com.blacktail92.whatiwannashowu.networking;

import com.blacktail92.whatiwannashowu.client.ClientEvents;
import com.blacktail92.whatiwannashowu.client.ItemCache;
import com.mojang.logging.LogUtils;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.io.IOException;

public class ClientPacketHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void handleShareItem(ShareItemPayload payload) {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        var player = mc.getConnection().getPlayerInfo(payload.senderUUID());
        var senderName = (player != null) ? player.getProfile().getName() : "Player";

        try {
            var nbt = ItemCache.decompress(payload.nbt());
            var stack = ItemStack.of(nbt);
            var link = ClientEvents.createItemLink(stack);
            var message = Component.literal("<")
                    .append(Component.literal(senderName))
                    .append(Component.literal("> "))
                    .append(link);

            mc.gui.getChat().addMessage(message, null, GuiMessageTag.system());
        } catch (IOException e) {
            LOGGER.error("Error while trying to handle share item link to client", e);
        }
    }
}
