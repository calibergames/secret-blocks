package net.calibermc.secretblocks.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuilderViewToggleTest {

	@Test
	void offUntilTheGogglesAreWorn() {
		BuilderViewToggle view = new BuilderViewToggle();
		assertFalse(view.showing());
		assertTrue(view.setWearing(true));
		assertTrue(view.showing());
		assertFalse(view.setWearing(true), "wearing them still changes nothing");
	}

	@Test
	void theKeySwitchesTheViewWhileWorn() {
		BuilderViewToggle view = new BuilderViewToggle();
		view.setWearing(true);
		assertTrue(view.toggle());
		assertFalse(view.showing());
		assertTrue(view.toggle());
		assertTrue(view.showing());
	}

	@Test
	void theKeyDoesNothingWithoutTheGoggles() {
		BuilderViewToggle view = new BuilderViewToggle();
		assertFalse(view.toggle());
		assertFalse(view.showing());
		view.setWearing(true);
		assertTrue(view.showing(), "a press without the goggles must not switch the view off for later");
	}

	@Test
	void puttingThemBackOnStartsWithTheViewOn() {
		BuilderViewToggle view = new BuilderViewToggle();
		view.setWearing(true);
		view.toggle();
		assertFalse(view.setWearing(false), "taking them off while the view is off changes nothing visible");
		assertTrue(view.setWearing(true));
		assertTrue(view.showing());
	}

	@Test
	void takingThemOffEndsTheView() {
		BuilderViewToggle view = new BuilderViewToggle();
		view.setWearing(true);
		assertTrue(view.setWearing(false));
		assertFalse(view.showing());
	}
}
