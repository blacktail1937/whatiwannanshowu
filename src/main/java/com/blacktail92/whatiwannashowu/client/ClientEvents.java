package com.blacktail92.whatiwannashowu.client;

import com.blacktail92.whatiwannashowu.Config;
import com.blacktail92.whatiwannashowu.WhatIWannaShowU;
import com.blacktail92.whatiwannashowu.integration.JeiPlugin;
import com.blacktail92.whatiwannashowu.networking.ModMessages;
import com.blacktail92.whatiwannashowu.networking.ShareItemPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = WhatIWannaShowU.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static KeyMapping SHARED_KEY;

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

    @SubscribeEvent
    static void onCliengLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ItemCache.clear();
    }

    static void sendSharePacket(Minecraft mc, ItemStack stack) {
        if (mc.player != null && mc.getConnection() != null) {
            try {
                var nbt = stack.serializeNBT();

                ModMessages.sendToServer(new ShareItemPayload(mc.player.getUUID(), ItemCache.compress(nbt)));
            } catch (IOException e) {
                LOGGER.error("Failed to send share packet", e);
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

                    if (Config.IS_JEI_LOADED) {
                        var hash = ItemCache.put(stack);
                        if (!hash.isEmpty()) {
                            var cmd = "/wiwsu_lookup " + hash;
                            // LOGGER.info("点击指令是 {}", cmd);
                            // var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
                            updateStyle = updateStyle.withClickEvent(
                                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd)
                            );
                        }
                    }

                    return updateStyle
                            .withUnderlined(true)
                            .withHoverEvent(itemHover);
                });
    }
}
