package com.blacktail92.whatiwannashowu.client;

import com.blacktail92.whatiwannashowu.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * 本 mod 的配置界面 —— Forge 1.20.1 的 mod 列表里那个 "Config" 按钮打开的就是它。
 *
 * <p><b>为什么需要自己写：</b>Forge 1.20.1 没有内置的配置界面（NeoForge 1.21.1 上那个自动生成的
 * 界面是 NeoForge 自带的），Forge 只给了 {@code ConfigScreenHandler.ConfigScreenFactory} 这个入口 ——
 * mod 不注册，mod 列表里的 Config 按钮就是灰的、点不动。
 *
 * <p>三个选项都用原版控件搭出来，点"完成"时 {@code ConfigValue#set} + {@link Config#save()}
 * 写回 toml。因为渲染端和分享逻辑都是每次用到时才读配置，所以改完<b>立即生效、不需要重启</b>。
 */
public class ConfigScreen extends Screen {
    private static final Component LABEL_RENDER = Component.translatable("whatiwannashowu.config.renderItemsInChat");
    private static final Component LABEL_COOLDOWN = Component.translatable("whatiwannashowu.config.shareCooldown");
    private static final Component LABEL_MAX_SIZE = Component.translatable("whatiwannashowu.config.maxCompressedSize");
    private static final Component HINT_COOLDOWN = Component.translatable("whatiwannashowu.config.shareCooldown.hint");
    private static final Component HINT_MAX_SIZE = Component.translatable("whatiwannashowu.config.maxCompressedSize.hint");
    private static final Component RESET = Component.translatable("whatiwannashowu.config.reset");

    private static final int BOX_WIDTH = 200;
    private static final int BOX_HEIGHT = 20;

    private final Screen parent;

    private CycleButton<Boolean> renderItemsInChat;
    private EditBox shareCooldown;
    private EditBox maxCompressedSize;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("whatiwannashowu.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - BOX_WIDTH / 2;
        int y = this.height / 4;

        // 开 / 关
        this.renderItemsInChat = this.addRenderableWidget(
                CycleButton.onOffBuilder(Config.RENDER_ITEMS_IN_CHAT.get())
                        .create(left, y, BOX_WIDTH, BOX_HEIGHT, LABEL_RENDER));

        // 分享冷却（毫秒）
        y += 40;
        this.shareCooldown = this.addNumericBox(left, y, Config.SHARE_COOLDOWN.get(), HINT_COOLDOWN);

        // 分享数据体积上限（KB）
        y += 40;
        this.maxCompressedSize = this.addNumericBox(left, y, Config.MAX_COMPRESSED_SIZE.get(), HINT_MAX_SIZE);

        // 完成 / 取消 / 恢复默认
        y += 40;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.saveAndClose())
                .bounds(left, y, BOX_WIDTH / 2 - 2, BOX_HEIGHT).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                .bounds(left + BOX_WIDTH / 2 + 2, y, BOX_WIDTH / 2 - 2, BOX_HEIGHT).build());
        this.addRenderableWidget(Button.builder(RESET, button -> this.resetToDefaults())
                .bounds(left, y + BOX_HEIGHT + 6, BOX_WIDTH, BOX_HEIGHT).build());
    }

    /** 数字输入框：只允许数字，超范围时按范围夹取（见 {@link #readValue}）。 */
    private EditBox addNumericBox(int x, int y, int value, Component hint) {
        var box = new EditBox(this.font, x, y, BOX_WIDTH, BOX_HEIGHT, hint);
        box.setValue(Integer.toString(value));
        box.setHint(hint);
        box.setFilter(text -> text.isEmpty() || text.matches("\\d{1,6}"));
        return this.addRenderableWidget(box);
    }

    private void resetToDefaults() {
        this.renderItemsInChat.setValue(Config.RENDER_ITEMS_IN_CHAT.getDefault());
        this.shareCooldown.setValue(Integer.toString(Config.SHARE_COOLDOWN.getDefault()));
        this.maxCompressedSize.setValue(Integer.toString(Config.MAX_COMPRESSED_SIZE.getDefault()));
    }

    private void saveAndClose() {
        Config.RENDER_ITEMS_IN_CHAT.set(this.renderItemsInChat.getValue());
        Config.SHARE_COOLDOWN.set(readValue(this.shareCooldown, Config.SHARE_COOLDOWN.get(), 0, 60000));
        Config.MAX_COMPRESSED_SIZE.set(readValue(this.maxCompressedSize, Config.MAX_COMPRESSED_SIZE.get(), 1, 512));
        Config.save();
        this.onClose();
    }

    /** 解析输入框里的数字；空着或者写了非法内容就沿用原值，超出范围就夹到边界。 */
    private static int readValue(EditBox box, int fallback, int min, int max) {
        try {
            return Math.min(max, Math.max(min, Integer.parseInt(box.getValue().trim())));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 24, 0xFFFFFF);

        // EditBox 自己不带标签，这里补上
        graphics.drawString(this.font, LABEL_COOLDOWN, this.shareCooldown.getX(), this.shareCooldown.getY() - 11, 0xA0A0A0);
        graphics.drawString(this.font, LABEL_MAX_SIZE, this.maxCompressedSize.getX(), this.maxCompressedSize.getY() - 11, 0xA0A0A0);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }
}
