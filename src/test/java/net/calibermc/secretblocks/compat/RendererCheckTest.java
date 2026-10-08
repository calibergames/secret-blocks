package net.calibermc.secretblocks.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RendererCheckTest {

	@Test
	void startsWithoutSodium() {
		assertNull(RendererCheck.problem(null, false));
		assertNull(RendererCheck.problem(null, true));
	}

	@Test
	void startsWithSodiumAndIndium() {
		assertNull(RendererCheck.problem("0.4.4+build.18", true));
	}

	@Test
	void refusesOldSodiumWithoutIndium() {
		String problem = RendererCheck.problem("0.4.4+build.18", false);
		assertNotNull(problem);
		assertTrue(problem.contains("Indium") && problem.contains("0.4.4+build.18"), problem);
		assertNotNull(RendererCheck.problem("0.5.11+mc1.20.4", false));
	}

	@Test
	void startsWithSodiumThatHasTheApiBuiltIn() {
		assertNull(RendererCheck.problem("0.6.0+mc1.21.1", false));
		assertNull(RendererCheck.problem("0.7.2+mc1.21.11", false));
	}

	@Test
	void anUnreadableSodiumVersionAsksForIndium() {
		assertNotNull(RendererCheck.problem("not a version", false));
	}
}
