package it.alqu.tinman.storage;

import it.alqu.tinman.menu.TerminalMenu;
import it.alqu.tinman.network.TerminalViewPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Receives the terminal's search box and scroll position from the client. */
public final class TerminalNetworking {
	private TerminalNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TerminalViewPayload.TYPE, TerminalViewPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(TerminalViewPayload.TYPE, (payload, context) ->
			context.server().execute(() -> {
				// Only ever applied to a terminal the player actually has open, so a stray packet
				// cannot reach into someone else's menu or a menu of another kind.
				if (context.player().containerMenu instanceof TerminalMenu menu) {
					menu.setView(payload.filter(), payload.scrollRow());
				}
			}));
	}
}
