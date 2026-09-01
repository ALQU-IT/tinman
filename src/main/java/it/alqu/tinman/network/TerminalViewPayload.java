package it.alqu.tinman.network;

import it.alqu.tinman.TinMan;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * "Show me this page of results for this search."
 *
 * <p>The search box and the scroll wheel live on the client, but the results do not: the pool is
 * assembled server side from chests the client never sees the contents of. So the terminal sends
 * what the player typed and which row they are on, and the server repaints the grid. Sending the
 * whole pooled inventory to the client to filter locally would mean shipping every chest in the
 * room over the wire on every change, for a search box.
 */
public record TerminalViewPayload(String filter, int scrollRow) implements CustomPacketPayload {
	/** Long enough for any item name worth typing, short enough not to be worth abusing. */
	private static final int MAX_FILTER = 64;

	public static final CustomPacketPayload.Type<TerminalViewPayload> TYPE =
		new CustomPacketPayload.Type<>(TinMan.id("terminal_view"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalViewPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.stringUtf8(MAX_FILTER), TerminalViewPayload::filter,
		ByteBufCodecs.VAR_INT, TerminalViewPayload::scrollRow,
		TerminalViewPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
