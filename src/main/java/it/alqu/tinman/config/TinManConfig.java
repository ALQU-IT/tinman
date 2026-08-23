package it.alqu.tinman.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.alqu.tinman.TinMan;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain-old-data config, serialised to {@code config/tinman.json} with GSON.
 *
 * <p>The instance is loaded once during mod init and then read from both the logical client and
 * server. Values are only ever read, never mutated at runtime, so no synchronisation is needed.
 * Server-side values are authoritative for anything that affects gameplay. The client does read
 * config to draw bars, the HUD and tooltips, so on joining a server it is sent the server's
 * numbers and applies them over its own until it disconnects — otherwise a player whose file
 * differed from the server's would be shown figures the server never uses.
 */
public class TinManConfig {
	/**
	 * Bumped whenever the shipped defaults change in a way players would want.
	 *
	 * <p>An existing file's values are always kept — silently rewriting numbers someone chose
	 * would be worse than leaving them stale — but when the file is behind, the mod says so at
	 * startup rather than letting a rebalance look like it did nothing.
	 */
	public static final int CURRENT_VERSION = 7;

	public int configVersion = CURRENT_VERSION;

	public Worldgen worldgen = new Worldgen();
	public Suit suit = new Suit();
	public Weapons weapons = new Weapons();
	public Hud hud = new Hud();

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
		/** Maximum energy a single Voltite Battery can hold. */
		public int batteryCapacity = 250000;
		/** Fraction of the energy cost each level of Conservation removes. */
		public double conservationPerLevel = 0.15;
		/** Whether the full set's chest-mounted unibeam is available. */
		public boolean unibeamEnabled = true;
		/** Damage the unibeam deals to everything it passes through. */
		public double unibeamDamage = 50.0;
		/** How far the unibeam reaches, in blocks. */
		public double unibeamRange = 32.0;
		/** Energy the unibeam draws per shot, before Conservation. */
		public int unibeamEnergyCost = 400;
		/** Ticks before the unibeam can fire again. */
		public int unibeamCooldownTicks = 40;
		/** Whether the suit leans into a dive while flying forward, the way an elytra does. */
		public boolean flightLeanEnabled = true;
		/** Horizontal speed, in blocks per tick, at which the lean reaches full. */
		public double flightLeanFullSpeed = 0.35;
		/** How much of the lean is gained or shed each tick; smaller is slower. */
		public double flightLeanRate = 0.05;
		/** Whether the full-set creative flight ability is available at all. */
		public boolean flightEnabled = true;
		/** Energy drained per second of flight, split across the four worn pieces. */
		public int flightDrainPerSecond = 20;
		/** Multiplier applied to the drain rate while sprint-boosting. */
		public double boostDrainMultiplier = 3.0;
		/** Extra velocity applied per tick while sprint-boosting. */
		public double boostSpeed = 0.085;
		/**
		 * Vertical thrust, in blocks per tick, applied while jump or sneak is held in flight.
		 *
		 * <p>Flight keeps only 0.6 of its vertical speed each tick, so a steady push settles at
		 * 1.5x itself: this default climbs at roughly 0.9 blocks a tick, about 18 a second,
		 * against the 4.5 vanilla creative flight manages. Set to 0 to leave vertical flight
		 * exactly as vanilla has it.
		 */
		public double climbSpeed = 0.6;
		/**
		 * Energy the Charging Station adds per second, to every piece it is charging at once.
		 * The default is 1.5x what the Assembler used to manage on a single item before it
		 * stopped charging at all (one 2500-energy ingot per 60-tick cycle, so 833/second).
		 */
		public int chargingStationRate = 1250;
		/** Energy a single Voltite Ingot is worth when used to recharge gear. */
		public int energyPerIngot = 2500;
	}

	public static class Hud {
		/** Whether the helmet marks nearby creatures in the world. */
		public boolean mobScannerEnabled = true;
		/** How far the scanner reaches, in blocks. */
		public double mobScannerRadius = 24.0;
		/** Most creatures marked at once. When more are in range, the nearest win. */
		public int mobScannerMaxTargets = 24;
		/** Whether marks stay visible through terrain, or are hidden by it like anything else. */
		public boolean mobScannerThroughWalls = true;
		/** Whether harmless creatures are marked as well as hostile ones. */
		public boolean mobScannerShowPassive = true;
	}

	public static class Weapons {
		/** Damage dealt by an uncharged Pulse Gauntlet bolt. */
		public double pulseDamage = 30.0;
		/** Damage dealt by a fully charged Pulse Gauntlet bolt. */
		public double chargedPulseDamage = 90.0;
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
		public double chargedShotExplosionRadius = 7.0;
		/** Whether the charged shot's explosion tears up terrain. */
		public boolean chargedShotBreaksBlocks = true;
		/** Fraction of the blocks it breaks that are thrown outward instead of just dropping. */
		public double blockLaunchChance = 0.45;
		/** How hard thrown blocks are flung. */
		public double blockLaunchPower = 0.55;
		/** Cap on thrown blocks per blast, so a big radius cannot flood the server with entities. */
		public int maxLaunchedBlocks = 140;
		/** Bonus damage the Voltite Blade deals while the wielder has energy available. */
		public double bladeEnergyBonusDamage = 15.0;
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

	/**
	 * Adopts the server's gameplay numbers for as long as this client is connected.
	 *
	 * <p>Applied over the in-memory instance rather than through a separate lookup, so every
	 * existing read of the config gets the right value with no call site needing to know. Never
	 * written to disk: the player's own file is left exactly as they wrote it.
	 */
	public void applyServerValues(it.alqu.tinman.network.ConfigSyncPayload values) {
		this.suit.batteryCapacity = values.batteryCapacity();
		this.suit.conservationPerLevel = values.conservationPerLevel();
		this.suit.flightEnabled = values.flightEnabled();
		this.suit.unibeamEnabled = values.unibeamEnabled();
		this.weapons.pulseDamage = values.pulseDamage();
		this.weapons.chargedPulseDamage = values.chargedPulseDamage();
		this.weapons.pulseEnergyCost = values.pulseEnergyCost();
		this.weapons.chargedPulseEnergyCost = values.chargedPulseEnergyCost();
		this.weapons.chargedShotEnabled = values.chargedShotEnabled();
		this.weapons.bladeEnergyBonusDamage = values.bladeEnergyBonusDamage();
		this.weapons.bladeEnergyCostPerHit = values.bladeEnergyCostPerHit();
	}

	/** Drops any adopted server values and goes back to this installation's own file. */
	public static void reloadFromDisk() {
		instance = null;
		get();
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
					if (parsed.configVersion < CURRENT_VERSION) {
						TinMan.LOGGER.warn(
							"config/tinman.json was written for config version {} and this build ships version {}. "
							+ "Your existing values are being kept, so any rebalanced defaults will NOT apply. "
							+ "Delete the file to regenerate it with the new numbers.",
							parsed.configVersion, CURRENT_VERSION);
					}

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
