package com.blacktail92.whatiwannashowu.integration;

import com.mojang.logging.LogUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Optional;

@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {
    static final Logger LOGGER = LogUtils.getLogger();
    private static IJeiRuntime JEI_RUNTIME;

    public static void showRecipe(ItemStack stack) {
        if (JEI_RUNTIME != null && !stack.isEmpty()) {
            var recipeGui = JEI_RUNTIME.getRecipesGui();
            recipeGui.show(JEI_RUNTIME.getJeiHelpers().getFocusFactory().createFocus(
                    RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack
            ));
        }
    }

    public static ItemStack getStackUnderMouse() {
        if (JEI_RUNTIME != null) {
            return JEI_RUNTIME.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK)
                    .or(() -> Optional.ofNullable(JEI_RUNTIME.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK)))
                    .orElse(ItemStack.EMPTY);
        }

        return ItemStack.EMPTY;
    }

    public static boolean hasKeyboardFocus() {
        return JEI_RUNTIME != null && JEI_RUNTIME.getIngredientListOverlay().hasKeyboardFocus();
    }

    /**
     * JEI："这个界面上鼠标下是什么物品？"
     *
     * <p>{@code IScreenHelper#getClickableIngredientUnderMouse} 会把各 mod 注册的
     * {@code IGuiContainerHandler} / {@code IGlobalGuiHandler} 都过一遍 —— 只要那个界面
     * （例如精炼存储的网格）自己告诉过 JEI"我这里鼠标下是什么"，我们就能拿到，
     * 不需要为它单独适配。
     *
     * @return 鼠标下的物品；拿不到返回空栈
     */
    public static ItemStack getScreenIngredientUnderMouse(Screen screen, double mouseX, double mouseY) {
        if (JEI_RUNTIME == null || screen == null) return ItemStack.EMPTY;

        try {
            var iterator = JEI_RUNTIME.getScreenHelper()
                    .getClickableIngredientUnderMouse(screen, mouseX, mouseY)
                    .iterator();
            while (iterator.hasNext()) {
                var stack = iterator.next().getTypedIngredient().getItemStack().orElse(ItemStack.EMPTY);
                if (!stack.isEmpty()) return stack;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to query the ingredient under mouse from JEI", e);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("whatiwannashowu", "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        LOGGER.info("get jei runtime");
        JEI_RUNTIME = runtime;
    }
}
