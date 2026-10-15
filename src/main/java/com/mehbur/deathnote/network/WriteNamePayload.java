package com.mehbur.deathnote.network;

import com.mehbur.deathnote.DeathNoteMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

/**
 * Client → server: "write this name in the Death Note held in this hand".
 * The server treats every field as untrusted and re-validates everything.
 */
public record WriteNamePayload(String name, InteractionHand hand) implements CustomPacketPayload {
	/** Minecraft usernames are at most 16 characters. */
	public static final int MAX_NAME_LENGTH = 16;

	public static final Type<WriteNamePayload> TYPE = new Type<>(DeathNoteMod.id("write_name"));

	private static final StreamCodec<io.netty.buffer.ByteBuf, InteractionHand> HAND_CODEC =
			ByteBufCodecs.BOOL.map(mainHand -> mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
					hand -> hand == InteractionHand.MAIN_HAND);

	public static final StreamCodec<RegistryFriendlyByteBuf, WriteNamePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), WriteNamePayload::name,
			HAND_CODEC, WriteNamePayload::hand,
			WriteNamePayload::new);

	@Override
	public Type<WriteNamePayload> type() {
		return TYPE;
	}
}
