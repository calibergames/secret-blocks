package net.calibermc.secretblocks.fabric.mixin;

import net.calibermc.secretblocks.render.SecretCulling;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Face culling beside secret blocks ({@link SecretCulling}) in Fabric API's
 * own renderer (Indigo), which draws the secret blocks' model and any other
 * model that is not a plain vanilla one. Its face test sees only the two
 * block states; the block being drawn is in the render info. The result
 * still goes through Indigo's per-face cache.
 */
@Environment(EnvType.CLIENT)
@Mixin(value = BlockRenderInfo.class, remap = false)
public abstract class IndigoCullingMixin {

	@Shadow
	public BlockAndTintGetter blockView;

	@Shadow
	public BlockPos blockPos;

	@Redirect(method = "shouldDrawFace", at = @At(value = "INVOKE", remap = true,
			target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
	private boolean secretblocks$drawFace(BlockState self, BlockState neighbour, Direction side) {
		Boolean draw = SecretCulling.drawFace(self, blockView, blockPos, side, blockPos.relative(side));
		return draw != null ? draw : Block.shouldRenderFace(self, neighbour, side);
	}
}
