package com.blacktail92.whatiwannashowu.integration;

import com.mojang.logging.LogUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {
    private static IJeiRuntime JEI_RUNTIME;
    static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("whatiwannashowu", "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        LOGGER.info("get jei runtime");
        JEI_RUNTIME = runtime;
    }

    public static void showRecipe(ItemStack stack) {
        if (JEI_RUNTIME != null && !stack.isEmpty()) {
            var recipeGui = JEI_RUNTIME.getRecipesGui();
            recipeGui.show(JEI_RUNTIME.getJeiHelpers().getFocusFactory().createFocus(
                    RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack
            ));
        }
    }
}
