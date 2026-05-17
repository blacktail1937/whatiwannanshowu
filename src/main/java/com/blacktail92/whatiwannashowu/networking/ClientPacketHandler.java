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
import java.util.Optional;

public class ClientPacketHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void handleShareItem(ShareItemPayload payload) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var info = mc.getConnection().getPlayerInfo(payload.senderUUID());
        var senderName = (info != null) ? info.getProfile().getName() : "Player";

        Optional.ofNullable(mc.player)
                .map(player -> {
                    try {
                        var nbt = ItemCache.decompress(payload.nbt());
                        return ItemStack.parse(player.registryAccess(), nbt);
                    } catch (IOException e) {
                        LOGGER.error("Failed to parse item stack!", e);
                    }
                    return Optional.<ItemStack>empty();
                })
                .flatMap(stack -> stack)
                .filter(stack -> !stack.isEmpty())
                .map(stack -> {
                    var link = ClientEvents.createItemLink(stack);
                    return Component.literal("<")
                            .append(Component.literal(senderName))
                            .append(Component.literal("> "))
                            .append(link);
                })
                .ifPresent(message -> mc.gui.getChat().addMessage(message, null, GuiMessageTag.system()));
    }
}
