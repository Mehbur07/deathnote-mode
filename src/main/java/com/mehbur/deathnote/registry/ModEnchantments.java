package com.mehbur.deathnote.registry;

import com.mehbur.deathnote.DeathNoteMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Keys and helpers for the data-driven Death Note enchantment
 * ({@code data/deathnote/enchantment/death_note.json}).
 */
public final class ModEnchantments {
	public static final ResourceKey<Enchantment> DEATH_NOTE =
			ResourceKey.create(Registries.ENCHANTMENT, DeathNoteMod.id("death_note"));

	private ModEnchantments() {
	}

	/** True if the stack itself is enchanted with Death Note (an enchanted feather or book). */
	public static boolean hasDeathNote(ItemStack stack) {
		return contains(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
	}

	/** True if the stack is an enchanted book that stores Death Note. */
	public static boolean storesDeathNote(ItemStack stack) {
		return contains(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
	}

	private static boolean contains(ItemEnchantments enchantments) {
		return enchantments.keySet().stream().anyMatch(holder -> holder.is(DEATH_NOTE));
	}
}
