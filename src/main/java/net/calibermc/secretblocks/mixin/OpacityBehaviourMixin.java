package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.Opacity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A see-through secret block ({@link Opacity}) blocks no light, casts no
 * corner shadow and hides nothing behind it, as glass: renderers that judge
 * light and shade by whether a block is solid (Sodium) see it as glass too.
 */
@Mixin(BlockBehaviour.class)
public abstract class OpacityBehaviourMixin {

	@Inject(method = "getLightBlock", at = @At("HEAD"), cancellable = true)
	private void secretblocks$lightBlock(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (Opacity.seeThrough(state)) {
			cir.setReturnValue(0);
		}
	}

	@Inject(method = "getOcclusionShape", at = @At("HEAD"), cancellable = true)
	private void secretblocks$occlusion(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
		if (Opacity.seeThrough(state)) {
			cir.setReturnValue(Shapes.empty());
		}
	}

	@Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
	private void secretblocks$shade(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		if (Opacity.seeThrough(state)) {
			cir.setReturnValue(1.0F);
		}
	}
}
