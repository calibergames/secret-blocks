package net.calibermc.secretblocks.gametest.neoforge;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.calibermc.secretblocks.gametest.support.SharedGameTests;
import net.calibermc.secretblocks.gametest.support.TestBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.RegisterEvent;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;

/**
 * The NeoForge test mod: registers the blocks only the game tests use,
 * and the shared game tests' functions and environment type ({@link SharedGameTests}).
 */
@Mod("secretblocks_gametest")
public class NeoForgeTestMod {

	/** Where to write a JUnit report of the run, as Fabric's {@code fabric-api.gametest.report-file}. */
	private static final String REPORT_PROPERTY = "secretblocks.gametest.report";

	public NeoForgeTestMod(IEventBus modBus) {
		String report = System.getProperty(REPORT_PROPERTY);
		if (report != null) {
			try {
				GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new File(report)));
			} catch (ParserConfigurationException e) {
				throw new IllegalStateException("Cannot write the game test report " + report, e);
			}
		}
		modBus.addListener((RegisterEvent event) -> event.register(Registries.BLOCK,
				helper -> helper.register(TestBlocks.TEST_GATE_OWNER_ID, TestBlocks.createTestGateOwner())));
		modBus.addListener((RegisterEvent event) -> {
			event.register(Registries.TEST_FUNCTION, helper -> SharedGameTests.registerFunctions(helper::register));
			event.register(Registries.TEST_ENVIRONMENT_DEFINITION_TYPE,
					helper -> helper.register(SharedGameTests.ENVIRONMENT_TYPE, SharedGameTests.ENVIRONMENT_CODEC));
		});
		// Outside production only, the loader ticks the running tests. A production
		// server started as a game test server still runs them (they come with the
		// data-pack registries), so there they are ticked once per server tick.
		if (FMLLoader.getCurrent().isProduction() && Boolean.getBoolean("neoforge.gameTestServer")) {
			NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> GameTestTicker.SINGLETON.tick());
		}
		if (FMLEnvironment.getDist().isClient()) {
			NeoForgeDevClient.init();
		}
	}
}
