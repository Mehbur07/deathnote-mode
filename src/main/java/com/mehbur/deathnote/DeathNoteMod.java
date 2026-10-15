package com.mehbur.deathnote;

import com.mehbur.deathnote.registry.ModComponents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (client + dedicated server) entrypoint.
 * Registries, networking and server-side logic are wired up from here in later phases.
 */
public final class DeathNoteMod implements ModInitializer {
	public static final String MOD_ID = "deathnote";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModComponents.init();
		LOGGER.info("Death Note initialized.");
	}

	/** Builds an identifier in this mod's namespace, e.g. {@code deathnote:death_note}. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
