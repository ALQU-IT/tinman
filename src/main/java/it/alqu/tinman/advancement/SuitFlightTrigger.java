package it.alqu.tinman.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Fires as a player accumulates distance under suit flight.
 *
 * <p>The threshold lives in the advancement JSON ({@code distance}, in blocks) rather than in code,
 * so a datapack can set its own target.
 */
public class SuitFlightTrigger extends SimpleCriterionTrigger<SuitFlightTrigger.TriggerInstance> {
	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer player, double blocksFlown) {
		this.trigger(player, instance -> blocksFlown >= instance.distance());
	}

	public record TriggerInstance(Optional<ContextAwarePredicate> player, double distance)
			implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
			i -> i.group(
					EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
					Codec.DOUBLE.optionalFieldOf("distance", 1000.0).forGetter(TriggerInstance::distance)
				)
				.apply(i, TriggerInstance::new)
		);
	}
}
