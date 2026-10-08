package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.blocks.DoorBlock;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.blocks.TrapdoorBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Which faces beside secret blocks are drawn. A secret block can look like
 * anything (glass, a faded disguise under the Builder's Goggles), so a face
 * of any block that touches one is drawn. Between two secret blocks of the
 * same kind the faces they show decide: a face is hidden behind an opaque
 * one (stone), and where vanilla would hide it between the two blocks shown
 * (glass beside glass). Beside a Secret Door or Trapdoor, or with the
 * Builder's Goggles' view on, it is always drawn. Used by vanilla's culling ({@code DrawSideMixin}) and by
 * Sodium's, which does not call vanilla's ({@code SodiumOcclusionMixin}).
 */
@Environment(EnvType.CLIENT)
public final class SecretCulling {

	private SecretCulling() {
	}

	/**
	 * Whether the face of {@code self} at {@code pos} towards {@code side}
	 * (touching {@code neighbourPos}) is drawn, or {@code null} where secret
	 * blocks have no say (the renderer's own rule).
	 */
	@Nullable
	public static Boolean drawFace(BlockState self, BlockGetter level, BlockPos pos, Direction side, BlockPos neighbourPos) {
		BlockState neighbour = level.getBlockState(neighbourPos);
		if (!(neighbour.getBlock() instanceof SecretBlock)) {
			return null;
		}
		if (self.getBlock() != neighbour.getBlock() || BuilderView.isActive()
				|| neighbour.getBlock() instanceof DoorBlock || neighbour.getBlock() instanceof TrapdoorBlock) {
			return true;
		}
		BlockState theirs = shown(level, neighbourPos, side.getOpposite());
		if (theirs.canOcclude() && ItemBlockRenderTypes.getChunkRenderType(theirs) == RenderType.solid()) {
			return false;
		}
		return !shown(level, pos, side).skipRendering(theirs, side);
	}

	/** The block the secret block at {@code pos} shows on {@code face}: its disguise there, or stone (no data, or a partial block). */
	private static BlockState shown(BlockGetter level, BlockPos pos, Direction face) {
		if (level.getBlockEntity(pos) instanceof DisguisedBlockEntity secret) {
			BlockState state = secret.getState(face);
			if (state != null && DisguiseRenderer.drawsAsItself(state, level, pos, secret.getDisguise().getDirection(face))) {
				return state;
			}
		}
		return Blocks.STONE.defaultBlockState();
	}
}
