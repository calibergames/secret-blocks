package net.calibermc.secretblocks.gametest.neoforge;

import net.calibermc.secretblocks.gametest.support.TestBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;

/**
 * The NeoForge/Forge test mod: registers the blocks only the game tests use,
 * and the shared game tests themselves ({@link SharedGameTests}).
 */
@Mod("secretblocks_gametest")
public class NeoForgeTestMod {

	/** Where to write a JUnit report of the run, as Fabric's {@code fabric-api.gametest.report-file}. */
	private static final String REPORT_PROPERTY = "secretblocks.gametest.report";

	public NeoForgeTestMod() {
		String report = System.getProperty(REPORT_PROPERTY);
		if (report != null) {
			try {
				GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new File(report)));
			} catch (ParserConfigurationException e) {
				throw new IllegalStateException("Cannot write the game test report " + report, e);
			}
		}
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener((RegisterEvent event) -> event.register(Registries.BLOCK,
				helper -> helper.register(TestBlocks.TEST_GATE_OWNER_ID, TestBlocks.createTestGateOwner())));
		modBus.addListener((RegisterGameTestsEvent event) -> SharedGameTests.register());
		// Outside production only, Forge fires RegisterGameTestsEvent and ticks the
		// running tests. A production server started as a game test server
		// (forge.gameTestServer) still runs whatever is registered, so there the
		// tests register themselves and the tests are ticked once per server tick.
		if (FMLLoader.isProduction() && Boolean.getBoolean("forge.gameTestServer")) {
			modBus.addListener((FMLCommonSetupEvent event) -> SharedGameTests.register());
			MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
				if (event.phase == TickEvent.Phase.END) {
					GameTestTicker.SINGLETON.tick();
				}
			});
		}
		if (FMLEnvironment.dist.isClient()) {
			NeoForgeDevClient.init();
		}
	}
}
