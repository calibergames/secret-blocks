package net.calibermc.secretblocks.render;

import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The piston move a moving block's render state was made from
 * ({@code MovingBlockRenderStateMixin}): the move is drawn after the block
 * entity renderer has run, so the state carries it to the draw.
 */
public interface MovingBlockSource {

	@Nullable
	PistonMovingBlockEntity secretblocks$moving();

	void secretblocks$setMoving(@Nullable PistonMovingBlockEntity moving);
}
