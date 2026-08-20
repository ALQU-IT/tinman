package dev.alqu.tinman.registry;

import dev.alqu.tinman.TinMan;
import dev.alqu.tinman.entity.PulseBolt;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	private ModEntities() {
	}

	public static final ResourceKey<EntityType<?>> PULSE_BOLT_KEY =
		ResourceKey.create(Registries.ENTITY_TYPE, TinMan.id("pulse_bolt"));

	public static final EntityType<PulseBolt> PULSE_BOLT = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		PULSE_BOLT_KEY,
		EntityType.Builder.<PulseBolt>of(PulseBolt::new, MobCategory.MISC)
			.sized(0.35F, 0.35F)
			.clientTrackingRange(4)
			.updateInterval(10)
			.noLootTable()
			.build(PULSE_BOLT_KEY)
	);

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
