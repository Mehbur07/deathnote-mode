package com.mehbur.deathnote.client;

import com.mehbur.deathnote.DeathNoteMod;
import com.mehbur.deathnote.client.screen.DeathNoteScreen;
import com.mehbur.deathnote.item.DeathNoteItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Client-only entrypoint: opens the name screen when a Death Note is used.
 */
public final class DeathNoteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		UseItemCallback.EVENT.register(DeathNoteClient::onUseItem);
		DeathNoteMod.LOGGER.info("Death Note client initialized.");
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
