package com.blacktail92.whatiwannashowu.networking;

import com.blacktail92.whatiwannashowu.Config;
import com.blacktail92.whatiwannashowu.client.ItemCache;
import com.blacktail92.whatiwannashowu.integration.JeiPlugin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public record OpenJeiPayload(String hash) {
    public static void encode(OpenJeiPayload payload, FriendlyByteBuf buf) {
        buf.writeUtf(payload.hash);
    }

    public static OpenJeiPayload decode(FriendlyByteBuf buf) {
        return new OpenJeiPayload(buf.readUtf());
    }

    public static void handle(OpenJeiPayload payload, Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Config.IS_JEI_LOADED) {
//                    var item = ForgeRegistries.ITEMS.getValue(payload.itemId);
//                    if (item != null) {
//                        JeiPlugin.showRecipe(new ItemStack(item));
//                    }
                    JeiPlugin.showRecipe(ItemCache.get(payload.hash()));
                }
            });
        });
        context.setPacketHandled(true);
    }
}
