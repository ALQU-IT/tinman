package it.alqu.tinman.network;

import it.alqu.tinman.TinMan;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * "A unibeam was fired from here to there."
 *
 * <p>The shot is a hitscan the server resolves in a single tick, so there is nothing in the world
 * for a client to look at afterwards — no entity, no block. The two ends of the line the server
 * actually traced are sent instead, and each client near enough to see it draws the beam for a
 * few frames. Both ends travel rather than an origin and a direction, so what is drawn is exactly
 * the segment that was damaged, right down to where terrain cut it short.
 */
public record UnibeamShotPayload(Vec3 start, Vec3 end) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<UnibeamShotPayload> TYPE =
		new CustomPacketPayload.Type<>(TinMan.id("unibeam_shot"));

	public static final StreamCodec<RegistryFriendlyByteBuf, UnibeamShotPayload> STREAM_CODEC = StreamCodec.composite(
		Vec3.STREAM_CODEC, UnibeamShotPayload::start,
		Vec3.STREAM_CODEC, UnibeamShotPayload::end,
		UnibeamShotPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
