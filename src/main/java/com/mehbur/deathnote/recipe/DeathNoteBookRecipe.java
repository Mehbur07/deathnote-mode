package com.mehbur.deathnote.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * Crafting-table-only recipe for the Death Note enchanted book:
 * <pre>
 *   [ ]        [netherite] [ ]
 *   [book A]   [star]      [book B]
 *   [ ]        [netherite] [ ]
 * </pre>
 * Books A and B must be enchanted books storing exactly Sharpness V and Smite V (either side).
 * <p>
 * The pattern spans the full 3x3 bounding box, so it can never fit the 2x2 inventory grid.
 * The crafter block is excluded separately by {@code CrafterBlockMixin}.
 * <p>
 * The result is read from the recipe JSON as an {@link ItemStackTemplate}: {@code assemble} has no
 * registry access in 26.x, and the template's enchantment holder is resolved when the recipe loads.
 */
public final class DeathNoteBookRecipe extends CustomRecipe {
	private static final int REQUIRED_LEVEL = 5;

	public static final MapCodec<DeathNoteBookRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
	).apply(instance, DeathNoteBookRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, DeathNoteBookRecipe> STREAM_CODEC =
			ItemStackTemplate.STREAM_CODEC.map(DeathNoteBookRecipe::new, recipe -> recipe.result);

	public static final RecipeSerializer<DeathNoteBookRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final ItemStackTemplate result;

	public DeathNoteBookRecipe(ItemStackTemplate result) {
		this.result = result;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		// The input is trimmed to its bounding box, so a valid layout is always exactly 3x3.
		if (input.width() != 3 || input.height() != 3) {
			return false;
		}

		boolean cornersEmpty = input.getItem(0, 0).isEmpty() && input.getItem(2, 0).isEmpty()
				&& input.getItem(0, 2).isEmpty() && input.getItem(2, 2).isEmpty();
		if (!cornersEmpty) {
			return false;
		}

		boolean frameMatches = input.getItem(1, 0).is(Items.NETHERITE_INGOT)
				&& input.getItem(1, 1).is(Items.NETHER_STAR)
				&& input.getItem(1, 2).is(Items.NETHERITE_INGOT);
		if (!frameMatches) {
			return false;
		}

		ItemStack left = input.getItem(0, 1);
		ItemStack right = input.getItem(2, 1);
		return (storesOnly(left, Enchantments.SHARPNESS) && storesOnly(right, Enchantments.SMITE))
				|| (storesOnly(left, Enchantments.SMITE) && storesOnly(right, Enchantments.SHARPNESS));
	}

	/** True if the stack is an enchanted book storing exactly one enchantment: {@code key} at level V. */
	private static boolean storesOnly(ItemStack stack, ResourceKey<Enchantment> key) {
		if (!stack.is(Items.ENCHANTED_BOOK)) {
			return false;
		}
		ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		if (stored.size() != 1) {
			return false;
		}
		Object2IntMap.Entry<Holder<Enchantment>> entry = stored.entrySet().iterator().next();
		return entry.getKey().is(key) && entry.getIntValue() == REQUIRED_LEVEL;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		return result.create();
	}

	@Override
	public RecipeSerializer<DeathNoteBookRecipe> getSerializer() {
		return SERIALIZER;
	}
}
