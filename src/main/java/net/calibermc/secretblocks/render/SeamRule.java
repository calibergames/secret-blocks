package net.calibermc.secretblocks.render;

import java.util.function.BooleanSupplier;

/**
 * When a face between two secret blocks is a seam to hide (see
 * {@link SecretCulling}): only where both touching faces look solid, and only
 * on a block that can be seen through (One Way Glass, a block with a glass
 * face), whose solid faces are the inside of the window. A solid-looking
 * block keeps its faces, since they are the wall seen through a see-through
 * neighbour; a face beside a see-through look (glass, leaves) shows through
 * it and is kept.
 */
final class SeamRule {

	private SeamRule() {
	}

	/** {@code selfSeeThrough} is asked last, and only if everything else holds (it reads every face). */
	static boolean hides(boolean mineSolid, boolean theirsSolid, BooleanSupplier selfSeeThrough) {
		return mineSolid && theirsSolid && selfSeeThrough.getAsBoolean();
	}
}
