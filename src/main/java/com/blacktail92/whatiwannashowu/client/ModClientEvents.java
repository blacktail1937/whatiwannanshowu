package com.blacktail92.whatiwannashowu.client;

import com.blacktail92.whatiwannashowu.WhatIWannaShowU;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = WhatIWannaShowU.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    static void onKeyRegister(RegisterKeyMappingsEvent event) {
        LOGGER.info("register short key");
        ClientEvents.SHARED_KEY = new KeyMapping("key.whatiwannashowu.share_item", InputConstants.KEY_G, "key.categories.whatiwannashowu");
        event.register(ClientEvents.SHARED_KEY);
    }
}
