package com.blacktail92.whatiwannashowu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 三层兜底里的第 ② 层：记录"界面上画了哪些物品、画在什么位置"。
 *
 * <p><b>为什么需要这一层：</b>第 ① 层 {@code AbstractContainerScreen#getSlotUnderMouse()} 只认
 * "坐标和视觉一致的原版槽位"。但不少 mod 的界面把物品**自己画**出来（RS 的网格、Chipped 的槽位
 * 控件、Sawmill 中间的配方列表……），它们的 {@code slot.x}/{@code slot.y} 与视觉位置脱钩，
 * 原版的 {@code hoveredSlot} 永远是 null，于是拿不到物品。
 *
 * <p>这些界面虽然"自绘"，但**仍然走 Minecraft 官方的绘制 API**（{@code GuiGraphics#renderItem}），
 * 所以只要在绘制发生的那一刻把 (物品, 位置) 记下来，就能反推"鼠标下是哪个物品"。
 * 不认任何具体 mod，也不用对方配合。
 *
 * <p><b>为什么不按帧清空：</b>界面渲染是嵌套的（{@code AbstractContainerScreen#render} 先调
 * {@code super.render}），按"进入/离开 Screen#render"划范围会漏掉派生类在 super 之后画的物品。
 * 这里改用"屏幕实例变化就清空 + 容量上限"，更简单也不会漏。
 */
public final class GuiItemPicker {
    /** 单屏记录上限，纯粹防止长时间停留导致列表无限增长。 */
    private static final int MAX_ENTRIES = 4096;

    private static final List<Entry> FRAME = new ArrayList<>();

    /** 记录所属的界面实例；换界面就清空，避免上一个界面的坐标残留。 */
    private static Screen lastScreen;

    /**
     * @param stack 画出来的物品
     * @param minX  在 GUI 空间里占的矩形
     */
    private record Entry(ItemStack stack, float minX, float minY, float maxX, float maxY) {
    }

    private GuiItemPicker() {
    }

    /**
     * 由 mixin 在每次绘制物品时调用，{@code x}/{@code y} 是该物品 16×16 方格在
     * <b>当前 pose（也就是界面自己那套变换）</b>下的左上角。
     */
    public static void record(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;

        var mc = Minecraft.getInstance();
        var screen = mc.screen;
        if (screen == null) return; // 只在界面里记录，HUD 里的物品不参与

        if (screen != lastScreen) {
            FRAME.clear();
            lastScreen = screen;
        }
        if (FRAME.size() >= MAX_ENTRIES) FRAME.clear();

        // 把 16×16 的两个角映射到 GUI 空间，和鼠标坐标同一套坐标系
        var pose = graphics.pose().last().pose();
        var p0 = pose.transformPosition(new Vector3f(x, y, 0.0F), new Vector3f());
        var p1 = pose.transformPosition(new Vector3f(x + 16, y + 16, 0.0F), new Vector3f());

        FRAME.add(new Entry(stack.copy(),
                Math.min(p0.x, p1.x), Math.min(p0.y, p1.y),
                Math.max(p0.x, p1.x), Math.max(p0.y, p1.y)));
    }

    /**
     * 取鼠标位置命中的物品；后画的优先（界面上层覆盖下层）。
     */
    public static ItemStack pickUnderMouse(double mouseX, double mouseY) {
        for (int i = FRAME.size() - 1; i >= 0; i--) {
            var entry = FRAME.get(i);
            if (mouseX >= entry.minX() && mouseX <= entry.maxX()
                    && mouseY >= entry.minY() && mouseY <= entry.maxY()) {
                return entry.stack();
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 鼠标在 GUI 空间里的坐标（和 {@code GuiGraphics} 用的是同一套缩放）。
     */
    public static double[] mouseInGuiSpace(Minecraft mc) {
        var window = mc.getWindow();
        double x = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / (double) window.getScreenWidth();
        double y = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / (double) window.getScreenHeight();
        return new double[]{x, y};
    }
}
