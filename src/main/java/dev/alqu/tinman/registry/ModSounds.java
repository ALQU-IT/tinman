package dev.alqu.tinman.registry;

import dev.alqu.tinman.TinMan;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	private ModSounds() {
	}

	/** Plays when the full set comes online and flight is granted. */
	public static final SoundEvent SUIT_POWER_UP = register("suit_power_up");
	/** Plays when charge runs out and flight cuts. */
	public static final SoundEvent SUIT_POWER_DOWN = register("suit_power_down");
	/** Looping jet note while boosting. */
	public static final SoundEvent THRUSTER_LOOP = register("thruster_loop");
	/** Pulse Gauntlet discharge. */
	public static final SoundEvent REPULSOR_FIRE = register("repulsor_fire");
	/** Assembler drone while an assembly is running. */
	public static final SoundEvent ASSEMBLER_HUM = register("assembler_hum");

	private static SoundEvent register(String name) {
		Identifier id = TinMan.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
