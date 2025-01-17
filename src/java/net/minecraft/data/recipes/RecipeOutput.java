package net.minecraft.data.recipes;

import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public interface RecipeOutput {
    void accept(ResourceLocation pLocation, Recipe<?> pRecipe, @Nullable AdvancementHolder pAdvancement);

    Advancement.Builder advancement();
}