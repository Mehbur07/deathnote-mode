package com.mehbur.deathnote.item;

import com.mehbur.deathnote.registry.ModEnchantments;
import net.minecraft.world.item.ItemStack;

/**
 * Decides what the anvil may do with Death Note inputs. Kept separate from the mixin so the rules
 * are readable in one place.
 * <ul>
 *   <li>Death Note book + single feather / writable book: {@link Verdict#APPLY}</li>
 *   <li>Death Note book + anything else: {@link Verdict#BLOCK}</li>
 *   <li>Death Note item or book on the left with anything on the right: {@link Verdict#BLOCK} (renaming alone is allowed)</li>
 *   <li>Death Note item used as the sacrifice on the right: {@link Verdict#BLOCK}</li>
 *   <li>Everything else: {@link Verdict#VANILLA}</li>
 * </ul>
 */
public final class DeathNoteAnvilRules {
	public enum Verdict {
		/** Not a Death Note case; keep the vanilla result. */
		VANILLA,
		/** Valid application; style the vanilla result as a Death Note. */
		APPLY,
		/** Forbidden combination; clear the result. */
		BLOCK
	}

	private DeathNoteAnvilRules() {
	}

	public static Verdict evaluate(ItemStack left, ItemStack right) {
		if (ModEnchantments.storesDeathNote(right)) {
			return DeathNoteItems.canReceiveDeathNote(left) ? Verdict.APPLY : Verdict.BLOCK;
		}
		if (DeathNoteItems.isDeathNoteItem(right)) {
			return Verdict.BLOCK;
		}
		boolean leftIsDeathNote = DeathNoteItems.isDeathNoteItem(left) || ModEnchantments.storesDeathNote(left);
		if (leftIsDeathNote && !right.isEmpty()) {
			return Verdict.BLOCK;
		}
		return Verdict.VANILLA;
	}
}
