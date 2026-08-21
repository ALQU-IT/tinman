package it.alqu.tinman.registry;

import it.alqu.tinman.TinMan;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModParticles {
	private ModParticles() {
	}

	/** Boot and hand thruster plume while flying. */
	public static final SimpleParticleType THRUSTER_FLAME = register("thruster_flame");
	/** Sparks thrown off the Assembler and the Charging Station while they work. */
	public static final SimpleParticleType ASSEMBLER_SPARK = register("assembler_spark");

	private static SimpleParticleType register(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, TinMan.id(name), FabricParticleTypes.simple());
	}

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
