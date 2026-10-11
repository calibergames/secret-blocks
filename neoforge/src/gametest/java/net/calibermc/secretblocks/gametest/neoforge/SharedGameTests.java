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
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.slf4j.Logger;

import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Registers the shared game tests as vanilla and Fabric name them: test
 * {@code <class>.<method>} in lower case, structure exactly as written
 * ({@code secretblocks_gametest:empty}). Forge's own registration would turn
 * the structure into {@code <namespace>:<class>.<template>}. The classes are
 * the test mod's classes with {@link GameTest} methods, from Forge's scan.
 * Only game test servers (development, or a production server started with
 * {@code forge.gameTestServer}) register them; a game never does.
 */
final class SharedGameTests {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String PACKAGE = "net.calibermc.secretblocks.gametest.";
	private static final Set<String> KINDS = Set.of(GameTest.class.getName(), BeforeBatch.class.getName(), AfterBatch.class.getName());

	private SharedGameTests() {
	}

	private static boolean registered;

	/** Registers the tests once; both the development event and the production start-up call it. */
	static synchronized void register() {
		if (registered) {
			return;
		}
		registered = true;
		// The annotations as the class files hold them, from Forge's scan, by class
		// and method name. Their values are read from there, not through the
		// annotation getters: in production those carry SRG names, while the
		// values in the test classes keep the names they were written with.
		Map<String, Map<String, Map<String, Map<String, Object>>>> found = new TreeMap<>();
		for (ModFileScanData scan : ModList.get().getAllScanData()) {
			for (ModFileScanData.AnnotationData annotation : scan.getAnnotations()) {
				String owner = annotation.clazz().getClassName();
				String kind = annotation.annotationType().getClassName();
				if (annotation.targetType() == ElementType.METHOD && owner.startsWith(PACKAGE) && KINDS.contains(kind)) {
					String member = annotation.memberName();
					String method = member.substring(0, member.indexOf('('));
					found.computeIfAbsent(owner, key -> new TreeMap<>())
							.computeIfAbsent(method, key -> new HashMap<>())
							.put(kind, annotation.annotationData());
				}
			}
		}
		try {
			// By SRG name, which Forge maps to the running names (Mojang's in development).
			Collection<TestFunction> functions = field("f_177495_"); // TEST_FUNCTIONS
			Set<String> classNames = field("f_177496_"); // TEST_CLASS_NAMES
			Map<String, Consumer<ServerLevel>> before = field("f_177497_"); // BEFORE_BATCH_FUNCTIONS
			Map<String, Consumer<ServerLevel>> after = field("f_177498_"); // AFTER_BATCH_FUNCTIONS
			for (Map.Entry<String, Map<String, Map<String, Map<String, Object>>>> byClass : found.entrySet()) {
				Class<?> type = Class.forName(byClass.getKey(), true, SharedGameTests.class.getClassLoader());
				for (Map.Entry<String, Map<String, Map<String, Object>>> byMethod : byClass.getValue().entrySet()) {
					String name = byMethod.getKey();
					Map<String, Map<String, Object>> kinds = byMethod.getValue();
					Map<String, Object> test = kinds.get(GameTest.class.getName());
					if (test != null) {
						functions.add(function(type, type.getDeclaredMethod(name, GameTestHelper.class), test));
						classNames.add(type.getSimpleName());
					}
					Map<String, Object> beforeBatch = kinds.get(BeforeBatch.class.getName());
					if (beforeBatch != null) {
						batchHook(before, (String) beforeBatch.get("batch"), type, type.getDeclaredMethod(name, ServerLevel.class));
					}
					Map<String, Object> afterBatch = kinds.get(AfterBatch.class.getName());
					if (afterBatch != null) {
						batchHook(after, (String) afterBatch.get("batch"), type, type.getDeclaredMethod(name, ServerLevel.class));
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

	/** A test from its {@link GameTest} values; absent ones take the annotation's defaults. */
	private static TestFunction function(Class<?> type, Method method, Map<String, Object> test) {
		String name = type.getSimpleName().toLowerCase() + "." + method.getName().toLowerCase();
		Consumer<GameTestHelper> run = helper -> invoke(type, method, helper);
		return new TestFunction(value(test, "batch", "defaultBatch"), name, value(test, "template", ""),
				StructureUtils.getRotationForRotationSteps(value(test, "rotationSteps", 0)),
				value(test, "timeoutTicks", 100), value(test, "setupTicks", 0L), value(test, "required", true),
				value(test, "requiredSuccesses", 1), value(test, "attempts", 1), run);
	}

	@SuppressWarnings("unchecked")
	private static <T> T value(Map<String, Object> values, String name, T fallback) {
		Object value = values.get(name);
		return value == null ? fallback : (T) value;
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
	private static <T> T field(String srgName) throws ReflectiveOperationException {
		return (T) ObfuscationReflectionHelper.findField(GameTestRegistry.class, srgName).get(null);
	}
}
