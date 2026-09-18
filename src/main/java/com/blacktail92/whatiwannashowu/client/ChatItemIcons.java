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
 * <p>做法照搬 Quark 的 ItemSharingModule：消息文本里放几个空格当占位，物品本体放在
 * 这段空格的 {@link HoverEvent.ItemStackInfo} 样式里；渲染时扫这一行的
 * {@link FormattedCharSequence}，撞到"两个连续空格"就把它们前面的文字宽度算出来，在那个位置
 * 用 {@link GuiGraphics#renderItem} 画<b>真正的物品模型</b>。
 *
 * <p>这样做的最大好处：走的是原版物品渲染管线，所以附魔光效、染色、多图层、3D 模型、BEWLR
 * 全都是对的 —— 而不是一张扁平贴图。
 */
public final class ChatItemIcons {
    /**
     * 图标占位：1 个空格（左侧间隙）+ 2 个空格（图标本体 8px）+ 1 个空格（右侧间隙）。
     *
     * <p>左侧间隙写进占位里，而不是靠消息前缀里那个空格：整行第一段连续空格就是从占位这里
     * 开始的，图标画在空格段起点右移 {@link #ICON_OFFSET_SPACES} 个空格处，于是物品图标和
     * 玩家名、物品名之间各留一个空格（4px）。这样写死以后，前缀带不带空格都不会把右侧间隙挤没。
     */
    public static final String PLACEHOLDER = "    ";

    /**
     * 图标相对"连续空格段起点"的偏移（单位：空格）。1 个空格 = 左侧间隙 4px。
     */
    private static final int ICON_OFFSET_SPACES = 1;

    private ChatItemIcons() {
    }

    /**
     * 由 GuiGraphicsMixin 在每次画一行文字前调用。
     *
     * <p>这里<b>不判断</b> {@code renderItemsInChat} 开关：那个开关只决定"新分享的消息带不带
     * 图标占位"，已经在聊天栏里的消息（占位还在文字里）应该继续显示图标，否则一关开关老消息的
     * 图标就全没了。
     */
    public static void renderItemForLine(GuiGraphics guiGraphics, FormattedCharSequence sequence,
                                         float x, float y, int color) {
        var mc = Minecraft.getInstance();
        var before = new StringBuilder();

        sequence.accept((index, style, character) -> {
            int length = before.length();
            // before 已经积累到"两个连续空格"了 —— 再往前的两个空格就是图标占位。
            // 这里只看最后两个字符，别每个字符都 toString()：这个函数每次画文字都会被调用
            if (length >= 2 && before.charAt(length - 1) == ' ' && before.charAt(length - 2) == ' ') {
                render(mc, guiGraphics, before.substring(0, length - 2), x, y, style, color);
                return false; // 一行只画一个图标
            }
            before.append((char) character);
            return true;
        });
    }

    private static void render(Minecraft mc, GuiGraphics guiGraphics, String before,
                               float x, float y, Style style, int color) {
        float alpha = (color >> 24 & 255) / 255.0F;
        if (alpha <= 0.0F) return;

        var hoverEvent = style.getHoverEvent();
        if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_ITEM) return;

        HoverEvent.ItemStackInfo contents = hoverEvent.getValue(HoverEvent.Action.SHOW_ITEM);
        var stack = contents != null ? contents.getItemStack() : ItemStack.EMPTY;
        if (stack.isEmpty()) stack = new ItemStack(Blocks.BARRIER); // 物品丢了就画个屏障，别留个空洞

        // 空格段起点 + 左侧间隙：图标落在左侧 1 个空格之后，右侧自然还剩 1 个空格
        float shift = mc.font.width(before) + ICON_OFFSET_SPACES * mc.font.width(" ");

        var pose = guiGraphics.pose();
        pose.pushPose();
        // 只做"平移到占位处 + 缩放到 8px"：当前 pose 就是这一行文字用的那套变换，
        // 所以不能再往里套一层 —— 早期照抄 Quark 的 mulPoseMatrix(pose.last().pose()) 会把聊天栏
        // 的变换平方掉，在整合包里（聊天栏被别的 mod 用更复杂的变换渲染时）图标会被推到可见范围外，
        // 表现成"代码执行了、屏幕上看不见"。
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
