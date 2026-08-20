package dev.alqu.tinman.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.alqu.tinman.TinMan;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain-old-data config, serialised to {@code config/tinman.json} with GSON.
 *
 * <p>The instance is loaded once during mod init and then read from both the logical client and
 * server. Values are only ever read, never mutated at runtime, so no synchronisation is needed.
 * Server-side values are authoritative for anything that affects gameplay: the client copy is only
 * consulted for presentation (bar colours, HUD thresholds).
 */
public class TinManConfig {
	public Worldgen worldgen = new Worldgen();
	public Suit suit = new Suit();
	public Weapons weapons = new Weapons();

	public static class Worldgen {
		/** Master switch for Voltite ore generation. */
		public boolean voltiteOreEnabled = true;
		/**
		 * Average number of Voltite vein placement attempts per chunk. Fractional values are
		 * honoured: 2.5 means two guaranteed attempts plus a 50% chance of a third.
		 * Diamond makes 11 attempts per chunk over a much taller band, so this is notably rarer.
		 */
		public double voltiteVeinsPerChunk = 3.5;
	}

	public static class Suit {
		/** Maximum energy any single armour piece can hold. */
		public int maxEnergy = 10000;
		/** Whether the full-set creative flight ability is available at all. */
		public boolean flightEnabled = true;
		/** Energy drained per second of flight, split across the four worn pieces. */
		public int flightDrainPerSecond = 20;
		/** Multiplier applied to the drain rate while sprint-boosting. */
		public double boostDrainMultiplier = 3.0;
		/** Extra velocity applied per tick while sprint-boosting. */
		public double boostSpeed = 0.085;
		/** Energy added per second by the Charging Station, per piece. */
		public int chargingStationRate = 100;
		/** Energy a single Voltite Ingot is worth when used to recharge gear. */
		public int energyPerIngot = 2500;
	}

	public static class Weapons {
		/** Damage dealt by an uncharged Pulse Gauntlet bolt. */
		public double pulseDamage = 18.0;
		/** Damage dealt by a fully charged Pulse Gauntlet bolt. */
		public double chargedPulseDamage = 45.0;
		/** Energy consumed per uncharged shot. */
		public int pulseEnergyCost = 25;
		/** Energy consumed per charged shot. */
		public int chargedPulseEnergyCost = 150;
		/** Ticks between uncharged shots. Charged shots use three times this. */
		public int pulseCooldownTicks = 2;
		/** Ticks of holding right-click needed for a full charge. */
		public int pulseChargeTicks = 10;
		/** How fast an uncharged bolt travels. */
		public double pulseVelocity = 3.2;
		/** How fast a charged bolt travels. */
		public double chargedPulseVelocity = 3.8;
		/** Whether the charged shot (and its explosion) is enabled. */
		public boolean chargedShotEnabled = true;
		/** Radius of the charged shot's explosion. */
		public double chargedShotExplosionRadius = 5.0;
		/** Whether the charged shot's explosion tears up terrain. */
		public boolean chargedShotBreaksBlocks = true;
		/** Fraction of the blocks it breaks that are thrown outward instead of just dropping. */
		public double blockLaunchChance = 0.45;
		/** How hard thrown blocks are flung. */
		public double blockLaunchPower = 0.55;
		/** Cap on thrown blocks per blast, so a big radius cannot flood the server with entities. */
		public int maxLaunchedBlocks = 90;
		/** Bonus damage the Voltite Blade deals while the wielder has energy available. */
		public double bladeEnergyBonusDamage = 9.0;
		/** Energy consumed by the Blade per empowered hit. */
		public int bladeEnergyCostPerHit = 15;
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static TinManConfig instance;

	public static TinManConfig get() {
		if (instance == null) {
			instance = load();
		}

		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("tinman.json");
	}

	private static TinManConfig load() {
		Path path = path();

		if (Files.exists(path)) {
			try {
				String json = Files.readString(path);
				TinManConfig parsed = GSON.fromJson(json, TinManConfig.class);

				if (parsed != null) {
					// Re-save so that newly added fields appear in the file for the user to edit.
					parsed.save();
					return parsed;
				}

				TinMan.LOGGER.warn("tinman.json was empty, falling back to defaults");
			} catch (Exception e) {
				TinMan.LOGGER.error("Could not read tinman.json, falling back to defaults", e);
			}
		}

		TinManConfig fresh = new TinManConfig();
		fresh.save();
		return fresh;
	}

	public void save() {
		try {
			Path path = path();
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(this));
		} catch (IOException e) {
			TinMan.LOGGER.error("Could not write tinman.json", e);
		}
	}
}
