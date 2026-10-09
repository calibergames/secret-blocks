package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A break event that names a secret block itself (not its look) sounds like
 * the look of its top while the client still has the block. Breaks by a
 * player or the world already name the look.
 */
@Mixin(LevelRenderer.class)
public abstract class BreakEventLookMixin {

	@Shadow
	private ClientLevel level;

	@ModifyVariable(method = "levelEvent", at = @At("STORE"))
	private BlockState secretblocks$breakLook(BlockState state, int type, BlockPos pos, int data) {
		return DisguiseLook.looks(level, pos, state, Direction.UP);
	}
}
