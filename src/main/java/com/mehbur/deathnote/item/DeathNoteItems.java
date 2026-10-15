package com.mehbur.deathnote.item;

import com.mehbur.deathnote.DeathNoteMod;
import com.mehbur.deathnote.registry.ModComponents;
import com.mehbur.deathnote.registry.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Helpers for Death Note items: a vanilla feather or writable book carrying the Death Note enchantment.
 * No new items are registered; the look and behaviour come entirely from data components.
 */
public final class DeathNoteItems {
	/** Item model ids ({@code assets/deathnote/items/*.json}): black notebook and golden quill. */
	public static final Identifier BOOK_MODEL = DeathNoteMod.id("death_note_book");
	public static final Identifier QUILL_MODEL = DeathNoteMod.id("death_note_quill");

	private DeathNoteItems() {
	}

	/** True for an enchanted Death Note feather or book (not the enchanted book that stores it). */
	public static boolean isDeathNoteItem(ItemStack stack) {
		return ModEnchantments.hasDeathNote(stack);
	}

	/** True if the stack may receive Death Note in the anvil: a single, not yet enchanted feather or writable book. */
	public static boolean canReceiveDeathNote(ItemStack stack) {
		return (stack.is(Items.FEATHER) || stack.is(Items.WRITABLE_BOOK))
				&& stack.getCount() == 1
				&& !ModEnchantments.hasDeathNote(stack);
	}

	/**
	 * Turns an anvil result (already carrying the enchantment) into a finished Death Note:
	 * no enchantment glint, custom model and name, unstackable, full use counter and matching durability bar.
	 */
	public static void applyDeathNote(ItemStack stack, int maxUses) {
		boolean book = stack.is(Items.WRITABLE_BOOK);

		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
		stack.set(DataComponents.ITEM_MODEL, book ? BOOK_MODEL : QUILL_MODEL);
		stack.set(DataComponents.ITEM_NAME, Component
				.translatable(book ? "item.deathnote.death_note_book" : "item.deathnote.death_note_quill")
				.withStyle(ChatFormatting.GOLD));

		// max_stack_size must be 1 before max_damage is set: damageable items cannot stack.
		stack.set(DataComponents.MAX_STACK_SIZE, 1);
		stack.set(DataComponents.MAX_DAMAGE, maxUses);
		stack.set(DataComponents.DAMAGE, 0);
		stack.set(ModComponents.USES_REMAINING, maxUses);
	}

	public static int getUsesRemaining(ItemStack stack) {
		return stack.getOrDefault(ModComponents.USES_REMAINING, 0);
	}

	/** Updates the use counter and mirrors it onto the vanilla damage bar (display only). */
	public static void setUsesRemaining(ItemStack stack, int uses) {
		int remaining = Math.max(0, uses);
		stack.set(ModComponents.USES_REMAINING, remaining);
		int max = stack.getOrDefault(DataComponents.MAX_DAMAGE, remaining);
		stack.set(DataComponents.DAMAGE, Math.max(0, max - remaining));
	}
}
