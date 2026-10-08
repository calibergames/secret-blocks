package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.render.SecretCulling;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium culls faces with its own code instead of vanilla's
 * {@code Block.shouldRenderFace}, so {@link DrawSideMixin} does not reach it:
 * the floor under a faded block (Builder's Goggles) showed the sky through
 * it, and a face beside a secret block disguised as glass would too. This
 * applies the same rule ({@link SecretCulling}) to Sodium 0.4's
 * {@code BlockOcclusionCache}. A pseudo mixin: skipped when Sodium is not
 * installed; no build dependency on Sodium.
 */
@Environment(EnvType.CLIENT)
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache")
public abstract class SodiumOcclusionMixin {

	@Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
	private void secretblocks$secretFaces(BlockState self, BlockGetter view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
		Boolean draw = SecretCulling.drawFace(self, view, pos, facing, pos.relative(facing));
		if (draw != null) {
			cir.setReturnValue(draw);
		}
	}
}
