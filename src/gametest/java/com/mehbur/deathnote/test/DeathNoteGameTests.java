package com.mehbur.deathnote.test;

import com.mehbur.deathnote.item.DeathNoteItems;
import com.mehbur.deathnote.network.DeathNoteServerHandler;
import com.mehbur.deathnote.network.DeathNoteServerHandler.Outcome;
import com.mehbur.deathnote.recipe.DeathNoteBookRecipe;
import com.mehbur.deathnote.registry.ModDamageTypes;
import com.mehbur.deathnote.registry.ModEnchantments;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.CrafterBlock;

import java.util.List;
import java.util.Optional;

/**
 * Server game tests, run headless by {@code gradlew build}. They exercise the real datapack, mixins and
 * server logic: enchantment restrictions, the recipe, anvil/grindstone/repair rules and the kill flow.
 */
public final class DeathNoteGameTests {
	private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

	// ---------------------------------------------------------------- helpers

	private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	private static ItemStack book(GameTestHelper helper, ResourceKey<Enchantment> key, int level) {
		return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment(helper, key), level));
	}

	private static ItemStack deathNoteBook(GameTestHelper helper) {
		return book(helper, ModEnchantments.DEATH_NOTE, 1);
	}

	private static ItemStack quill(GameTestHelper helper) {
		return DeathNoteItems.create(Items.FEATHER, enchantment(helper, ModEnchantments.DEATH_NOTE), 30);
	}

	/** 3x3 Death Note layout with the given middle-row books. */
	private static CraftingInput recipeGrid(ItemStack left, ItemStack right) {
		ItemStack empty = ItemStack.EMPTY;
		ItemStack ingot = new ItemStack(Items.NETHERITE_INGOT);
		return CraftingInput.of(3, 3, List.of(
				empty, ingot, empty,
				left, new ItemStack(Items.NETHER_STAR), right,
				empty, ingot.copy(), empty));
	}

	private static Optional<RecipeHolder<CraftingRecipe>> findRecipe(GameTestHelper helper, CraftingInput input) {
		return helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
	}

	private static ItemStack anvil(Player player, ItemStack left, ItemStack right) {
		AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
		menu.getSlot(AnvilMenu.INPUT_SLOT).set(left);
		menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right);
		menu.createResult();
		return menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
	}

	private static ItemStack grindstone(Player player, ItemStack input) {
		GrindstoneMenu menu = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
		menu.getSlot(GrindstoneMenu.INPUT_SLOT).set(input);
		return menu.getSlot(GrindstoneMenu.RESULT_SLOT).getItem();
	}

	/** A survival writer holding a fresh Death Note quill. */
	private static ServerPlayer writer(GameTestHelper helper, int levels) {
		ServerPlayer writer = helper.makeMockServerPlayerInLevel();
		writer.setGameMode(GameType.SURVIVAL);
		writer.getAbilities().instabuild = false;
		writer.experienceLevel = levels;
		writer.setItemInHand(HAND, quill(helper));
		return writer;
	}

	private static ServerPlayer target(GameTestHelper helper) {
		return helper.makeMockServerPlayerInLevel();
	}

	private static Outcome write(GameTestHelper helper, ServerPlayer writer, ServerPlayer target) {
		return DeathNoteServerHandler.writeTarget(helper.getLevel().getServer(), writer, HAND, target, "target");
	}

	private static int uses(ServerPlayer player) {
		return DeathNoteItems.getUsesRemaining(player.getItemInHand(HAND));
	}

	// ---------------------------------------------------------------- data

	@GameTest
	public void enchantment_is_restricted_to_feather_and_book(GameTestHelper helper) {
		Holder<Enchantment> deathNote = enchantment(helper, ModEnchantments.DEATH_NOTE);

		helper.assertFalse(deathNote.is(EnchantmentTags.IN_ENCHANTING_TABLE), "must not be in the enchanting table");
		helper.assertFalse(deathNote.is(EnchantmentTags.ON_RANDOM_LOOT), "must not be random loot");
		helper.assertFalse(deathNote.is(EnchantmentTags.TRADEABLE), "must not be tradeable");
		helper.assertFalse(deathNote.is(EnchantmentTags.NON_TREASURE), "must not be non-treasure");

		helper.assertTrue(deathNote.value().canEnchant(new ItemStack(Items.FEATHER)), "feather must be supported");
		helper.assertTrue(deathNote.value().canEnchant(new ItemStack(Items.WRITABLE_BOOK)), "writable book must be supported");
		helper.assertFalse(deathNote.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD)), "sword must not be supported");
		helper.assertFalse(deathNote.value().canEnchant(new ItemStack(Items.BOOK)), "plain book must not be supported");
		helper.succeed();
	}

	@GameTest
	public void damage_type_bypasses_all_protection(GameTestHelper helper) {
		Holder<DamageType> type = helper.getLevel().registryAccess()
				.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ModDamageTypes.DEATH_NOTE);
		helper.assertTrue(type.is(DamageTypeTags.BYPASSES_INVULNERABILITY), "bypasses_invulnerability (totem, creative)");
		helper.assertTrue(type.is(DamageTypeTags.BYPASSES_ARMOR), "bypasses_armor");
		helper.assertTrue(type.is(DamageTypeTags.BYPASSES_EFFECTS), "bypasses_effects");
		helper.assertTrue(type.is(DamageTypeTags.BYPASSES_RESISTANCE), "bypasses_resistance");
		helper.assertTrue(type.is(DamageTypeTags.BYPASSES_ENCHANTMENTS), "bypasses_enchantments");
		helper.assertTrue(type.is(DamageTypeTags.BYPASSES_SHIELD), "bypasses_shield");

		// The anonymous source must produce the "killed by the Death Note" message.
		ServerPlayer victim = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(ModDamageTypes.source(helper.getLevel()).getLocalizedDeathMessage(victim).getContents()
						instanceof TranslatableContents contents && contents.getKey().equals("death.attack.deathnote"),
				"death message key must be death.attack.deathnote");
		helper.succeed();
	}

	// ---------------------------------------------------------------- recipe

	@GameTest
	public void recipe_accepts_both_book_orders(GameTestHelper helper) {
		ItemStack sharpness = book(helper, Enchantments.SHARPNESS, 5);
		ItemStack smite = book(helper, Enchantments.SMITE, 5);

		for (CraftingInput input : List.of(recipeGrid(sharpness, smite), recipeGrid(smite.copy(), sharpness.copy()))) {
			Optional<RecipeHolder<CraftingRecipe>> recipe = findRecipe(helper, input);
			helper.assertTrue(recipe.isPresent(), "recipe must match");
			helper.assertTrue(recipe.get().value() instanceof DeathNoteBookRecipe, "must be the Death Note recipe");
			ItemStack result = recipe.get().value().assemble(input);
			helper.assertTrue(result.is(Items.ENCHANTED_BOOK), "result must be an enchanted book");
			helper.assertTrue(ModEnchantments.storesDeathNote(result), "result must store Death Note");
			helper.assertValueEqual(result.getCount(), 1, "result count");
		}
		helper.succeed();
	}

	@GameTest
	public void recipe_rejects_wrong_books_and_layouts(GameTestHelper helper) {
		ItemStack sharpness = book(helper, Enchantments.SHARPNESS, 5);
		ItemStack smite = book(helper, Enchantments.SMITE, 5);

		ItemStack sharpnessFour = book(helper, Enchantments.SHARPNESS, 4);
		helper.assertTrue(findRecipe(helper, recipeGrid(sharpnessFour, smite)).isEmpty(), "Sharpness IV must not work");

		ItemStack twoSharpness = book(helper, Enchantments.SHARPNESS, 5);
		helper.assertTrue(findRecipe(helper, recipeGrid(twoSharpness, sharpness)).isEmpty(), "two Sharpness books must not work");

		ItemStack extraEnchant = smite.copy();
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		EnchantmentHelper.updateEnchantments(extraEnchant, mutable -> mutable.set(unbreaking, 3));
		helper.assertTrue(findRecipe(helper, recipeGrid(sharpness, extraEnchant)).isEmpty(), "extra stored enchantment must not work");

		ItemStack empty = ItemStack.EMPTY;
		ItemStack ingot = new ItemStack(Items.NETHERITE_INGOT);
		CraftingInput cornerFilled = CraftingInput.of(3, 3, List.of(
				new ItemStack(Items.DIRT), ingot, empty,
				sharpness, new ItemStack(Items.NETHER_STAR), smite,
				empty, ingot.copy(), empty));
		helper.assertTrue(findRecipe(helper, cornerFilled).isEmpty(), "a filled corner must not work");
		helper.succeed();
	}

	@GameTest
	public void crafter_cannot_craft_death_note(GameTestHelper helper) {
		CraftingInput input = recipeGrid(book(helper, Enchantments.SHARPNESS, 5), book(helper, Enchantments.SMITE, 5));
		helper.assertTrue(findRecipe(helper, input).isPresent(), "control: the crafting table recipe matches");
		helper.assertTrue(CrafterBlock.getPotentialResults(helper.getLevel(), input).isEmpty(), "crafter must not find it");
		helper.succeed();
	}

	// ---------------------------------------------------------------- anvil, grindstone, repair

	@GameTest
	public void anvil_turns_single_feather_into_quill(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack result = anvil(player, new ItemStack(Items.FEATHER), deathNoteBook(helper));

		helper.assertTrue(result.is(Items.FEATHER), "result must be a feather");
		helper.assertTrue(DeathNoteItems.isDeathNoteItem(result), "result must carry Death Note");
		helper.assertValueEqual(result.getCount(), 1, "count");
		helper.assertValueEqual(result.get(DataComponents.ITEM_MODEL), DeathNoteItems.QUILL_MODEL, "quill model");
		helper.assertValueEqual(result.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), false, "no glint");
		helper.assertValueEqual(result.getMaxStackSize(), 1, "max stack size");
		helper.assertValueEqual(DeathNoteItems.getUsesRemaining(result), 30, "uses");
		helper.assertValueEqual(result.getMaxDamage(), 30, "durability bar max");
		helper.succeed();
	}

	@GameTest
	public void anvil_turns_writable_book_into_notebook(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack result = anvil(player, new ItemStack(Items.WRITABLE_BOOK), deathNoteBook(helper));

		helper.assertTrue(result.is(Items.WRITABLE_BOOK), "result must be a writable book");
		helper.assertTrue(DeathNoteItems.isDeathNoteItem(result), "result must carry Death Note");
		helper.assertValueEqual(result.get(DataComponents.ITEM_MODEL), DeathNoteItems.BOOK_MODEL, "notebook model");
		helper.succeed();
	}

	@GameTest
	public void anvil_blocks_every_other_death_note_combination(GameTestHelper helper) {
		Player creative = helper.makeMockPlayer(GameType.CREATIVE);
		creative.getAbilities().instabuild = true;

		// Control: the harness really computes anvil results.
		ItemStack control = anvil(creative, new ItemStack(Items.DIAMOND_SWORD), book(helper, Enchantments.SHARPNESS, 5));
		helper.assertFalse(control.isEmpty(), "control: vanilla sword + Sharpness must work");

		helper.assertTrue(anvil(creative, new ItemStack(Items.DIAMOND_SWORD), deathNoteBook(helper)).isEmpty(),
				"Death Note book on a sword must fail, even in creative");
		helper.assertTrue(anvil(creative, new ItemStack(Items.BOOK), deathNoteBook(helper)).isEmpty(),
				"Death Note book on a plain book must fail");
		helper.assertTrue(anvil(creative, new ItemStack(Items.FEATHER, 2), deathNoteBook(helper)).isEmpty(),
				"a stack of feathers must fail");
		helper.assertTrue(anvil(creative, quill(helper), deathNoteBook(helper)).isEmpty(),
				"an existing Death Note must not take another book");
		helper.assertTrue(anvil(creative, quill(helper), new ItemStack(Items.FEATHER)).isEmpty(),
				"a Death Note must not be combined with anything");
		helper.assertTrue(anvil(creative, new ItemStack(Items.FEATHER), quill(helper)).isEmpty(),
				"a Death Note must not be sacrificed");
		helper.assertTrue(anvil(creative, deathNoteBook(helper), deathNoteBook(helper)).isEmpty(),
				"two Death Note books must not combine");
		helper.succeed();
	}

	@GameTest
	public void grindstone_cannot_erase_death_note(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertFalse(grindstone(player, book(helper, Enchantments.SHARPNESS, 5)).isEmpty(),
				"control: grindstone works on a normal enchanted book");
		helper.assertTrue(grindstone(player, quill(helper)).isEmpty(), "quill must not be ground");
		helper.assertTrue(grindstone(player, deathNoteBook(helper)).isEmpty(), "Death Note book must not be ground");
		helper.succeed();
	}

	@GameTest
	public void repair_recipe_cannot_merge_death_notes(GameTestHelper helper) {
		ItemStack swordA = new ItemStack(Items.DIAMOND_SWORD);
		ItemStack swordB = new ItemStack(Items.DIAMOND_SWORD);
		swordA.setDamageValue(100);
		swordB.setDamageValue(100);
		helper.assertTrue(RepairItemRecipe.INSTANCE.matches(CraftingInput.of(2, 1, List.of(swordA, swordB)), helper.getLevel()),
				"control: two damaged swords can be repaired");

		ItemStack quillA = quill(helper);
		ItemStack quillB = quill(helper);
		DeathNoteItems.setUsesRemaining(quillA, 10);
		DeathNoteItems.setUsesRemaining(quillB, 10);
		helper.assertFalse(RepairItemRecipe.INSTANCE.matches(CraftingInput.of(2, 1, List.of(quillA, quillB)), helper.getLevel()),
				"two Death Notes must not be merged");
		helper.succeed();
	}

	// ---------------------------------------------------------------- kill flow

	@GameTest
	public void kill_ignores_totem_resistance_and_creative(GameTestHelper helper) {
		ServerPlayer writer = writer(helper, 35);
		ServerPlayer target = target(helper);
		target.setGameMode(GameType.CREATIVE);
		target.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
		target.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
		target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 254));

		helper.assertValueEqual(write(helper, writer, target), Outcome.SUCCESS, "outcome");
		helper.assertTrue(target.isDeadOrDying(), "target must be dead");
		helper.assertValueEqual(writer.experienceLevel, 5, "writer levels after paying 30");
		helper.assertValueEqual(uses(writer), 29, "uses left");
		helper.assertValueEqual(writer.getItemInHand(HAND).getDamageValue(), 1, "durability bar mirrors uses");
		helper.assertFalse(helper.getLevel().getEntities(EntityTypes.LIGHTNING_BOLT, target.getBoundingBox().inflate(4), bolt -> true).isEmpty(),
				"a lightning bolt must strike the target");
		helper.succeed();
	}

	@GameTest
	public void spectator_target_dies(GameTestHelper helper) {
		ServerPlayer writer = writer(helper, 30);
		ServerPlayer target = target(helper);
		target.setGameMode(GameType.SPECTATOR);

		helper.assertValueEqual(write(helper, writer, target), Outcome.SUCCESS, "outcome");
		helper.assertTrue(target.isDeadOrDying(), "spectator target must be dead");
		helper.succeed();
	}

	@GameTest
	public void not_enough_levels_spends_nothing(GameTestHelper helper) {
		ServerPlayer writer = writer(helper, 29);
		ServerPlayer target = target(helper);

		helper.assertValueEqual(write(helper, writer, target), Outcome.NOT_ENOUGH_LEVELS, "outcome");
		helper.assertTrue(target.isAlive(), "target must live");
		helper.assertValueEqual(writer.experienceLevel, 29, "levels unchanged");
		helper.assertValueEqual(uses(writer), 30, "uses unchanged");
		helper.succeed();
	}

	@GameTest
	public void unknown_and_invalid_names_spend_nothing(GameTestHelper helper) {
		ServerPlayer writer = writer(helper, 30);
		var server = helper.getLevel().getServer();

		helper.assertValueEqual(DeathNoteServerHandler.write(server, writer, HAND, "NoSuchPlayer42"), Outcome.NOT_FOUND, "unknown name");
		helper.assertValueEqual(DeathNoteServerHandler.write(server, writer, HAND, "bad name!"), Outcome.INVALID_NAME, "invalid name");
		helper.assertValueEqual(DeathNoteServerHandler.write(server, writer, HAND, ""), Outcome.INVALID_NAME, "empty name");
		helper.assertValueEqual(writer.experienceLevel, 30, "levels unchanged");
		helper.assertValueEqual(uses(writer), 30, "uses unchanged");
		helper.succeed();
	}

	@GameTest
	public void self_kill_is_rejected(GameTestHelper helper) {
		ServerPlayer writer = writer(helper, 30);

		helper.assertValueEqual(write(helper, writer, writer), Outcome.SELF, "outcome");
		helper.assertTrue(writer.isAlive(), "writer must live");
		helper.assertValueEqual(uses(writer), 30, "uses unchanged");
		helper.succeed();
	}

	@GameTest
	public void not_holding_death_note_is_rejected(GameTestHelper helper) {
		ServerPlayer writer = writer(helper, 30);
		writer.setItemInHand(HAND, new ItemStack(Items.FEATHER));
		ServerPlayer target = target(helper);

		helper.assertValueEqual(write(helper, writer, target), Outcome.NOT_HOLDING, "outcome");
		helper.assertTrue(target.isAlive(), "target must live");
		helper.succeed();
	}

	@GameTest
	public void cooldown_blocks_second_use(GameTestHelper helper) {
		ServerPlayer writer = writer(helper, 65);
		ServerPlayer first = target(helper);
		ServerPlayer second = target(helper);

		helper.assertValueEqual(write(helper, writer, first), Outcome.SUCCESS, "first use");
		helper.assertValueEqual(write(helper, writer, second), Outcome.COOLDOWN, "second use");
		helper.assertTrue(second.isAlive(), "second target must live");
		helper.assertValueEqual(writer.experienceLevel, 35, "only one payment");
		helper.assertValueEqual(uses(writer), 29, "only one use spent");
		helper.succeed();
	}

	@GameTest
	public void creative_writer_pays_nothing(GameTestHelper helper) {
		ServerPlayer writer = writer(helper, 0);
		writer.setGameMode(GameType.CREATIVE);
		writer.getAbilities().instabuild = true;
		ServerPlayer target = target(helper);

		helper.assertValueEqual(write(helper, writer, target), Outcome.SUCCESS, "outcome");
		helper.assertTrue(target.isDeadOrDying(), "target must be dead");
		helper.assertValueEqual(writer.experienceLevel, 0, "no levels spent");
		helper.assertValueEqual(uses(writer), 30, "no uses spent");
		helper.succeed();
	}

	@GameTest
	public void last_use_breaks_the_item(GameTestHelper helper) {
		ServerPlayer writer = writer(helper, 30);
		DeathNoteItems.setUsesRemaining(writer.getItemInHand(HAND), 1);
		ServerPlayer target = target(helper);

		helper.assertValueEqual(write(helper, writer, target), Outcome.SUCCESS, "outcome");
		helper.assertTrue(writer.getItemInHand(HAND).isEmpty(), "the Death Note must break");
		helper.succeed();
	}
}
