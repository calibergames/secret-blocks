package net.calibermc.secretblocks.fabric.mixin;

import net.calibermc.secretblocks.render.SecretCulling;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium culls faces with its own code instead of vanilla's face test, so
 * {@link VanillaCullingMixin} does not reach it: the floor under a faded block
 * (Builder's Goggles) would show the sky through it, and a face beside a
 * secret block disguised as glass would too. This applies the same rule
 * ({@link SecretCulling}) to the face test of Sodium's block renderer. A
 * pseudo mixin: without Sodium it is skipped; no build dependency on Sodium.
 */
@Environment(EnvType.CLIENT)
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext", remap = false)
public abstract class SodiumOcclusionMixin {

	@Shadow(remap = false)
	protected BlockAndTintGetter level;
	@Shadow(remap = false)
	protected BlockState state;
	@Shadow(remap = false)
	protected BlockPos pos;

	@Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
	private void secretblocks$secretFaces(Direction facing, CallbackInfoReturnable<Boolean> cir) {
		Boolean draw = SecretCulling.drawFace(state, level, pos, facing, pos.relative(facing));
		if (draw != null) {
			cir.setReturnValue(draw);
		}
	}
}
