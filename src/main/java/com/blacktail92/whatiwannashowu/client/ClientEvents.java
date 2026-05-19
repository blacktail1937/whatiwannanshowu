package com.blacktail92.whatiwannashowu.client;

import com.blacktail92.whatiwannashowu.Config;
import com.blacktail92.whatiwannashowu.WhatIwannashowU;
import com.blacktail92.whatiwannashowu.integration.JeiPlugin;
import com.blacktail92.whatiwannashowu.networking.ShareItemPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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

import java.io.IOException;
import java.util.Optional;


@EventBusSubscriber(modid = WhatIwannashowU.MODID, value = Dist.CLIENT)
public class ClientEvents {
    static final Logger LOGGER = LogUtils.getLogger();
    static KeyMapping SHARED_KEY;
    static long LAST_SHARE_TIME;

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
                    if (!stack.isEmpty()) {
                        var nbt = stack.save(mc.player.registryAccess());
                        LOGGER.info("nbt:{}", nbt);
                        sendSharePacket(mc, stack);
                    }
                }
            }
        }
    }

    static void sendSharePacket(Minecraft mc, ItemStack stack) {
        if (mc.player == null) return;

        var now = System.currentTimeMillis();
        int cooldown = Config.SHARE_COOLDOWN.get();
        if (cooldown > 0 && now - LAST_SHARE_TIME < cooldown) {
            var remaining = (int) Math.ceil((LAST_SHARE_TIME + cooldown - now) / 1000.0);
            mc.player.displayClientMessage(
                    Component.translatable("message.whatiwannashowu.share_cooldown", remaining), true);
            return;
        }
        LAST_SHARE_TIME = now;

        if (mc.getConnection() != null) {
            try {
                //compress
                var nbt = (CompoundTag) stack.save(mc.player.registryAccess());
                LOGGER.info("nbt:{}", nbt);
                mc.getConnection().send(
                        new ShareItemPayload(mc.player.getUUID(), ItemCache.compress(nbt))
                );
            } catch (IOException e) {
                LOGGER.error("Error while sending share packet", e);
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
                        var mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            var hash = ItemCache.put(stack, mc.player.registryAccess());
                            var cmd = "/wiwsu_lookup " + hash;
//                            var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                            updateStyle = updateStyle.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                        }
                    }

                    return updateStyle
                            .withUnderlined(true)
                            .withHoverEvent(itemHover);
                });
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        if (Config.IS_JEI_LOADED) return;

        var key = "hash";
        event.getDispatcher().register(
                Commands.literal("wiwsu_lookup")
                        .then(Commands.argument(key, StringArgumentType.word())
                                .suggests(((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        BuiltInRegistries.ITEM.keySet(), builder
                                )))
                                .executes(context -> {
//                                        var itemId = ResourceLocationArgument.getId(context, key);
//                                        var item = BuiltInRegistries.ITEM.get(itemId);
                                    var hash = StringArgumentType.getString(context, key);
                                    var mc = Minecraft.getInstance();
                                    Optional.ofNullable(mc.player)
                                            .ifPresent(player ->
                                                    mc.execute(() -> JeiPlugin.showRecipe(ItemCache.get(hash, mc.player.registryAccess()))));

                                    return 1;
                                }))
        );
    }
}
