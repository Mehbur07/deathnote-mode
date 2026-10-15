package com.mehbur.deathnote.client;

import com.mehbur.deathnote.DeathNoteMod;
import com.mehbur.deathnote.client.screen.DeathNoteScreen;
import com.mehbur.deathnote.item.DeathNoteItems;
import com.mehbur.deathnote.registry.ModComponents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Client-only entrypoint: opens the name screen when a Death Note is used and shows the uses tooltip.
 */
public final class DeathNoteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		UseItemCallback.EVENT.register(DeathNoteClient::onUseItem);
		ItemTooltipCallback.EVENT.register(DeathNoteClient::addUsesTooltip);
		DeathNoteMod.LOGGER.info("Death Note client initialized.");
	}

	/** Adds "Uses left: X/Y" right below the item name. Values come from the server-synced components. */
	private static void addUsesTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
		Integer uses = stack.get(ModComponents.USES_REMAINING);
		if (uses == null) {
			return;
		}
		int maxUses = stack.getOrDefault(DataComponents.MAX_DAMAGE, uses);
		lines.add(Math.min(1, lines.size()),
				Component.translatable("deathnote.tooltip.uses", uses, maxUses).withStyle(ChatFormatting.GOLD));
	}

	/**
	 * Client side of a right-click. Returning SUCCESS stops the vanilla writable-book screen from opening;
	 * the use packet still reaches the server, whose listener claims the interaction there too.
	 */
	private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
		if (!level.isClientSide() || !DeathNoteItems.isDeathNoteItem(player.getItemInHand(hand))) {
			return InteractionResult.PASS;
		}
		Minecraft.getInstance().setScreenAndShow(new DeathNoteScreen(hand));
		return InteractionResult.SUCCESS;
	}
}
