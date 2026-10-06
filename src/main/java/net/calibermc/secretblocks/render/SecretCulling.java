package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.blocks.DoorBlock;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.blocks.TrapdoorBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Which faces beside secret blocks are drawn: a secret block can look like
 * anything (glass, a faded disguise under the Builder's Goggles), so a face
 * that touches one is always drawn, except between two of the same secret
 * block; a secret block draws its faces beside a Secret Door or Trapdoor.
 * Used by vanilla's culling ({@code DrawSideMixin}) and by Sodium's, which
 * does not call vanilla's ({@code SodiumOcclusionMixin}).
 */
@Environment(EnvType.CLIENT)
public final class SecretCulling {

	private SecretCulling() {
	}

	/**
	 * Whether the face of {@code self} towards {@code neighbour} is drawn, or
	 * {@code null} where secret blocks have no say (the renderer's own rule).
	 */
	@Nullable
	public static Boolean drawFace(BlockState self, BlockState neighbour) {
		Boolean draw = null;
		if (neighbour.getBlock() instanceof SecretBlock) {
			draw = true;
		}
		if (self.getBlock() == neighbour.getBlock() && self.getBlock() instanceof SecretBlock) {
			draw = false;
		}
		if (self.getBlock() instanceof SecretBlock && (neighbour.getBlock() instanceof DoorBlock || neighbour.getBlock() instanceof TrapdoorBlock)) {
			draw = true;
		}
		return draw;
	}
}
