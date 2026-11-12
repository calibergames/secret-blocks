package net.calibermc.secretblocks.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** From inside a Ghost Block the blocks around it show their faces; Ghost Blocks are seen through. */
class GhostRuleTest {

	@Test
	void aBlockBesideAGhostBlockDrawsItsFaceTowardsIt() {
		// Hidden, the face would leave a hole from inside the Ghost Block into whatever is behind.
		assertEquals(Boolean.TRUE, GhostRule.drawFace(false, true));
	}

	@Test
	void ghostBlocksDrawNoFacesTowardsEachOther() {
		assertEquals(Boolean.FALSE, GhostRule.drawFace(true, true));
	}

	@Test
	void hasNoSayBesideOtherBlocks() {
		assertNull(GhostRule.drawFace(true, false));
		assertNull(GhostRule.drawFace(false, false));
	}
}
