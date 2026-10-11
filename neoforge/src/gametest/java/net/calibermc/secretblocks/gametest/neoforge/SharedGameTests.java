package net.calibermc.secretblocks.gametest.neoforge;

import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Registers the shared game tests as vanilla and Fabric name them: test
 * {@code <class>.<method>} in lower case, structure exactly as written
 * ({@code secretblocks_gametest:empty}). Forge's own registration would turn
 * the structure into {@code <namespace>:<class>.<template>}. The classes are
 * the test mod's classes with {@link GameTest} methods, from Forge's scan.
 * Development only: the game test server never runs in a production game.
 */
final class SharedGameTests {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String PACKAGE = "net.calibermc.secretblocks.gametest.";

	private SharedGameTests() {
	}

	static void register() {
		Set<String> classes = new TreeSet<>();
		for (ModFileScanData scan : ModList.get().getAllScanData()) {
			for (ModFileScanData.AnnotationData annotation : scan.getAnnotations()) {
				String owner = annotation.clazz().getClassName();
				if (annotation.annotationType().getClassName().equals(GameTest.class.getName()) && owner.startsWith(PACKAGE)) {
					classes.add(owner);
				}
			}
		}
		try {
			Collection<TestFunction> functions = field("TEST_FUNCTIONS");
			Set<String> classNames = field("TEST_CLASS_NAMES");
			Map<String, Consumer<ServerLevel>> before = field("BEFORE_BATCH_FUNCTIONS");
			Map<String, Consumer<ServerLevel>> after = field("AFTER_BATCH_FUNCTIONS");
			for (String name : classes) {
				Class<?> type = Class.forName(name, true, SharedGameTests.class.getClassLoader());
				Method[] methods = type.getDeclaredMethods();
				Arrays.sort(methods, Comparator.comparing(Method::getName));
				for (Method method : methods) {
					GameTest test = method.getAnnotation(GameTest.class);
					if (test != null) {
						functions.add(function(type, method, test));
						classNames.add(type.getSimpleName());
					}
					BeforeBatch beforeBatch = method.getAnnotation(BeforeBatch.class);
					if (beforeBatch != null) {
						batchHook(before, beforeBatch.batch(), type, method);
					}
					AfterBatch afterBatch = method.getAnnotation(AfterBatch.class);
					if (afterBatch != null) {
						batchHook(after, afterBatch.batch(), type, method);
					}
				}
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not register the shared game tests", e);
		}
	}

	/** One hook per batch, as vanilla requires. */
	private static void batchHook(Map<String, Consumer<ServerLevel>> hooks, String batch, Class<?> type, Method method) {
		if (hooks.putIfAbsent(batch, level -> invoke(type, method, level)) != null) {
			throw new IllegalStateException("Batch '" + batch + "' has more than one hook of one kind");
		}
	}

	private static TestFunction function(Class<?> type, Method method, GameTest test) {
		String name = type.getSimpleName().toLowerCase() + "." + method.getName().toLowerCase();
		Consumer<GameTestHelper> run = helper -> invoke(type, method, helper);
		return new TestFunction(test.batch(), name, test.template(), StructureUtils.getRotationForRotationSteps(test.rotationSteps()),
				test.timeoutTicks(), test.setupTicks(), test.required(), test.requiredSuccesses(), test.attempts(), run);
	}

	private static void invoke(Class<?> type, Method method, Object argument) {
		try {
			Object instance = Modifier.isStatic(method.getModifiers()) ? null : type.getDeclaredConstructor().newInstance();
			method.setAccessible(true);
			method.invoke(instance, argument);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			// The test log keeps only the message; anything but a failed check gets its trace.
			if (!(cause instanceof GameTestAssertException)) {
				LOGGER.error("{}.{} threw", type.getSimpleName(), method.getName(), cause);
			}
			if (cause instanceof RuntimeException runtime) {
				throw runtime;
			}
			throw new RuntimeException(cause);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T field(String name) throws ReflectiveOperationException {
		Field field = GameTestRegistry.class.getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(null);
	}
}
