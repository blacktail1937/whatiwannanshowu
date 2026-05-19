package com.blacktail92.whatiwannashowu;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;


// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = WhatIWannaShowU.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final boolean IS_JEI_LOADED = ModList.get().isLoaded("jei");

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_COMPRESSED_SIZE = BUILDER
            .comment("Maximum compressed NBT size in kilobytes before stripping item data to bare minimum (id + count).")
            .defineInRange("maxCompressedSize", 128, 1, 512);

    public static final ForgeConfigSpec.IntValue SHARE_COOLDOWN = BUILDER
            .comment("Minimum cooldown between shares in milliseconds. 0 = no cooldown.")
            .defineInRange("shareCooldown", 1000, 0, 60000);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {

    }
}
