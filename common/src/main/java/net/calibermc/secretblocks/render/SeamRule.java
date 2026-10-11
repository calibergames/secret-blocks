package net.calibermc.secretblocks.render;

import java.util.function.BooleanSupplier;

/**
 * When a face between two secret blocks is a seam to hide (see
 * {@link SecretCulling}): only with a connected-textures mod installed, only
 * where both touching faces show the same opaque block, and only on a block
 * that can be seen through (One Way Glass, a block with a glass face). A
 * solid-looking block keeps its faces, since they can be seen through a
 * see-through neighbour; see-through or cutout looks (glass, leaves) follow
 * vanilla's rule.
 */
final class SeamRule {

	private SeamRule() {
	}

	/** {@code selfSeeThrough} is asked last, and only if everything else holds (it reads every face). */
	static boolean hides(boolean connectedTextures, boolean sameBlock, boolean opaqueLook, BooleanSupplier selfSeeThrough) {
		return connectedTextures && sameBlock && opaqueLook && selfSeeThrough.getAsBoolean();
	}
}
