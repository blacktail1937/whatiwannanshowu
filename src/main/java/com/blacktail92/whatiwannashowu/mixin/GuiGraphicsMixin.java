package com.blacktail92.whatiwannashowu.mixin;

import com.blacktail92.whatiwannashowu.client.ChatItemIcons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在每次画一行文字（{@link FormattedCharSequence}）之前，先把这一行里的物品图标画出来。
 *
 * <p><b>为什么挂在 GuiGraphics 上，而不是重定向 ChatComponent 里那次 drawString：</b>
 * 聊天栏每一行最终都会调用 {@code GuiGraphics#drawString(Font, FormattedCharSequence, int, int, int)}，
 * 但这一个调用点很容易被别的 mod 抢（Quark 用 MixinExtras 的 {@code @WrapOperation}、
 * Showcase Item 用 {@code @Redirect} 占住同一条指令）。{@code @Redirect} 之间只能活一个，
 * 抢不到的那个会被静默跳过（日志里只有一句 "Redirect conflict. Skipping …"）——
 * 表现就是"自己单独跑没问题、进了整合包图标就没了"。
 *
 * <p>{@code @Inject} 没有这个问题：它挂在被调用方，和任何调用点上的 {@code @Redirect} /
 * {@code @WrapOperation} 都不冲突，可以共存。
 */
@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Inject(
            method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I",
            at = @At("HEAD")
    )
    private void wiwsu$drawItemIcons(Font font, FormattedCharSequence sequence, int x, int y, int color,
                                     CallbackInfoReturnable<Integer> cir) {
        ChatItemIcons.renderItemForLine((GuiGraphics) (Object) this, sequence, x, y, color);
    }
}
