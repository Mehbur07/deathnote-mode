package com.mehbur.deathnote.mixin;

import com.mehbur.deathnote.item.DeathNoteItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Death Note items use the vanilla damage bar to display remaining uses, which would make two of them
 * eligible for the crafting-grid repair recipe (merging their durability). That recipe is disabled for them.
 */
@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin {
	@Inject(
			method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
			at = @At("HEAD"),
			cancellable = true)
	private void deathnote$blockDeathNoteRepair(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
		for (ItemStack stack : input.items()) {
			if (DeathNoteItems.isDeathNoteItem(stack)) {
				cir.setReturnValue(false);
				return;
			}
		}
	}
}
