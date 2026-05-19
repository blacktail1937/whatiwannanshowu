package com.blacktail92.whatiwannashowu;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    public static final boolean IS_JEI_LOADED = ModList.get().isLoaded("jei");

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_COMPRESSED_SIZE = BUILDER
            .comment("Maximum compressed NBT size in kilobytes before stripping item components.")
            .defineInRange("maxCompressedSize", 128, 128, 512);

    public static final ModConfigSpec.IntValue SHARE_COOLDOWN = BUILDER
            .comment("Minimum cooldown between shares in milliseconds. 0 = no cooldown.")
            .defineInRange("shareCooldown", 500, 0, 60000);

    static final ModConfigSpec SPEC = BUILDER.build();
}
