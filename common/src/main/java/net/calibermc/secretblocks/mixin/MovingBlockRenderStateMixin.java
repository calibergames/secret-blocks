package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.render.MovingBlockSource;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Lets a moving block's render state carry the piston move it shows ({@link MovingBlockSource}). */
@Mixin(MovingBlockRenderState.class)
public abstract class MovingBlockRenderStateMixin implements MovingBlockSource {

	@Unique
	@Nullable
	private PistonMovingBlockEntity secretblocks$moving;

	@Override
	@Nullable
	public PistonMovingBlockEntity secretblocks$moving() {
		return secretblocks$moving;
	}

	@Override
	public void secretblocks$setMoving(@Nullable PistonMovingBlockEntity moving) {
		secretblocks$moving = moving;
	}
}
