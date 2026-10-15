package com.mehbur.deathnote.client;

import com.mehbur.deathnote.DeathNoteMod;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client-only entrypoint. The name-input screen, tooltip and use-callback are wired up here in later phases.
 */
public final class DeathNoteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DeathNoteMod.LOGGER.info("Death Note client initialized.");
	}
}
