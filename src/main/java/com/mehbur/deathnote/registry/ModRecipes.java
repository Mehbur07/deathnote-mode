package com.mehbur.deathnote.registry;

import com.mehbur.deathnote.DeathNoteMod;
import com.mehbur.deathnote.recipe.DeathNoteBookRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Recipe serializer registration. The recipe itself lives in
 * {@code data/deathnote/recipe/death_note_book.json}.
 */
public final class ModRecipes {
	private ModRecipes() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, DeathNoteMod.id("death_note_book"), DeathNoteBookRecipe.SERIALIZER);
	}
}
