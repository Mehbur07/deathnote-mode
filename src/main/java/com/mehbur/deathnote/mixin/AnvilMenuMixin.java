package com.mehbur.deathnote.mixin;

import com.mehbur.deathnote.config.DeathNoteConfig;
import com.mehbur.deathnote.item.DeathNoteAnvilRules;
import com.mehbur.deathnote.item.DeathNoteItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enforces the Death Note anvil rules ({@link DeathNoteAnvilRules}) after vanilla computes its result.
 * Runs on both sides, so the client preview always matches what the server allows.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
	@Shadow
	@Final
	private DataSlot cost;

	@Shadow
	private int repairItemCountCost;

	private AnvilMenuMixin(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access,
			ItemCombinerMenuSlotDefinition slotDefinition) {
		super(type, containerId, inventory, access, slotDefinition);
	}

	@Inject(method = "createResult", at = @At("TAIL"))
	private void deathnote$applyDeathNoteRules(CallbackInfo ci) {
		ItemStack left = this.inputSlots.getItem(AnvilMenu.INPUT_SLOT);
		ItemStack right = this.inputSlots.getItem(AnvilMenu.ADDITIONAL_SLOT);

		switch (DeathNoteAnvilRules.evaluate(left, right)) {
			case VANILLA -> {
				return;
			}
			case APPLY -> {
				ItemStack result = this.resultSlots.getItem(0);
				// Empty means vanilla refused (e.g. "Too Expensive"); respect that.
				if (result.isEmpty()) {
					return;
				}
				ItemStack styled = result.copyWithCount(1);
				DeathNoteItems.applyDeathNote(styled, DeathNoteConfig.get().maxUses);
				this.resultSlots.setItem(0, styled);
			}
			case BLOCK -> {
				this.resultSlots.setItem(0, ItemStack.EMPTY);
				this.cost.set(0);
				this.repairItemCountCost = 0;
			}
		}
		this.broadcastChanges();
	}
}
