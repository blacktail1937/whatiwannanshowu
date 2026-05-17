package com.blacktail92.whatiwannashowu.integration;

import com.mojang.logging.LogUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
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
