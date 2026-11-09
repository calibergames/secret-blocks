package net.calibermc.secretblocks.gametest.neoforge;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.calibermc.secretblocks.gametest.support.TestBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.RegisterEvent;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;

/**
 * The NeoForge test mod: registers the blocks only the game tests use,
 * and the shared game tests themselves ({@link SharedGameTests}).
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
		modBus.addListener((RegisterGameTestsEvent event) -> SharedGameTests.register());
		// Outside production only, NeoForge fires RegisterGameTestsEvent and ticks the
		// running tests. A production server started as a game test server
		// (neoforge.gameTestServer) still runs whatever is registered, so there the
		// tests register themselves and are ticked once per server tick.
		if (FMLLoader.isProduction() && Boolean.getBoolean("neoforge.gameTestServer")) {
			modBus.addListener((FMLCommonSetupEvent event) -> SharedGameTests.register());
			NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> GameTestTicker.SINGLETON.tick());
		}
		if (FMLEnvironment.dist.isClient()) {
			NeoForgeDevClient.init();
		}
	}
}
