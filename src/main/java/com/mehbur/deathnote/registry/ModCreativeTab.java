package com.mehbur.deathnote.registry;

import com.mehbur.deathnote.DeathNoteMod;
import com.mehbur.deathnote.config.DeathNoteConfig;
import com.mehbur.deathnote.item.DeathNoteItems;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/**
 * "Death Note" creative tab for testing: the enchanted book plus a ready-made notebook and quill.
 * Survival players can still only obtain the book through the crafting recipe.
 */
public final class ModCreativeTab {
	public static final ResourceKey<CreativeModeTab> KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, DeathNoteMod.id("death_note"));

	private ModCreativeTab() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.deathnote"))
				.icon(ModCreativeTab::icon)
				.displayItems((parameters, output) -> parameters.holders()
						.lookup(Registries.ENCHANTMENT)
						.flatMap(enchantments -> enchantments.get(ModEnchantments.DEATH_NOTE))
						.ifPresent(deathNote -> {
							int maxUses = DeathNoteConfig.get().maxUses;
							output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(deathNote, 1)));
							output.accept(DeathNoteItems.create(Items.WRITABLE_BOOK, deathNote, maxUses));
							output.accept(DeathNoteItems.create(Items.FEATHER, deathNote, maxUses));
						}))
				.build());
	}

	/** Tab icon: a writable book wearing the Death Note model (no enchantment needed for display). */
	private static ItemStack icon() {
		ItemStack icon = new ItemStack(Items.WRITABLE_BOOK);
		icon.set(DataComponents.ITEM_MODEL, DeathNoteItems.BOOK_MODEL);
		return icon;
	}
}
