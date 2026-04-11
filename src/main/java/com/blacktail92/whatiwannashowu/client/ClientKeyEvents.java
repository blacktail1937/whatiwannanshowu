package com.blacktail92.whatiwannashowu.client;

import com.blacktail92.whatiwannashowu.Config;
import com.blacktail92.whatiwannashowu.WhatIWannaShowU;
import com.blacktail92.whatiwannashowu.integration.JeiPlugin;
import com.blacktail92.whatiwannashowu.networking.ModMessages;
import com.blacktail92.whatiwannashowu.networking.ShareItemPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = WhatIWannaShowU.MODID, value = Dist.CLIENT)
public class ClientKeyEvents {
    public static KeyMapping SHARED_KEY;
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    static void onScreenKey(ScreenEvent.KeyReleased.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.screen == null) return;

        if (SHARED_KEY.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) {
            var stack = Optional.of(mc.screen)
                    .filter(AbstractContainerScreen.class::isInstance)
                    .map(AbstractContainerScreen.class::cast)
                    .map(AbstractContainerScreen::getSlotUnderMouse)
                    .map(Slot::getItem)
                    .or(() -> Optional.ofNullable(Config.IS_JEI_LOADED ? JeiPlugin.getStackUnderMouse() : null))
                    .filter(s -> !s.isEmpty())
                    .orElse(ItemStack.EMPTY);

            if (!stack.isEmpty()) {
                sendSharePacket(mc, stack);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    static void onClientTick(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.screen == null) {
            while (SHARED_KEY.consumeClick()) {
                if (mc.player != null) {
                    var stack = mc.player.getMainHandItem();
                    if (!stack.isEmpty())
                        sendSharePacket(mc, stack);
                }
            }
        }
    }

    public static Component createItemLink(ItemStack stack) {
        var itemName = stack.getHoverName();

        var itemHover = new HoverEvent(
                HoverEvent.Action.SHOW_ITEM,
                new HoverEvent.ItemStackInfo(stack)
        );

        return Component.literal("[")
                .append(itemName)
                .append("]")
                .withStyle(style -> {
                    var updateStyle = stack.getRarity().getStyleModifier().apply(style);

                    return updateStyle
                            .withUnderlined(true)
                            .withHoverEvent(itemHover);
                });
    }

    static void sendSharePacket(Minecraft mc, ItemStack stack) {
        if (mc.player != null && mc.getConnection() != null) {
            ModMessages.sendToServer(new ShareItemPayload(mc.player.getUUID(), stack));
        }
    }
}
