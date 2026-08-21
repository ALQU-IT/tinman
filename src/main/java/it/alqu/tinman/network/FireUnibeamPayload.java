package it.alqu.tinman.network;

import it.alqu.tinman.TinMan;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * "The player pressed the unibeam key."
 *
 * <p>The mod's only custom packet, and the only place one is genuinely needed: a key press is
 * client input the server cannot observe. It carries no data on purpose — the server decides
 * entirely on its own whether the shot is allowed, where it points and what it hits, so a client
 * cannot ask for a beam it has not earned.
 */
public record FireUnibeamPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FireUnibeamPayload> TYPE =
		new CustomPacketPayload.Type<>(TinMan.id("fire_unibeam"));

	public static final FireUnibeamPayload INSTANCE = new FireUnibeamPayload();

	public static final StreamCodec<RegistryFriendlyByteBuf, FireUnibeamPayload> STREAM_CODEC =
		StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
