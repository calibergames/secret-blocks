package net.calibermc.secretblocks.render;

import org.jetbrains.annotations.Nullable;

/**
 * Faces beside Ghost Blocks (see {@link SecretCulling}). A Ghost Block can be
 * walked into, so from inside one the faces around it must be there, as
 * around a pocket of air:
 * <ul>
 * <li>a face of any other block towards a Ghost Block is always drawn. It
 * faces into the Ghost Block, so from outside it is never seen;</li>
 * <li>Ghost Blocks never draw faces towards each other, so from inside a row
 * of them the next block that is not one shows, and nothing past it.</li>
 * </ul>
 */
final class GhostRule {

	private GhostRule() {
	}

	/**
	 * Whether a face towards a neighbour is drawn, or {@code null} where Ghost
	 * Blocks have no say (the neighbour is not one).
	 */
	@Nullable
	static Boolean drawFace(boolean selfGhost, boolean neighbourGhost) {
		if (!neighbourGhost) {
			return null;
		}
		return !selfGhost;
	}
}
