package com.mehbur.deathnote.registry;

import com.mehbur.deathnote.DeathNoteMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;

/**
 * Custom item data components.
 */
public final class ModComponents {
	/**
	 * Remaining Death Note uses on a stack. This is the single source of truth for the use counter;
	 * the vanilla damage bar is only a visual mirror of it. Synced to clients so the tooltip can show it.
	 */
	public static final DataComponentType<Integer> USES_REMAINING = register("uses_remaining",
			DataComponentType.<Integer>builder()
					.persistent(ExtraCodecs.NON_NEGATIVE_INT)
					.networkSynchronized(ByteBufCodecs.VAR_INT)
					.build());

	private ModComponents() {
	}

	private static <T> DataComponentType<T> register(String path, DataComponentType<T> type) {
		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, DeathNoteMod.id(path), type);
	}

	/** Forces class loading so the static registrations run during mod initialization. */
	public static void init() {
	}
}
