package it.alqu.tinman.client.mixin;

import it.alqu.tinman.client.render.FlightLean;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leans a suited flier into their dive, using vanilla's own elytra pose.
 *
 * <p>Rather than rotating the model here, this feeds the two fields the elytra lean already reads.
 * {@code AvatarRenderer.setupRotations} turns {@code isFallFlying} plus
 * {@code fallFlyingScale()} into exactly the rotation and limb pose we want, so borrowing them
 * gets the elytra look for free and stays correct if Mojang changes how that pose is built.
 *
 * <p>{@code fallFlyingScale()} is {@code clamp(t * t / 100, 0, 1)}, so writing
 * {@code t = 10 * sqrt(lean)} makes the scale come out as the lean itself — which is what lets the
 * pose ease out again on the way back to upright, where vanilla's own counter would snap to zero.
 */
@Mixin(net.minecraft.client.renderer.entity.player.AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	// The full descriptor matters: the generic hierarchy leaves three extractRenderState
	// overloads on this class, and matching by name alone would be ambiguous.
	@Inject(
		method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;"
			+ "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
		at = @At("TAIL")
	)
	private void tinman$leanIntoFlight(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		// Leave a genuine elytra flight completely alone.
		if (state.isFallFlying || !(entity instanceof Player player)) {
			return;
		}

		float lean = FlightLean.lean(player, partialTicks);

		if (lean <= 0.0F) {
			return;
		}

		state.isFallFlying = true;
		state.fallFlyingTimeInTicks = 10.0F * (float) Math.sqrt(lean);
	}
}
