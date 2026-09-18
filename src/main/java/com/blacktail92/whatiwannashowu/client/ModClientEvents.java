package com.blacktail92.whatiwannashowu.client;

import com.blacktail92.whatiwannashowu.WhatIWannaShowU;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = WhatIWannaShowU.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    static void onKeyRegister(RegisterKeyMappingsEvent event) {
        LOGGER.info("register short key");
        ClientEvents.SHARED_KEY = new KeyMapping("key.whatiwannashowu.share_item", InputConstants.KEY_G, "key.categories.whatiwannashowu");
        event.register(ClientEvents.SHARED_KEY);

        ClientEvents.SHARED_KEY_VIA_AIM_POINT = new KeyMapping("key.whatiwannashowu.share_looked_at", InputConstants.KEY_V, "key.categories.whatiwannashowu");
        event.register(ClientEvents.SHARED_KEY_VIA_AIM_POINT);
    }

    /**
     * 注册游戏内的配置界面 —— 注册之后，mod 列表里选中本 mod 时下方的 "Config" 按钮才会亮起来。
     *
     * <p>Forge 1.20.1 只提供 {@link ConfigScreenHandler.ConfigScreenFactory} 这个入口，
     * 不自带配置界面（不像 NeoForge 1.21.1 会按配置自动生成），所以必须在这里挂上自己的界面。
     */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ModList.get().getModContainerById(WhatIWannaShowU.MODID).ifPresent(container ->
                container.registerExtensionPoint(
                        ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent))));
    }
}
