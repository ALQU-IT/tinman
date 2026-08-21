package it.alqu.tinman.component;

import com.mojang.serialization.Codec;
import it.alqu.tinman.TinMan;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModComponents {
	private ModComponents() {
	}

	/**
	 * Stored energy, in the same units the config uses.
	 *
	 * <p>Persistent so it survives death, chests and world reloads, and network-synchronised so the
	 * client can draw the bar and HUD straight from the stack without a bespoke packet.
	 */
	public static final DataComponentType<Integer> ENERGY = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		TinMan.id("energy"),
		DataComponentType.<Integer>builder()
			.persistent(Codec.INT)
			.networkSynchronized(ByteBufCodecs.VAR_INT)
			.build()
	);

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
