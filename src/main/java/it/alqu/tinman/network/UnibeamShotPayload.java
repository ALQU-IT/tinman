package it.alqu.tinman.network;

import it.alqu.tinman.TinMan;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * "This player's unibeam currently runs from here to there."
 *
 * <p>The beam is a hitscan the server re-resolves every tick it is held, so there is nothing in
 * the world for a client to look at — no entity, no block. The two ends of the line the server
 * actually traced are sent instead, once a tick, and each client near enough to see it keeps one
 * beam per shooter alive for as long as the updates keep coming. Both ends travel rather than an
 * origin and a direction, so what is drawn is exactly the segment that was damaged, right down to
 * where terrain cut it short.
 *
 * <p>The shooter's entity id is what makes a held beam refresh in place instead of piling twenty
 * overlapping copies a second on top of each other.
 */
public record UnibeamShotPayload(int shooterId, Vec3 start, Vec3 end) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<UnibeamShotPayload> TYPE =
		new CustomPacketPayload.Type<>(TinMan.id("unibeam_shot"));

	public static final StreamCodec<RegistryFriendlyByteBuf, UnibeamShotPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, UnibeamShotPayload::shooterId,
		Vec3.STREAM_CODEC, UnibeamShotPayload::start,
		Vec3.STREAM_CODEC, UnibeamShotPayload::end,
		UnibeamShotPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
