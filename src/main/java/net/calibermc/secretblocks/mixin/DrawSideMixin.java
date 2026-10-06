package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.render.SecretCulling;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla's face culling beside secret blocks: see {@link SecretCulling}. */
@Environment(EnvType.CLIENT)
@Mixin(Block.class)
public class DrawSideMixin {

	@Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
	private static void TSRshouldDrawSide(BlockState state, BlockGetter world, BlockPos pos, Direction side, BlockPos blockPos, CallbackInfoReturnable<Boolean> ci) {
		Boolean draw = SecretCulling.drawFace(state, world.getBlockState(blockPos));
		if (draw != null) {
			ci.setReturnValue(draw);
		}
	}
}
