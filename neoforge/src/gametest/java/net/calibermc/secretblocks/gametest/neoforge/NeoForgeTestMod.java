package net.calibermc.secretblocks.gametest.neoforge;

import net.calibermc.secretblocks.gametest.support.TestBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

/**
 * The NeoForge/Forge test mod: registers the blocks only the game tests use,
 * and the shared game tests themselves ({@link SharedGameTests}).
 */
@Mod("secretblocks_gametest")
public class NeoForgeTestMod {

	public NeoForgeTestMod() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener((RegisterEvent event) -> event.register(Registries.BLOCK,
				helper -> helper.register(TestBlocks.TEST_GATE_OWNER_ID, TestBlocks.createTestGateOwner())));
		modBus.addListener((RegisterGameTestsEvent event) -> SharedGameTests.register());
	}
}
