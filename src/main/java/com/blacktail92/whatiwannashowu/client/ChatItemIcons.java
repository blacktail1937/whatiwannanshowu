package com.blacktail92.whatiwannashowu.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * 在聊天栏里画物品图标。
 *
 * <p>做法照搬 Quark 的 ItemSharingModule：消息文本里放 <b>3 个空格</b>当占位，物品本体放在
 * 这 3 个空格的 {@link HoverEvent.ItemStackInfo} 样式里；渲染时扫这一行的
 * {@link FormattedCharSequence}，撞到"两个连续空格"就把它们前面的文字宽度算出来，在那个位置
 * 用 {@link GuiGraphics#renderItem} 画<b>真正的物品模型</b>。
 *
 * <p>这样做的最大好处：走的是原版物品渲染管线，所以附魔光效、染色、多图层、3D 模型、BEWLR
 * 全都是对的 —— 而不是一张扁平贴图。
 */
public final class ChatItemIcons {
    /**
     * 图标占位：前 2 个空格（8px）放图标，剩下的是图标和名字之间的间隙。
     */
    public static final String PLACEHOLDER = "    ";

    private ChatItemIcons() {
    }

    /**
     * 由 ChatComponentMixin 在每行文字绘制前调用。
     */
    public static void renderItemForLine(GuiGraphics guiGraphics, FormattedCharSequence sequence,
                                         float x, float y, int color) {
        var mc = Minecraft.getInstance();
        var before = new StringBuilder();
        int halfSpace = mc.font.width(" ") / 2;

        sequence.accept((index, style, character) -> {
            var sofar = before.toString();
            // before 已经积累到"两个连续空格"了 —— 再往前的两个空格就是图标占位
            if (sofar.endsWith("  ")) {
                render(mc, guiGraphics, sofar.substring(0, sofar.length() - 2),
                        character == ' ' ? 0 : -halfSpace, x, y, style, color);
                return false; // 一行只画一个图标
            }
            before.append((char) character);
            return true;
        });
    }

    private static void render(Minecraft mc, GuiGraphics guiGraphics, String before, float extraShift,
                               float x, float y, Style style, int color) {
        float alpha = (color >> 24 & 255) / 255.0F;
        if (alpha <= 0.0F) return;

        var hoverEvent = style.getHoverEvent();
        if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_ITEM) return;

        HoverEvent.ItemStackInfo contents = hoverEvent.getValue(HoverEvent.Action.SHOW_ITEM);
        var stack = contents != null ? contents.getItemStack() : ItemStack.EMPTY;
        if (stack.isEmpty()) stack = new ItemStack(Blocks.BARRIER); // 物品丢了就画个屏障，别留个空洞

        // 占位空格比图标宽，所以要按字体宽度算一下偏移
        float shift = mc.font.width(before) + extraShift;

        var pose = guiGraphics.pose();
        pose.pushPose();
        // 套用聊天栏当前的变换（这一段是和 Quark 一样的写法，不要自己"优化"）
        pose.mulPoseMatrix(pose.last().pose());
        pose.translate(shift + x, y, 0.0F);
        pose.scale(0.5F, 0.5F, 0.5F); // 16px 的模型 → 8px，正好是聊天栏文字的高度
        try {
            // 淡出用原版的 shader 颜色：setColor 就是 RenderSystem.setShaderColor，
            // ColorModulator 会乘到物品（含附魔光效）的顶点颜色上，
            // 这样就不用去 mixin ItemRenderer 里那个 Forge 加的重载
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            guiGraphics.renderItem(stack, 0, 0);
        } finally {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }
}
