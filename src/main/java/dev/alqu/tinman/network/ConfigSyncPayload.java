package dev.alqu.tinman.network;

import dev.alqu.tinman.TinMan;
import dev.alqu.tinman.config.TinManConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The server's gameplay numbers, sent to each client as it joins.
 *
 * <p>The client draws battery bars, the suit HUD and every weapon tooltip from config. Left to its
 * own {@code config/tinman.json} those numbers are whatever that player happens to have on disk,
 * which on a server is routinely not what the server is actually running — a battery bar scaled to
 * the wrong capacity, tooltips quoting damage the server never deals. Only values the client
 * displays or measures against travel here; purely local presentation settings, such as the flight
 * lean, stay the player's own.
 */
public record ConfigSyncPayload(
	int batteryCapacity,
	double conservationPerLevel,
	boolean flightEnabled,
	boolean unibeamEnabled,
	double pulseDamage,
	double chargedPulseDamage,
	int pulseEnergyCost,
	int chargedPulseEnergyCost,
	boolean chargedShotEnabled,
	double bladeEnergyBonusDamage,
	int bladeEnergyCostPerHit
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ConfigSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(TinMan.id("config_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ConfigSyncPayload::batteryCapacity,
		ByteBufCodecs.DOUBLE, ConfigSyncPayload::conservationPerLevel,
		ByteBufCodecs.BOOL, ConfigSyncPayload::flightEnabled,
		ByteBufCodecs.BOOL, ConfigSyncPayload::unibeamEnabled,
		ByteBufCodecs.DOUBLE, ConfigSyncPayload::pulseDamage,
		ByteBufCodecs.DOUBLE, ConfigSyncPayload::chargedPulseDamage,
		ByteBufCodecs.VAR_INT, ConfigSyncPayload::pulseEnergyCost,
		ByteBufCodecs.VAR_INT, ConfigSyncPayload::chargedPulseEnergyCost,
		ByteBufCodecs.BOOL, ConfigSyncPayload::chargedShotEnabled,
		ByteBufCodecs.DOUBLE, ConfigSyncPayload::bladeEnergyBonusDamage,
		ByteBufCodecs.VAR_INT, ConfigSyncPayload::bladeEnergyCostPerHit,
		ConfigSyncPayload::new
	);

	/** Snapshot of whatever this side is currently running. */
	public static ConfigSyncPayload of(TinManConfig config) {
		return new ConfigSyncPayload(
			config.suit.batteryCapacity,
			config.suit.conservationPerLevel,
			config.suit.flightEnabled,
			config.suit.unibeamEnabled,
			config.weapons.pulseDamage,
			config.weapons.chargedPulseDamage,
			config.weapons.pulseEnergyCost,
			config.weapons.chargedPulseEnergyCost,
			config.weapons.chargedShotEnabled,
			config.weapons.bladeEnergyBonusDamage,
			config.weapons.bladeEnergyCostPerHit
		);
	}

	@Override
	public CustomPacketPayload.Type<ConfigSyncPayload> type() {
		return TYPE;
	}
}
