package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.blocks.DoorBlock;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.blocks.TrapdoorBlock;
import net.calibermc.secretblocks.platform.Services;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Which faces beside secret blocks are drawn. A secret block can look like
 * anything (glass, a faded disguise under the Builder's Goggles), so the
 * faces it shows decide, as vanilla decides between the blocks shown:
 * <ul>
 * <li>a face where vanilla would hide it between the two blocks shown is
 * hidden (glass beside glass), whether either block is secret;</li>
 * <li>a real block's face behind a secret block that looks solid on every
 * face (stone all round) is hidden, as behind a solid block; so is a face
 * between two secret blocks of the same kind behind such a block. A block
 * with any see-through face (One Way Glass) hides nothing, since the face
 * behind it can be seen through that face;</li>
 * <li>any other face of a real block beside a secret block is drawn (the
 * look may be see-through); a secret block's own other faces follow the
 * renderer's rule.</li>
 * <li>with a connected-textures mod installed, a secret block that can be
 * seen through (One Way Glass) hides a face whose look is the same opaque
 * block as the touching face of the secret block beside it, so a wall of
 * them seen through connected glass is one clean pane, with no seams
 * between the blocks ({@link SeamRule}).</li>
 * </ul>
 * Beside a Secret Door or Trapdoor (thin panels), or with the Builder's
 * Goggles' view on, faces beside secret blocks are always drawn. Used by
 * vanilla's culling ({@code DrawSideMixin}) and by Sodium's, which does not
 * call vanilla's ({@code SodiumOcclusionMixin}).
 */
public final class SecretCulling {

	/** Continuity (Fabric, or Forge through Connector), Fusion, or ConnectedTexturesMod. */
	private static final boolean CONNECTED_TEXTURES = Services.PLATFORM.isModLoaded("continuity")
			|| Services.PLATFORM.isModLoaded("fusion") || Services.PLATFORM.isModLoaded("ctm");

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
		boolean selfSecret = self.getBlock() instanceof SecretBlock;
		boolean neighbourSecret = neighbour.getBlock() instanceof SecretBlock;
		if (!neighbourSecret) {
			if (!selfSecret || BuilderView.isActive() || panels(self)) {
				return null;
			}
			// Secret beside real: hidden where vanilla would hide the look's face beside that block.
			return DisguiseLook.shown(level, pos, side).skipRendering(neighbour, side) ? false : null;
		}
		if (BuilderView.isActive() || panels(neighbour) || (selfSecret && panels(self))) {
			return true;
		}
		BlockState theirs = DisguiseLook.shown(level, neighbourPos, side.getOpposite());
		BlockState mine = selfSecret ? DisguiseLook.shown(level, pos, side) : self;
		if (mine.skipRendering(theirs, side)) {
			return false;
		}
		if (selfSecret && SeamRule.hides(CONNECTED_TEXTURES, mine.getBlock() == theirs.getBlock(), opaque(mine),
				() -> !allSolid(level, pos))) {
			return false;
		}
		// Hidden behind it only if no face of it can be seen through (One Way Glass's glass face shows this one).
		boolean behindItsKind = !selfSecret || self.getBlock() == neighbour.getBlock();
		return !(behindItsKind && allSolid(level, neighbourPos));
	}

	/** Whether every face of the secret block at {@code pos} looks like a solid, opaque block (nothing shows through it). */
	private static boolean allSolid(BlockGetter level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof DisguisedBlockEntity secret)) {
			return true; // no disguise yet: stone all round
		}
		for (Direction face : Direction.values()) {
			if (!opaque(DisguiseLook.shown(secret, level, pos, face))) {
				return false;
			}
		}
		return true;
	}

	/** Whether a look is a solid, opaque block (stone, not glass or leaves). */
	private static boolean opaque(BlockState look) {
		return look.canOcclude() && ItemBlockRenderTypes.getChunkRenderType(look) == RenderType.solid();
	}

	/** Secret Doors and Trapdoors are drawn as thin panels, not full faces. */
	private static boolean panels(BlockState state) {
		return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapdoorBlock;
	}
}
