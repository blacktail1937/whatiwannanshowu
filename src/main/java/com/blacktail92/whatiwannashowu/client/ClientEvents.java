package com.blacktail92.whatiwannashowu.client;

import com.blacktail92.whatiwannashowu.Config;
import com.blacktail92.whatiwannashowu.WhatIwannashowU;
import com.blacktail92.whatiwannashowu.integration.JeiPlugin;
import com.blacktail92.whatiwannashowu.networking.ShareItemPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;

import java.util.Optional;


@EventBusSubscriber(modid = WhatIwannashowU.MODID, value = Dist.CLIENT)
public class ClientEvents {
    static KeyMapping SHARED_KEY;
    static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    static void onKeyRegister(RegisterKeyMappingsEvent event) {
        SHARED_KEY = new KeyMapping("key.whatiwannashowu.share_item", InputConstants.KEY_G, "key.categories.whatiwannashowu");
        event.register(SHARED_KEY);
    }

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
    static void onClientTick(ClientTickEvent.Post event) {
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

    static void sendSharePacket(Minecraft mc, ItemStack stack) {
        if (mc.player != null && mc.getConnection() != null) {
            mc.getConnection().send(
                    new ShareItemPayload(mc.player.getUUID(), stack)
            );
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
                    var updateStyle = style
                            .withColor(ChatFormatting.AQUA)
                            .withUnderlined(true)
                            .withHoverEvent(itemHover);

                    if (Config.IS_JEI_LOADED) {
                        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        updateStyle = updateStyle.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wiwsu_lookup " + itemId));
                    }

                    return updateStyle;
                });
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var key = "itemId";
        event.getDispatcher().register(
                Commands.literal("wiwsu_lookup")
                        .then(Commands.argument(key, ResourceLocationArgument.id())
                                .executes(context -> {
                                    if (Config.IS_JEI_LOADED) {
                                        var itemId = ResourceLocationArgument.getId(context, key);
                                        var item = BuiltInRegistries.ITEM.get(itemId);

                                        Minecraft.getInstance().execute(() -> JeiPlugin.showRecipe(new ItemStack(item)));
                                    }

                                    return 1;
                                }))
        );
    }
}
