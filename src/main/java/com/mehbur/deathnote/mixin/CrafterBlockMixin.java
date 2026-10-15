package com.mehbur.deathnote.mixin;

import com.mehbur.deathnote.recipe.DeathNoteBookRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Keeps the Death Note recipe crafting-table-only: the crafter block also has a 3x3 grid,
 * so its recipe lookup (used for both crafting and the result preview) filters the recipe out.
 */
@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {
	@Inject(method = "getPotentialResults", at = @At("RETURN"), cancellable = true)
	private static void deathnote$excludeDeathNoteRecipe(ServerLevel level, CraftingInput input,
			CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir) {
		Optional<RecipeHolder<CraftingRecipe>> result = cir.getReturnValue();
		if (result.isPresent() && result.get().value() instanceof DeathNoteBookRecipe) {
			cir.setReturnValue(Optional.empty());
		}
	}
}
