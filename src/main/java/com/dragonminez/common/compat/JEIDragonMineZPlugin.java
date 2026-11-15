package com.dragonminez.common.compat;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.client.init.menu.screens.KikonoStationScreen;
import com.dragonminez.server.recipes.KikonoRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class JEIDragonMineZPlugin implements IModPlugin {

	@Override
	public Identifier getPluginUid() {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new KikonoStationCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		// 26.2: RecipeAccess only exposes property sets; the full recipe list lives on RecipeManager.

		List<KikonoRecipe> recipes = new ArrayList<>();

		var level = Minecraft.getInstance().level;
		if (level == null || level.getServer() == null) return;
		for (var holder : level.getServer().getRecipeManager().getRecipes()) {
			Recipe<?> recipe = holder.value();
			if (recipe instanceof KikonoRecipe kikonoRecipe) {
				recipes.add(kikonoRecipe);
			}
		}

		if (!recipes.isEmpty()) {
			registration.addRecipes(KikonoStationCategory.TYPE, recipes);
		}
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(MainBlocks.KIKONO_STATION.get()), KikonoStationCategory.TYPE);
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addRecipeClickArea(KikonoStationScreen.class, 111, 35, 26, 17, KikonoStationCategory.TYPE);
	}
}

