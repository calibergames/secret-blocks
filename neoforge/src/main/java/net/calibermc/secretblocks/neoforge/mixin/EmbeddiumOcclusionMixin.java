package net.calibermc.secretblocks.neoforge.mixin;

import net.calibermc.secretblocks.render.SecretCulling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Embeddium (Sodium 0.5's port) culls faces with its own code instead of
 * vanilla's {@code Block.shouldRenderFace}, so the culling mixin there does
 * not reach it: in the Builder's Goggles' view the faces between secret
 * blocks were culled and the faded blocks showed the world behind them. This
 * applies the same rule ({@link SecretCulling}) to its
 * {@code BlockOcclusionCache}. A pseudo mixin, applied only when Embeddium is
 * installed; no build dependency on Embeddium.
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache")
public abstract class EmbeddiumOcclusionMixin {

	@Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
	private void secretblocks$secretFaces(BlockState self, BlockGetter view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
		Boolean draw = SecretCulling.drawFace(self, view, pos, facing, pos.relative(facing));
		if (draw != null) {
			cir.setReturnValue(draw);
		}
	}
}
