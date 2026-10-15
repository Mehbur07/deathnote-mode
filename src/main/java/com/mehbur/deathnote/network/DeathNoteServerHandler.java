package com.mehbur.deathnote.network;

import com.mehbur.deathnote.config.DeathNoteConfig;
import com.mehbur.deathnote.item.DeathNoteItems;
import com.mehbur.deathnote.registry.ModDamageTypes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Server-side Death Note logic. Every check happens here; the client only shows the screen and sends a name.
 * <p>
 * Order of checks for a {@link WriteNamePayload}:
 * name format → held item → cooldown → XP → target online and alive → self / op protection.
 * Nothing is spent unless the target is actually killed.
 */
public final class DeathNoteServerHandler {
	/** Result of a write attempt; everything except {@link #SUCCESS} spends nothing. */
	public enum Outcome {
		SUCCESS,
		INVALID_NAME,
		NOT_HOLDING,
		COOLDOWN,
		NOT_ENOUGH_LEVELS,
		NOT_FOUND,
		SELF,
		PROTECTED
	}

	private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1," + WriteNamePayload.MAX_NAME_LENGTH + "}");

	/** Server tick of each player's last successful use. Only touched on the server thread. */
	private static final Map<UUID, Integer> LAST_USE_TICK = new HashMap<>();

	private DeathNoteServerHandler() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(WriteNamePayload.TYPE, WriteNamePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(WriteNamePayload.TYPE,
				(payload, context) -> handleWriteName(context.server(), context.player(), payload));

		UseItemCallback.EVENT.register(DeathNoteServerHandler::onUseItem);

		// Tick counts restart with each server; stale entries would otherwise block an integrated server reopen.
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LAST_USE_TICK.clear());
	}

	/**
	 * Server side of a right-click: claim the interaction so the vanilla writable-book screen never opens.
	 * The client-side listener (in the client entrypoint) opens the name screen.
	 */
	private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
		if (level.isClientSide() || !DeathNoteItems.isDeathNoteItem(player.getItemInHand(hand))) {
			return InteractionResult.PASS;
		}
		return InteractionResult.SUCCESS;
	}

	private static void handleWriteName(MinecraftServer server, ServerPlayer writer, WriteNamePayload payload) {
		write(server, writer, payload.hand(), payload.name());
	}

	/**
	 * Validates the written name, resolves it to an online player and continues with {@link #writeTarget}.
	 */
	public static Outcome write(MinecraftServer server, ServerPlayer writer, InteractionHand hand, String rawName) {
		String name = rawName.trim();
		if (!VALID_NAME.matcher(name).matches()) {
			return fail(writer, Outcome.INVALID_NAME, Component.translatable("deathnote.message.invalid_name"));
		}
		// getPlayerByName is case-insensitive but requires the exact name.
		ServerPlayer target = server.getPlayerList().getPlayerByName(name);
		return writeTarget(server, writer, hand, target, name);
	}

	/**
	 * Runs every remaining check against an already resolved target and, if all pass, kills the target
	 * and charges the writer. Public so game tests can supply the target directly.
	 *
	 * @param target      the resolved player, or {@code null} if nobody with that name is online
	 * @param writtenName the name as written, used in the "player not found" message
	 */
	public static Outcome writeTarget(MinecraftServer server, ServerPlayer writer, InteractionHand hand,
			@Nullable ServerPlayer target, String writtenName) {
		DeathNoteConfig config = DeathNoteConfig.get();
		ItemStack stack = writer.getItemInHand(hand);

		if (!DeathNoteItems.isDeathNoteItem(stack) || DeathNoteItems.getUsesRemaining(stack) <= 0) {
			return fail(writer, Outcome.NOT_HOLDING, Component.translatable("deathnote.message.not_holding"));
		}

		int now = server.getTickCount();
		int cooldownTicks = config.cooldownSeconds * 20;
		Integer lastUse = LAST_USE_TICK.get(writer.getUUID());
		if (lastUse != null && now - lastUse < cooldownTicks) {
			int secondsLeft = (int) Math.ceil((cooldownTicks - (now - lastUse)) / 20.0);
			return fail(writer, Outcome.COOLDOWN, Component.translatable("deathnote.message.cooldown", secondsLeft));
		}

		// Creative players pay nothing (no levels, no uses).
		boolean free = writer.hasInfiniteMaterials();
		if (!free && writer.experienceLevel < config.xpCost) {
			return fail(writer, Outcome.NOT_ENOUGH_LEVELS,
					Component.translatable("deathnote.message.not_enough_levels", config.xpCost));
		}

		if (target == null || !target.isAlive()) {
			return fail(writer, Outcome.NOT_FOUND, Component.translatable("deathnote.message.player_not_found", writtenName));
		}
		if (target == writer && !config.allowSelfKill) {
			return fail(writer, Outcome.SELF, Component.translatable("deathnote.message.self"));
		}
		if (config.protectOps && server.getPlayerList().isOp(target.nameAndId())) {
			return fail(writer, Outcome.PROTECTED, Component.translatable("deathnote.message.protected"));
		}

		strike(target, config);
		LAST_USE_TICK.put(writer.getUUID(), now);
		writer.sendSystemMessage(Component.translatable("deathnote.message.success", target.getName())
				.withStyle(ChatFormatting.DARK_RED));

		if (!free) {
			writer.giveExperienceLevels(-config.xpCost);
			consumeUse(writer, hand, stack);
		}
		return Outcome.SUCCESS;
	}

	/** Summons the lightning bolt and kills the target with the anonymous Death Note damage source. */
	private static void strike(ServerPlayer target, DeathNoteConfig config) {
		ServerLevel level = target.level();

		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(target.position());
			bolt.setVisualOnly(config.visualOnlyLightning);
			level.addFreshEntity(bolt);
		}

		// The damage type is in every bypasses_* tag, so armor, effects, totems and creative mode do not help.
		DamageSource source = ModDamageTypes.source(level);
		target.hurtServer(level, source, Float.MAX_VALUE);

		// Fallback for states where hurt is refused anyway (e.g. a player who has not finished loading in).
		// Recording the damage keeps the "killed by the Death Note" message, which comes from the combat tracker.
		if (!target.isDeadOrDying()) {
			target.getCombatTracker().recordDamage(source, target.getHealth());
			target.setHealth(0.0F);
			target.die(source);
		}
	}

	/** Spends one use; the item breaks when none are left. */
	private static void consumeUse(ServerPlayer writer, InteractionHand hand, ItemStack stack) {
		int remaining = DeathNoteItems.getUsesRemaining(stack) - 1;
		if (remaining > 0) {
			DeathNoteItems.setUsesRemaining(stack, remaining);
			return;
		}

		writer.setItemInHand(hand, ItemStack.EMPTY);
		writer.level().playSound(null, writer.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
		writer.sendSystemMessage(Component.translatable("deathnote.message.broken").withStyle(ChatFormatting.GRAY));
	}

	private static Outcome fail(ServerPlayer player, Outcome outcome, Component message) {
		player.sendSystemMessage(message.copy().withStyle(ChatFormatting.RED));
		return outcome;
	}
}
