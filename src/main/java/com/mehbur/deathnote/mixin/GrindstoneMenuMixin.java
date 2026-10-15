package com.mehbur.deathnote.mixin;

import com.mehbur.deathnote.item.DeathNoteItems;
import com.mehbur.deathnote.registry.ModEnchantments;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A Death Note cannot be erased: the grindstone gives no result for Death Note items or books.
 * Otherwise it would strip the enchantment but leave a feather/book with the Death Note model and durability.
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {
	@Inject(method = "computeResult", at = @At("HEAD"), cancellable = true)
	private void deathnote$blockDeathNote(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> cir) {
		if (isDeathNote(input) || isDeathNote(additional)) {
			cir.setReturnValue(ItemStack.EMPTY);
		}
	}

	private static boolean isDeathNote(ItemStack stack) {
		return DeathNoteItems.isDeathNoteItem(stack) || ModEnchantments.storesDeathNote(stack);
	}
}
