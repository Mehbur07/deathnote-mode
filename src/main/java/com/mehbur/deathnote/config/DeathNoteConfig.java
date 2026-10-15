package com.mehbur.deathnote.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mehbur.deathnote.DeathNoteMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-side settings, stored in {@code config/deathnote.json}.
 * The file is created with defaults on first launch and rewritten on load so new fields appear automatically.
 */
public final class DeathNoteConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = DeathNoteMod.MOD_ID + ".json";

	private static DeathNoteConfig instance = new DeathNoteConfig();

	/** Experience levels spent per successful use. */
	public int xpCost = 30;
	/** Total uses of a freshly made Death Note item. */
	public int maxUses = 30;
	/** Seconds a player must wait between successful uses. */
	public int cooldownSeconds = 10;
	/** Whether a player may write their own name. */
	public boolean allowSelfKill = false;
	/** Whether server operators are immune to the Death Note. */
	public boolean protectOps = false;
	/** Visual-only lightning causes no fire and no damage to bystanders. */
	public boolean visualOnlyLightning = true;

	public static DeathNoteConfig get() {
		return instance;
	}

	/** Loads the config (or defaults if missing/invalid) and writes the sanitized result back to disk. */
	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		DeathNoteConfig loaded = null;

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, DeathNoteConfig.class);
			} catch (IOException | JsonParseException e) {
				DeathNoteMod.LOGGER.warn("Could not read {}, using defaults: {}", FILE_NAME, e.getMessage());
			}
		}

		instance = loaded != null ? loaded : new DeathNoteConfig();
		instance.sanitize();
		save(path);
	}

	private void sanitize() {
		xpCost = Math.max(0, xpCost);
		maxUses = Math.max(1, maxUses);
		cooldownSeconds = Math.max(0, cooldownSeconds);
	}

	private static void save(Path path) {
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			DeathNoteMod.LOGGER.warn("Could not write {}: {}", FILE_NAME, e.getMessage());
		}
	}
}
