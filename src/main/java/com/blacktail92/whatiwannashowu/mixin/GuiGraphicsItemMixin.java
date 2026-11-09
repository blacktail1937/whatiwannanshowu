package com.blacktail92.whatiwannashowu.mixin;

import com.blacktail92.whatiwannashowu.client.GuiItemPicker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 记录"界面上画出来的物品"——三层兜底里的第 ② 层，用来支持自绘界面
 * （RS 网格、Chipped 的槽位、Sawmill 中间的配方列表等）。
 *
 * <p>目标是 GuiGraphics 里那个 7 参私有重载：
 * <pre>private void renderItem(LivingEntity, Level, ItemStack, int x, int y, int seed, int guiOffset)</pre>
 * 它是所有物品绘制的唯一汇聚点，而且 translate(x + 8, y + 8) 是在它内部做的，
 * 所以 HEAD 处拿到的 (x, y) 正好是"界面自己那套变换下的 16×16 方格左上角"。
 */
@Mixin(GuiGraphics.class)
public class GuiGraphicsItemMixin {

    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("HEAD")
    )
    private void wiwsu$recordItemPosition(LivingEntity entity, Level level, ItemStack stack,
                                          int x, int y, int seed, int guiOffset, CallbackInfo ci) {
        GuiItemPicker.record((GuiGraphics) (Object) this, stack, x, y);
    }
}
