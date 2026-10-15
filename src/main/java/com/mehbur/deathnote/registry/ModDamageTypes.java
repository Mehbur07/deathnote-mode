package com.mehbur.deathnote.registry;

import com.mehbur.deathnote.DeathNoteMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * The Death Note damage type ({@code data/deathnote/damage_type/death_note.json}).
 * It is added to every vanilla {@code bypasses_*} damage-type tag, so armor, effects, enchantments,
 * shields, creative invulnerability and Totems of Undying cannot prevent the death.
 */
public final class ModDamageTypes {
	public static final ResourceKey<DamageType> DEATH_NOTE =
			ResourceKey.create(Registries.DAMAGE_TYPE, DeathNoteMod.id("death_note"));

	private ModDamageTypes() {
	}

	/**
	 * Creates an anonymous Death Note damage source (no attacker), so the death message
	 * does not reveal who wrote the name.
	 */
	public static DamageSource source(ServerLevel level) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DEATH_NOTE));
	}
}
