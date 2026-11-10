package net.calibermc.secretblocks.gametest.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A shared game test: a public, non-static method of a class listed in
 * {@link SharedGameTests#CLASSES}, taking one {@code GameTestHelper}. Every
 * test runs in the empty structure ({@code secretblocks_gametest:empty}) and
 * is named {@code secretblocks_gametest:<class>.<method>} in lower case.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTest {

	/** Ticks the test may take before it fails. */
	int maxTicks() default 100;

	/** Whether the sky stays open above the test (no barrier roof). */
	boolean skyAccess() default false;

	/**
	 * The environment the test runs in, one of {@link TestEnvironments}. Tests
	 * run in batches by environment, so a test that must not share the world
	 * with others names an environment of its own.
	 */
	String environment() default TestEnvironments.DEFAULT;
}
