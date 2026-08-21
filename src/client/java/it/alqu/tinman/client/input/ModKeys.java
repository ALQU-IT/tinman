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

			// consumeClick drains the queued presses, so holding the key does not spam the server;
			// the cooldown is still enforced server side regardless of what arrives.
			while (FIRE_UNIBEAM.consumeClick()) {
				ClientPlayNetworking.send(FireUnibeamPayload.INSTANCE);
			}
		});
	}
}
