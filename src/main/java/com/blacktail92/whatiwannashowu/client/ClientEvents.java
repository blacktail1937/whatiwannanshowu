package com.blacktail92.whatiwannashowu.client;

import com.blacktail92.whatiwannashowu.Config;
import com.blacktail92.whatiwannashowu.WhatIWannaShowU;
import com.blacktail92.whatiwannashowu.integration.JeiPlugin;
import com.blacktail92.whatiwannashowu.networking.ModMessages;
import com.blacktail92.whatiwannashowu.networking.ShareItemPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = WhatIWannaShowU.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    /// hotkey for sharing the item in hand or under the cursor
    public static KeyMapping SHARED_KEY;
    /// hotkey for sharing the item  under the crosshair
    public static KeyMapping SHARED_KEY_VIA_AIM_POINT;
    private static long LAST_SHARE_TIME;

    @SubscribeEvent
    static void onScreenKey(ScreenEvent.KeyReleased.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.screen == null) return;

        if (SHARED_KEY.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) {
            var stack = Optional.of(mc.screen)
                    .filter(AbstractContainerScreen.class::isInstance)
                    .map(AbstractContainerScreen.class::cast)
                    .map(AbstractContainerScreen::getSlotUnderMouse)
                    .map(Slot::getItem)
                    .or(() -> Optional.ofNullable(Config.IS_JEI_LOADED ? JeiPlugin.getStackUnderMouse() : null))
                    .filter(s -> !s.isEmpty())
                    .orElse(ItemStack.EMPTY);

            if (!stack.isEmpty()) {
                sendSharePacket(mc, stack);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var mc = Minecraft.getInstance();
        var inWorld = mc.screen == null && mc.player != null;

        while (SHARED_KEY.consumeClick()) {
            if (inWorld) {
                var stack = mc.player.getMainHandItem();
                if (!stack.isEmpty())
                    sendSharePacket(mc, stack);
            }
        }

        while (SHARED_KEY_VIA_AIM_POINT.consumeClick()) {
            if (inWorld) {
                var stack = stackUnderCrosshair();
                if (!stack.isEmpty())
                    sendSharePacket(mc, stack);
            }
        }
    }

    @SubscribeEvent
    static void onCliengLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ItemCache.clear();
    }

    static void sendSharePacket(Minecraft mc, ItemStack stack) {
        if (mc.player == null) return;

        var now = System.currentTimeMillis();
        var cooldown = Config.SHARE_COOLDOWN.get();
        if (cooldown > 0 && now - LAST_SHARE_TIME < cooldown) {
            var remaining = (int) Math.ceil((LAST_SHARE_TIME + cooldown - now) / 1000.0);
            mc.player.displayClientMessage(
                    Component.translatable("message.whatiwannashowu.share_cooldown", remaining), true);
            return;
        }
        LAST_SHARE_TIME = now;

        if (mc.getConnection() != null) {
            try {
                var nbt = stack.serializeNBT();
                ModMessages.sendToServer(new ShareItemPayload(mc.player.getUUID(), ItemCache.compress(nbt)));
            } catch (IOException e) {
                LOGGER.error("Failed to send share packet", e);
            }
        }
    }

    public static Component createItemLink(ItemStack stack) {
        var itemName = stack.getHoverName();

        var itemHover = new HoverEvent(
                HoverEvent.Action.SHOW_ITEM,
                new HoverEvent.ItemStackInfo(stack)
        );

        // 3 个空格是图标的占位：物品本体就藏在这段空格的 hover 样式里，
        // 渲染时由 ChatItemIcons 从文字样式里读回来（和 Quark 一样的做法）。
        // 注意占位必须是"整行里第一段连续空格"，所以调用方的发送者前缀不要再带空格。
        var placeholder = Component.literal(ChatItemIcons.PLACEHOLDER)
                .withStyle(style -> style.withHoverEvent(itemHover));

        return Component.empty()
                .append(placeholder)
                .append(Component.literal("[")
                        .append(itemName)
                        .append("]")
                        .withStyle(style -> {
                            var updateStyle = stack.getRarity().getStyleModifier().apply(style);

                            if (Config.IS_JEI_LOADED) {
                                var hash = ItemCache.put(stack);
                                if (!hash.isEmpty()) {
                                    var cmd = "/wiwsu_lookup " + hash;
                                    // LOGGER.info("点击指令是 {}", cmd);
                                    // var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
                                    updateStyle = updateStyle.withClickEvent(
                                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd)
                                    );
                                }
                            }

                            return updateStyle
                                    .withUnderlined(true)
                                    .withHoverEvent(itemHover);
                        }));
    }

    /**
     * 准星下的物品：<b>谁离准星近就取谁</b>（方块或实体），射线全部用原版 API。
     *
     * <p>方块用 {@link Player#pick(double, float, boolean)}（和准星同一次口径）；
     * 实体用 {@link ProjectileUtil#getEntityHitResult}——谓词由调用方传，所以掉落物这种
     * "原版准星选不中但你看得见"的实体也能拿到（原版准星自己传的是 {@code isPickable()}，
     * 把 {@code ItemEntity} 排除掉了）。
     *
     * <p>把方块命中距离当作实体射线的 maxDistance，墙后的实体就自动被排除；于是
     * "有实体命中 ⇒ 它一定比方块近"，直接用它就是"谁近取谁"。
     */
    private static ItemStack stackUnderCrosshair() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null) return ItemStack.EMPTY;

        float partialTick = mc.getFrameTime();
        var eye = player.getEyePosition(partialTick);
        var look = player.getViewVector(partialTick);
        // 用方块 reach（和 WAILA/Jade 一致），而不是原版的实体 reach(3.0)，
        // 否则会出现"WAILA 有 tips、按 V 却没反应"
        double reach = mc.gameMode == null ? 0.0D : mc.gameMode.getPickRange();

        var blockHit = player.pick(reach, partialTick, false);
        boolean hasBlock = blockHit.getType() == HitResult.Type.BLOCK;
        double blockDistSq = hasBlock ? eye.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;

        var end = eye.add(look.scale(reach));
        var box = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        // 注意：ProjectileUtil 最后一个参数是"平方距离"，别直接把 reach 传进去
        var entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> canTarget(player, e), Math.min(reach * reach, blockDistSq));

        if (entityHit == null) {
            // 兜底：掉落物的碰撞箱只有 0.25（EntityType.ITEM 注册成 sized(0.25F, 0.25F)），
            // 而它的模型渲染时又被抬高了 0.25~0.45 格（ItemEntityRenderer），
            // 只strictly按原版箱子打，必须瞄模型下围才选中。这里照 Jade 的做法给小箱子加宽容。
            entityHit = generousItemHit(player, eye, end, box, Math.min(reach * reach, blockDistSq));
        }

        if (entityHit != null) {
            var stack = itemOf(entityHit.getEntity());
            if (!stack.isEmpty()) return stack;
            // 最近的是实体但它没有物品形态（比如插着的箭）→ 落回方块
        }

        if (hasBlock && blockHit instanceof BlockHitResult result) {
            var pos = result.getBlockPos();
            // getCloneItemStack = 原版中键选取方块的行为：作物给作物、花盆带着花、
            // 潜影盒带着内容物，而不是一律给个空壳
            return level.getBlockState(pos).getCloneItemStack(result, level, pos, player).copy();
        }

        return ItemStack.EMPTY;
    }

    /** 排除自己、自己骑的东西、旁观者、已移除和隐形的实体（口径和 Jade 基本一致）。 */
    private static boolean canTarget(Player player, Entity entity) {
        return entity != player
                && !entity.isRemoved()
                && !entity.isSpectator()
                && entity != player.getVehicle()
                && !entity.isInvisibleTo(player);
    }

    /**
     * 只针对掉落物的"宽容"检测：原版箱子太小且和模型错位，把 0.25 的箱子撑到 0.3 下限再求交
     * （Jade 的做法，见其 RayTracing）。只在原版 API 什么都没打中时才作为兜底调用，
     * 所以不会影响方块、生物、展示框这些手感。
     */
    private static EntityHitResult generousItemHit(Player player, Vec3 eye, Vec3 end, AABB searchBox,
                                                   double maxDistSq) {
        EntityHitResult best = null;
        double bestDist = maxDistSq;

        for (var entity : player.level().getEntities(player, searchBox, e -> e instanceof ItemEntity)) {
            var box = entity.getBoundingBox();
            if (box.getSize() < 0.3D) box = box.inflate(0.3D);

            var clip = box.clip(eye, end);
            if (clip.isEmpty()) continue;

            double dist = eye.distanceToSqr(clip.get());
            if (dist < bestDist) {
                bestDist = dist;
                best = new EntityHitResult(entity, clip.get());
            }
        }

        return best;
    }

    /** 实体对应的"物品形态"；拿不到就返回空栈（调用方会继续看下一个实体）。 */
    private static ItemStack itemOf(Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            return itemEntity.getItem().copy();   // 地上的掉落物：就是它本身
        }
        // 展示框 → 框里的物品；船 / 矿车 / 盔甲架 → 本体；生物 → 刷怪蛋；其余 → null
        var pickResult = entity.getPickResult();
        return pickResult == null ? ItemStack.EMPTY : pickResult;
    }
}
