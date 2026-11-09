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
 * <p>第 ① 层 {@code AbstractContainerScreen#getSlotUnderMouse()} 只认"坐标和视觉一致的原版槽位"。
 * 但不少 mod 的界面把物品自己画出来（RS 网格、Chipped 的槽位、Sawmill 中间的配方列表……），
 * 它们的 slot 坐标与视觉脱钩，原版 hoveredSlot 永远是 null，于是拿不到物品。
 *
 * <p>这些界面虽然"自绘"，但仍然走 {@code GuiGraphics#renderItem}，所以只要在绘制的那一刻
 * 把 (物品, 位置) 记下来，就能反推"鼠标下是哪个物品"。不认任何具体 mod。
 *
 * <p>不按帧清空的原因：界面渲染是嵌套的（{@code AbstractContainerScreen#render} 先调 super.render），
 * 按"进入/离开 Screen#render"划范围会漏掉派生类在 super 之后画的物品。改用"屏幕实例变化就
 * 清空 + 容量上限"。
 */
public final class GuiItemPicker {
    private static final int MAX_ENTRIES = 4096;

    private static final List<Entry> FRAME = new ArrayList<>();

    private static Screen lastScreen;

    private record Entry(ItemStack stack, float minX, float minY, float maxX, float maxY) {
    }

    private GuiItemPicker() {
    }

    /** 由 mixin 在每次绘制物品时调用；(x, y) 是该物品 16×16 方格在当前 pose 下的左上角。 */
    public static void record(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;

        var mc = Minecraft.getInstance();
        var screen = mc.screen;
        if (screen == null) return;

        if (screen != lastScreen) {
            FRAME.clear();
            lastScreen = screen;
        }
        if (FRAME.size() >= MAX_ENTRIES) FRAME.clear();

        var pose = graphics.pose().last().pose();
        var p0 = pose.transformPosition(new Vector3f(x, y, 0.0F), new Vector3f());
        var p1 = pose.transformPosition(new Vector3f(x + 16, y + 16, 0.0F), new Vector3f());

        FRAME.add(new Entry(stack.copy(),
                Math.min(p0.x, p1.x), Math.min(p0.y, p1.y),
                Math.max(p0.x, p1.x), Math.max(p0.y, p1.y)));
    }

    /** 取鼠标位置命中的物品；后画的优先（界面上层覆盖下层）。 */
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

    /** 鼠标在 GUI 空间里的坐标（和 GuiGraphics 同一套缩放）。 */
    public static double[] mouseInGuiSpace(Minecraft mc) {
        var window = mc.getWindow();
        double x = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / (double) window.getScreenWidth();
        double y = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / (double) window.getScreenHeight();
        return new double[]{x, y};
    }
}
