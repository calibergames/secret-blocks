package net.calibermc.secretblocks.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A block's tint colours in the world, as vanilla tints its model's quads. */
public final class BlockTints {

	private BlockTints() {
	}

	/**
	 * The opaque colour of tint {@code index} of {@code state} at {@code pos}
	 * (its colour away from any world for a null {@code level}): white when
	 * the block has no tint at that index.
	 */
	public static int color(BlockState state, @Nullable BlockAndTintGetter level, BlockPos pos, int index) {
		BlockTintSource source = index < 0 ? null : Minecraft.getInstance().getBlockColors().getTintSource(state, index);
		if (source == null) {
			return 0xFFFF_FFFF;
		}
		return 0xFF00_0000 | (level != null ? source.colorInWorld(state, level, pos) : source.color(state));
	}
}
