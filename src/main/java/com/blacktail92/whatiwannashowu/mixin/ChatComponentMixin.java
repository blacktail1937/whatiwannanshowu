package com.blacktail92.whatiwannashowu.mixin;

import com.blacktail92.whatiwannashowu.client.ChatItemIcons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 在聊天栏每一行画文字之前，先把该行的物品图标画出来。
 *
 * <p>聊天栏本身没有"逐行"的事件，但 {@code ChatComponent#render} 每画一行都会调用一次
 * {@code GuiGraphics#drawString(Font, FormattedCharSequence, int, int, int)}，所以拦这一次
 * 调用就拿到了这一行内容、位置和淡出用的 alpha。做法与 Quark 的 ItemSharingModule 相同。
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"
            )
    )
    private int wiwsu$drawItemIcons(GuiGraphics guiGraphics, Font font, FormattedCharSequence sequence,
                                    int x, int y, int color) {
        ChatItemIcons.renderItemForLine(guiGraphics, sequence, x, y, color);
        return guiGraphics.drawString(font, sequence, x, y, color);
    }
}
