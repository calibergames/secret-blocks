package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * A secret block resists a blast as the look of its face toward the blast
 * does ({@link DisguiseLook#explosionResistance}), and as much as any fluid
 * in it. Entity-based explosions (a wither skull, for one) go through here
 * too before adjusting the result. A call with no explosion (from another
 * mod) gets vanilla's answer.
 */
@Mixin(ExplosionDamageCalculator.class)
public abstract class DisguiseBlastMixin {

	@Inject(method = "getBlockExplosionResistance", at = @At("HEAD"), cancellable = true)
	private void secretblocks$disguiseResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state,
			FluidState fluid, CallbackInfoReturnable<Optional<Float>> cir) {
		if (state.getBlock() instanceof SecretBlock && explosion != null) {
			ExplosionAccessor center = (ExplosionAccessor) explosion;
			float resistance = DisguiseLook.explosionResistance(level, pos,
					new Vec3(center.secretblocks$getX(), center.secretblocks$getY(), center.secretblocks$getZ()));
			cir.setReturnValue(Optional.of(Math.max(resistance, fluid.getExplosionResistance())));
		}
	}
}
