package dev.alqu.tinman.worldgen;

import com.mojang.serialization.MapCodec;
import dev.alqu.tinman.config.TinManConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.placement.RepeatingPlacement;

/**
 * A {@code minecraft:count}-alike whose count comes from the mod config rather than the JSON.
 *
 * <p>Keeping the feature itself in JSON means datapacks can still retarget or replace it, while the
 * spawn rate stays a simple number a server admin can edit in {@code config/tinman.json} without
 * touching a datapack. Fractional counts are honoured probabilistically, so 3.5 gives three veins
 * plus a coin flip for a fourth.
 */
public class ConfigCountPlacement extends RepeatingPlacement {
	public static final ConfigCountPlacement INSTANCE = new ConfigCountPlacement();
	public static final MapCodec<ConfigCountPlacement> CODEC = MapCodec.unit(INSTANCE);

	/** Registered in {@link ModWorldGen#init()} under {@code tinman:config_count}. */
	public static final PlacementModifierType<ConfigCountPlacement> TYPE = () -> CODEC;

	private ConfigCountPlacement() {
	}

	@Override
	protected int count(RandomSource random, BlockPos origin) {
		TinManConfig.Worldgen config = TinManConfig.get().worldgen;

		if (!config.voltiteOreEnabled) {
			return 0;
		}

		double count = Math.max(0.0, config.voltiteVeinsPerChunk);
		int whole = (int) count;
		return random.nextDouble() < (count - whole) ? whole + 1 : whole;
	}

	@Override
	public PlacementModifierType<?> type() {
		return TYPE;
	}
}
