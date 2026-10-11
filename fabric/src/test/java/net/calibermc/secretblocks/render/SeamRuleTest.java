package net.calibermc.secretblocks.render;

import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The seam between two secret blocks is hidden only in the one case it is a seam. */
class SeamRuleTest {

	private static final BooleanSupplier SEE_THROUGH = () -> true;
	private static final BooleanSupplier SOLID = () -> false;

	@Test
	void hidesTheSeamOfASeeThroughBlockWithConnectedTextures() {
		assertTrue(SeamRule.hides(true, true, true, SEE_THROUGH));
	}

	@Test
	void keepsEveryFaceWithoutConnectedTextures() {
		assertFalse(SeamRule.hides(false, true, true, SEE_THROUGH));
	}

	@Test
	void keepsTheFacesOfASolidLookingBlock() {
		// Seen through a One Way Glass window beside it: hiding them would show its inside.
		assertFalse(SeamRule.hides(true, true, true, SOLID));
	}

	@Test
	void keepsFacesWhoseLooksDifferOrAreNotOpaque() {
		assertFalse(SeamRule.hides(true, false, true, SEE_THROUGH));
		assertFalse(SeamRule.hides(true, true, false, SEE_THROUGH));
	}

	@Test
	void readsTheBlockOnlyWhenEverythingElseHolds() {
		BooleanSupplier unexpected = () -> {
			throw new AssertionError("the block's faces were read for a face that is drawn anyway");
		};
		assertFalse(SeamRule.hides(false, true, true, unexpected));
		assertFalse(SeamRule.hides(true, false, true, unexpected));
		assertFalse(SeamRule.hides(true, true, false, unexpected));
	}
}
