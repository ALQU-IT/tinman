package dev.alqu.tinman.advancement;

import dev.alqu.tinman.TinMan;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;

public final class ModTriggers {
	private ModTriggers() {
	}

	public static final SuitFlightTrigger SUIT_FLIGHT = Registry.register(
		BuiltInRegistries.TRIGGER_TYPES,
		TinMan.id("suit_flight"),
		new SuitFlightTrigger()
	);

	/**
	 * Total blocks a player has flown under suit power. Persistent, so progress toward the
	 * thousand-block advancement survives logging out.
	 */
	public static final AttachmentType<Double> FLIGHT_DISTANCE = AttachmentRegistry.<Double>builder()
		.persistent(com.mojang.serialization.Codec.DOUBLE)
		.initializer(() -> 0.0)
		.buildAndRegister(TinMan.id("flight_distance"));

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
