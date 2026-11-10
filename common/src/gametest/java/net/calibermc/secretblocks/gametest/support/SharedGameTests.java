package net.calibermc.secretblocks.gametest.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import net.calibermc.secretblocks.gametest.ApprovedPlacementGameTests;
import net.calibermc.secretblocks.gametest.BehaviourGameTests;
import net.calibermc.secretblocks.gametest.BuildersGogglesGameTests;
import net.calibermc.secretblocks.gametest.ClamberGameTests;
import net.calibermc.secretblocks.gametest.ComparatorGameTests;
import net.calibermc.secretblocks.gametest.ConductorGameTests;
import net.calibermc.secretblocks.gametest.ContainerGameTests;
import net.calibermc.secretblocks.gametest.CounterpartGameTests;
import net.calibermc.secretblocks.gametest.DaylightDetectorGameTests;
import net.calibermc.secretblocks.gametest.DisguiseLookGameTests;
import net.calibermc.secretblocks.gametest.DispenserGameTests;
import net.calibermc.secretblocks.gametest.DoorHalvesGameTests;
import net.calibermc.secretblocks.gametest.DropperGameTests;
import net.calibermc.secretblocks.gametest.GateGameTests;
import net.calibermc.secretblocks.gametest.GateSegmentGameTests;
import net.calibermc.secretblocks.gametest.GogglesGameTests;
import net.calibermc.secretblocks.gametest.HopperGameTests;
import net.calibermc.secretblocks.gametest.IronDoorGameTests;
import net.calibermc.secretblocks.gametest.LightGameTests;
import net.calibermc.secretblocks.gametest.NbtGameTests;
import net.calibermc.secretblocks.gametest.ObserverGameTests;
import net.calibermc.secretblocks.gametest.PistonGameTests;
import net.calibermc.secretblocks.gametest.PlacementGameTests;
import net.calibermc.secretblocks.gametest.PressurePlateGameTests;
import net.calibermc.secretblocks.gametest.ProbeGameTests;
import net.calibermc.secretblocks.gametest.PushableGameTests;
import net.calibermc.secretblocks.gametest.RecipeGameTests;
import net.calibermc.secretblocks.gametest.RedstoneGameTests;
import net.calibermc.secretblocks.gametest.RedstoneWireGameTests;
import net.calibermc.secretblocks.gametest.RepeaterGameTests;
import net.calibermc.secretblocks.gametest.SculkSensorGameTests;
import net.calibermc.secretblocks.gametest.SyncGameTests;
import net.calibermc.secretblocks.gametest.TorchLeverGameTests;
import net.calibermc.secretblocks.gametest.TrappedChestGameTests;
import net.calibermc.secretblocks.gametest.WandConversionGameTests;
import net.calibermc.secretblocks.gametest.WandGameTests;
import net.calibermc.secretblocks.gametest.WeightedPlateGameTests;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * The shared game tests, registered the same way on every loader, in
 * development and on production game test servers:
 * <ul>
 * <li>each loader's test mod registers the test functions and the
 * {@link SkyEnvironment} type in their built-in registries
 * ({@link #registerFunctions}, {@link #ENVIRONMENT_TYPE});</li>
 * <li>while the data-pack registries load, the environments and the test
 * instances join the ones read from data ({@link #registerEnvironments},
 * {@link #registerInstances}).</li>
 * </ul>
 */
public final class SharedGameTests {

	public static final String NAMESPACE = "secretblocks_gametest";
	public static final ResourceLocation ENVIRONMENT_TYPE = id("sky");
	public static final MapCodec<SkyEnvironment> ENVIRONMENT_CODEC = SkyEnvironment.CODEC;
	/**
	 * An 8 x 8 x 8 test area of air, from this test mod's data (the same size
	 * as Fabric's own empty structure), so every loader runs the tests in the
	 * same space. The test framework walls each test in with barriers and
	 * roofs it over too, so a test that builds or shoots above the area sets
	 * {@link GameTest#skyAccess}.
	 */
	private static final ResourceLocation STRUCTURE = id("empty");

	/** Every class with shared tests. */
	public static final List<Class<?>> CLASSES = List.of(
			PlacementGameTests.class, BehaviourGameTests.class, NbtGameTests.class, ProbeGameTests.class,
			ApprovedPlacementGameTests.class, SyncGameTests.class, PressurePlateGameTests.class, ObserverGameTests.class,
			ClamberGameTests.class, DispenserGameTests.class, HopperGameTests.class, GogglesGameTests.class,
			DisguiseLookGameTests.class, IronDoorGameTests.class, RedstoneGameTests.class, RecipeGameTests.class,
			CounterpartGameTests.class, ContainerGameTests.class, RedstoneWireGameTests.class, RepeaterGameTests.class,
			ComparatorGameTests.class, TrappedChestGameTests.class, TorchLeverGameTests.class,
			DaylightDetectorGameTests.class, DropperGameTests.class, SculkSensorGameTests.class,
			GateSegmentGameTests.class, GateGameTests.class, PistonGameTests.class, PushableGameTests.class,
			BuildersGogglesGameTests.class, DoorHalvesGameTests.class, ConductorGameTests.class, WandGameTests.class,
			WandConversionGameTests.class, WeightedPlateGameTests.class, LightGameTests.class);

	/** One test: its id, its settings and how to run it. */
	public record Test(ResourceLocation id, GameTest settings, Consumer<GameTestHelper> function) {
	}

	private static List<Test> tests;

	private SharedGameTests() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
	}

	/** Every test, by id. Each class is made once; its test methods run on that instance. */
	public static synchronized List<Test> tests() {
		if (tests == null) {
			Map<ResourceLocation, Test> found = new TreeMap<>();
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			for (Class<?> type : CLASSES) {
				Object instance = newInstance(type);
				for (Method method : type.getDeclaredMethods()) {
					GameTest settings = method.getAnnotation(GameTest.class);
					if (settings == null) {
						continue;
					}
					check(method);
					ResourceLocation id = id(type.getSimpleName().toLowerCase(Locale.ROOT) + "." + method.getName().toLowerCase(Locale.ROOT));
					MethodHandle handle;
					try {
						handle = lookup.unreflect(method).bindTo(instance);
					} catch (IllegalAccessException e) {
						throw new IllegalStateException("Cannot reach test " + id, e);
					}
					MethodHandle run = handle;
					Test test = new Test(id, settings, helper -> invoke(run, helper));
					if (found.put(id, test) != null) {
						throw new IllegalStateException("Two tests are named " + id);
					}
				}
			}
			tests = List.copyOf(found.values());
		}
		return tests;
	}

	/** Registers each test's function, under the test's id. */
	public static void registerFunctions(BiConsumer<ResourceLocation, Consumer<GameTestHelper>> register) {
		for (Test test : tests()) {
			register.accept(test.id(), test.function());
		}
	}

	/** Adds the environments the tests name to the test environment registry as it loads. */
	public static void registerEnvironments(WritableRegistry<TestEnvironmentDefinition> registry) {
		tests().stream().map(test -> test.settings().environment()).distinct().sorted(Comparator.naturalOrder())
				.forEach(name -> registry.register(ResourceKey.create(Registries.TEST_ENVIRONMENT, id(name)),
						TestEnvironments.sky(name), RegistrationInfo.BUILT_IN));
	}

	/** Adds the tests to the test instance registry as it loads, each in its environment. */
	public static void registerInstances(WritableRegistry<GameTestInstance> registry, HolderGetter<TestEnvironmentDefinition> environments) {
		for (Test test : tests()) {
			GameTest settings = test.settings();
			Holder<TestEnvironmentDefinition> environment = environments.getOrThrow(
					ResourceKey.create(Registries.TEST_ENVIRONMENT, id(settings.environment())));
			TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(environment, STRUCTURE, settings.maxTicks(), 0, true,
					net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, settings.skyAccess());
			registry.register(ResourceKey.create(Registries.TEST_INSTANCE, test.id()),
					new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, test.id()), data), RegistrationInfo.BUILT_IN);
		}
	}

	private static void check(Method method) {
		int modifiers = method.getModifiers();
		if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers) || method.getReturnType() != void.class
				|| method.getParameterCount() != 1 || method.getParameterTypes()[0] != GameTestHelper.class) {
			throw new IllegalStateException("A game test must be public, not static, return void and take one GameTestHelper: "
					+ method.getDeclaringClass().getName() + "#" + method.getName());
		}
	}

	private static Object newInstance(Class<?> type) {
		try {
			return type.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot make " + type.getName(), e);
		}
	}

	/** Runs a test method; what it throws (a failed check above all) reaches the test framework as it is. */
	private static void invoke(MethodHandle run, GameTestHelper helper) {
		try {
			run.invoke(helper);
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException(e);
		}
	}
}
