package net.calibermc.secretblocks.render;

/**
 * Whether the Builder's Goggles' view shows: while the goggles are worn,
 * unless the player switched it off with the toggle key. Putting the
 * goggles on always starts with the view on. No Minecraft classes, so plain
 * tests can check it.
 */
public final class BuilderViewToggle {

	private boolean wearing;
	private boolean hidden;

	/** The goggles are worn or not. Returns whether the view changed. */
	public boolean setWearing(boolean now) {
		if (now == wearing) {
			return false;
		}
		boolean was = showing();
		wearing = now;
		if (now) {
			hidden = false;
		}
		return showing() != was;
	}

	/** The toggle key was pressed: switches the view only while the goggles are worn. Returns whether it changed. */
	public boolean toggle() {
		if (!wearing) {
			return false;
		}
		hidden = !hidden;
		return true;
	}

	public boolean showing() {
		return wearing && !hidden;
	}
}
