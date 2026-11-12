package net.calibermc.secretblocks.forge.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A secret block's appearance on a side is that face's look, as the shared
 * {@code AppearanceMixin} gives it on Fabric and NeoForge. Forge's
 * {@code IForgeBlock#getAppearance}, which connected-texture mods ask about
 * each neighbour, takes the client's {@link BlockAndTintGetter}; added to
 * {@link Block} itself, this overrides Forge's default (the block's own state)
 * for every block, and any other block still answers its own state.
 */
@Mixin(Block.class)
public abstract class ForgeAppearanceMixin {

	public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side,
			BlockState queryState, BlockPos queryPos) {
		return DisguiseLook.looks(level, pos, state, side);
	}
}
