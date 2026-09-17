package com.blacktail92.whatiwannashowu.client;

import com.blacktail92.whatiwannashowu.Config;
import com.blacktail92.whatiwannashowu.WhatIwannashowU;
import com.blacktail92.whatiwannashowu.integration.JeiPlugin;
import com.blacktail92.whatiwannashowu.networking.ShareItemPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Optional;


@EventBusSubscriber(modid = WhatIwannashowU.MODID, value = Dist.CLIENT)
public class ClientEvents {
    static final Logger LOGGER = LogUtils.getLogger();
    static KeyMapping SHARED_KEY;
    static KeyMapping LOOKED_AT_KEY;
    static long LAST_SHARE_TIME;

    @SubscribeEvent
    static void onKeyRegister(RegisterKeyMappingsEvent event) {
        SHARED_KEY = new KeyMapping("key.whatiwannashowu.share_item", InputConstants.KEY_G, "key.categories.whatiwannashowu");
        LOOKED_AT_KEY = new KeyMapping("key.whatiwannashowu.share_looked_at", InputConstants.KEY_V, "key.categories.whatiwannashowu");

        event.register(SHARED_KEY);
        event.register(LOOKED_AT_KEY);
    }

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
    static void onClientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var inWorld = mc.screen == null && mc.player != null;


        while (SHARED_KEY.consumeClick()) {
            if (inWorld) {
                var stack = mc.player.getMainHandItem();
                if (!stack.isEmpty()) sendSharePacket(mc, stack);
            }
        }

        while (LOOKED_AT_KEY.consumeClick()) {
            if (inWorld) {
                var stack = stackUnderCrosshair(mc);
                if (!stack.isEmpty()) sendSharePacket(mc, stack);
            }
        }
    }

    static void sendSharePacket(Minecraft mc, ItemStack stack) {
        if (mc.player == null) return;

        var now = System.currentTimeMillis();
        int cooldown = Config.SHARE_COOLDOWN.get();
        if (cooldown > 0 && now - LAST_SHARE_TIME < cooldown) {
            var remaining = (int) Math.ceil((LAST_SHARE_TIME + cooldown - now) / 1000.0);
            mc.player.displayClientMessage(
                    Component.translatable("message.whatiwannashowu.share_cooldown", remaining), true);
            return;
        }
        LAST_SHARE_TIME = now;

        if (mc.getConnection() != null) {
            try {
                //compress
                var nbt = (CompoundTag) stack.save(mc.player.registryAccess());
                mc.getConnection().send(
                        new ShareItemPayload(mc.player.getUUID(), ItemCache.compress(nbt))
                );
            } catch (IOException e) {
                LOGGER.error("Error while sending share packet", e);
            }
        }
    }

    public static Component createItemLink(ItemStack stack) {
        var itemName = stack.getHoverName();

        var itemHover = new HoverEvent(
                HoverEvent.Action.SHOW_ITEM,
                new HoverEvent.ItemStackInfo(stack)
        );

        // 图标占位：物品本体藏在这段空格的 hover 样式里，渲染时由 ChatItemIcons 从文字的样式里读回来
        //（和 Quark 一样的做法）。占位必须是整行第一段连续空格，所以发送者前缀不要再带空格。
        // 关掉 renderItemsInChat 时就完全不加占位，消息退化成普通的 "[物品名]" 链接，
        // 但玩家名和物品名之间仍然要留一个空格。
        var link = Component.empty();
        if (Config.RENDER_ITEMS_IN_CHAT.get()) {
            link.append(Component.literal(ChatItemIcons.PLACEHOLDER)
                    .withStyle(style -> style.withHoverEvent(itemHover)));
        } else {
            link.append(Component.literal(" "));
        }

        return link
                .append(Component.literal("[")
                        .append(itemName)
                        .append("]")
                        .withStyle(style -> {
                            var updateStyle = stack.getRarity().getStyleModifier().apply(style);

                            if (Config.IS_JEI_LOADED) {
                                var mc = Minecraft.getInstance();
                                if (mc.player != null) {
                                    var hash = ItemCache.put(stack, mc.player.registryAccess());
                                    var cmd = "/wiwsu_lookup " + hash;
                                    updateStyle = updateStyle.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                                }
                            }

                            return updateStyle
                                    .withUnderlined(true)
                                    .withHoverEvent(itemHover);
                        }));
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        // 这里是客户端命令：装了 JEI 才需要它（用来打开配方），原来写成了 if (Config.IS_JEI_LOADED) return，
        // 结果恰好反了 —— 装了 JEI 的常见情况下命令没注册，点链接会提示未知命令
        if (!Config.IS_JEI_LOADED) return;

        var key = "hash";
        event.getDispatcher().register(
                Commands.literal("wiwsu_lookup")
                        .then(Commands.argument(key, StringArgumentType.word())
                                .suggests(((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        BuiltInRegistries.ITEM.keySet(), builder
                                )))
                                .executes(context -> {
//                                        var itemId = ResourceLocationArgument.getId(context, key);
//                                        var item = BuiltInRegistries.ITEM.get(itemId);
                                    var hash = StringArgumentType.getString(context, key);
                                    var mc = Minecraft.getInstance();
                                    Optional.ofNullable(mc.player)
                                            .ifPresent(player ->
                                                    mc.execute(() -> JeiPlugin.showRecipe(ItemCache.get(hash, mc.player.registryAccess()))));

                                    return 1;
                                }))
        );
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
    private static ItemStack stackUnderCrosshair(Minecraft mc) {
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null) return ItemStack.EMPTY;

        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
        var eye = player.getEyePosition(partialTick);
        var look = player.getViewVector(partialTick);
        // 1.21 里"方块交互距离"是原版属性（默认 4.5），和 WAILA/Jade 的提示范围一致
        double reach = player.blockInteractionRange();

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
            // 严格按原版箱子打必须瞄模型下围。这里照 Jade 的做法给小箱子加宽容。
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
     * （Jade 的做法）。只在原版 API 什么都没打中时才作为兜底调用。
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

    /** 实体对应的"物品形态"；拿不到就返回空栈。 */
    private static ItemStack itemOf(Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            return itemEntity.getItem().copy();   // 地上的掉落物：就是它本身
        }
        // 展示框 → 框里的物品；船 / 矿车 / 盔甲架 → 本体；生物 → 刷怪蛋；其余 → null
        var pickResult = entity.getPickResult();
        return pickResult == null ? ItemStack.EMPTY : pickResult;
    }
}
