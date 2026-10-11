package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A secret block's appearance on a side is that face's look. Fabric API
 * ({@code FabricBlock}) and Forge ({@code IForgeBlock}) both give every block a
 * {@code getAppearance} method of this signature, which connected-texture mods
 * (Continuity, Fusion) ask about each neighbour: so real glass joins secret
 * glass beside it, and secret glass joins its neighbours. Added to
 * {@link Block} itself, it overrides the loader's default (the block's own
 * state) for every block; any other block still answers its own state.
 */
@Mixin(Block.class)
public abstract class AppearanceMixin {

	public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side,
			BlockState queryState, BlockPos queryPos) {
		return DisguiseLook.looks(level, pos, state, side);
	}
}
