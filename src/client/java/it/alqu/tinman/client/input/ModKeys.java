package it.alqu.tinman.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import it.alqu.tinman.network.FireUnibeamPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeys {
	private ModKeys() {
	}

	/** Unbound-in-vanilla default, and rebindable from the controls screen like any other. */
	public static final KeyMapping FIRE_UNIBEAM = new KeyMapping(
		"key.tinman.fire_unibeam",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_R,
		KeyMapping.Category.GAMEPLAY
	);

	public static void register() {
		KeyMappingHelper.registerKeyMapping(FIRE_UNIBEAM);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}

			// Drained and thrown away: the beam is continuous, so what matters is whether the key
			// is down right now, not how many press events have queued up behind it.
			while (FIRE_UNIBEAM.consumeClick()) {
				// discarded on purpose
			}

			// One request a tick for as long as it is held. Each buys exactly one tick of beam,
			// so letting go stops it without needing to tell the server anything.
			if (FIRE_UNIBEAM.isDown()) {
				ClientPlayNetworking.send(FireUnibeamPayload.INSTANCE);
			}
		});
	}
}
