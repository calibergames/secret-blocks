package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.Opacity;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * A secret block lets light through as its looks do: when the look of every
 * face lets it through (glass, leaves), it blocks none, as glass; otherwise
 * it blocks it, as stone. Layout: stone floor at y = 0.
 */
public class LightGameTests implements FabricGameTest {

	private static final BlockPos AT = new BlockPos(3, 2, 3);

	private static Map<Direction, BlockState> all(Block block) {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			faces.put(dir, block.defaultBlockState());
		}
		return faces;
	}

	private static int lightBlock(GameTestHelper h, BlockPos rel) {
		return h.getBlockState(rel).getLightBlock(h.getLevel(), h.absolutePos(rel));
	}

	/**
	 * Glass, stained glass, ice and leaves let light through (and the block
	 * no longer counts as solid to renderers); stone, tinted glass and no
	 * disguise do not; one stone face is enough to block it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lightFollowsTheLooks(GameTestHelper h) {
		floor(h);
		h.setBlock(AT, ModBlocks.SOLID_BLOCK);
		check(h, lightBlock(h, AT) == 15, "with no disguise it should block light, blocks " + lightBlock(h, AT));
		List<String> problems = new ArrayList<>();
		for (Block look : new Block[] {Blocks.GLASS, Blocks.RED_STAINED_GLASS, Blocks.ICE, Blocks.OAK_LEAVES, Blocks.STONE, Blocks.TINTED_GLASS}) {
			secret(h, AT).setDisguise(all(look));
			boolean through = look != Blocks.STONE && look != Blocks.TINTED_GLASS;
			boolean solid = h.getBlockState(AT).isSolidRender(h.getLevel(), h.absolutePos(AT));
			if ((lightBlock(h, AT) == 0) != through || h.getBlockState(AT).getValue(Opacity.OPAQUE) == through || solid == through) {
				problems.add(look + ": blocks " + lightBlock(h, AT) + ", " + h.getBlockState(AT));
			}
		}
		secret(h, AT).setDisguise(all(Blocks.GLASS));
		secret(h, AT).setState(Direction.UP, Blocks.STONE.defaultBlockState());
		if (lightBlock(h, AT) != 15) {
			problems.add("glass with a stone top blocks " + lightBlock(h, AT));
		}
		check(h, problems.isEmpty(), "light not as the looks: " + problems);
		h.succeed();
	}

	/** Every disguised secret block drawn as a full block carries the property; doors, trapdoors, One Way Glass, the Ghost Block and Clamber do not. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theRightBlocksCarryIt(GameTestHelper h) {
		List<String> problems = new ArrayList<>();
		int carriers = 0;
		for (Block block : BuiltInRegistries.BLOCK) {
			if (!(block instanceof SecretBlock) || !BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(SecretBlocks.MOD_ID)) {
				continue;
			}
			boolean has = block.defaultBlockState().hasProperty(Opacity.OPAQUE);
			boolean exempt = block == ModBlocks.DOOR_BLOCK || block == ModBlocks.IRON_DOOR_BLOCK || block == ModBlocks.TRAPDOOR_BLOCK
					|| block == ModBlocks.IRON_TRAPDOOR_BLOCK || block == ModBlocks.ONE_WAY_GLASS || block == ModBlocks.GHOST_BLOCK
					|| block == ModBlocks.SECRET_CLAMBER;
			if (has == exempt) {
				problems.add(BuiltInRegistries.BLOCK.getKey(block).getPath() + (has ? " carries it" : " lacks it"));
			}
			if (has) {
				carriers++;
				if (!block.defaultBlockState().getValue(Opacity.OPAQUE)) {
					problems.add(BuiltInRegistries.BLOCK.getKey(block).getPath() + " starts see-through");
				}
			}
		}
		check(h, carriers >= 20, "expected the full secret blocks to carry it, found " + carriers);
		check(h, problems.isEmpty(), "wrong: " + problems);
		h.succeed();
	}

	/**
	 * A glowstone lamp shines through a Dummy Block that looks like glass as
	 * through glass (13 two blocks on), and not through one that looks like
	 * stone (light has to go round it).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void lampLightShinesThroughAGlassLook(GameTestHelper h) {
		floor(h);
		BlockPos lamp = new BlockPos(1, 2, 3);
		BlockPos glass = new BlockPos(2, 2, 3);
		BlockPos stone = new BlockPos(2, 2, 6);
		h.setBlock(lamp, Blocks.GLOWSTONE);
		h.setBlock(new BlockPos(1, 2, 6), Blocks.GLOWSTONE);
		h.setBlock(glass, ModBlocks.SOLID_BLOCK);
		secret(h, glass).setDisguise(all(Blocks.GLASS));
		h.setBlock(stone, ModBlocks.SOLID_BLOCK);
		secret(h, stone).setDisguise(all(Blocks.STONE));
		h.runAfterDelay(10, () -> {
			int through = h.getLevel().getBrightness(LightLayer.BLOCK, h.absolutePos(new BlockPos(3, 2, 3)));
			int round = h.getLevel().getBrightness(LightLayer.BLOCK, h.absolutePos(new BlockPos(3, 2, 6)));
			check(h, through == 13, "behind the glass look the light should be 13, is " + through);
			check(h, round < 13, "behind the stone look the light should come round it (below 13), is " + round);
			h.succeed();
		});
	}

	/**
	 * Secret Redstone Dust that looks like glass stays see-through when its
	 * connections change, also when vanilla rebuilds its state from the
	 * default one (a change above it).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void dustKeepsItsLightWhenItConnects(GameTestHelper h) {
		floor(h);
		h.setBlock(AT.below(), Blocks.STONE);
		h.setBlock(AT, ModBlocks.SECRET_REDSTONE);
		secret(h, AT).setDisguise(all(Blocks.GLASS));
		check(h, !h.getBlockState(AT).getValue(Opacity.OPAQUE), "the glass-look dust should be see-through");
		h.setBlock(AT.east(), Blocks.REDSTONE_WIRE);
		check(h, !h.getBlockState(AT).getValue(Opacity.OPAQUE), "after connecting it should still be see-through: " + h.getBlockState(AT));
		// A change above makes vanilla rebuild the dust's state from its default one.
		h.setBlock(AT.above(), Blocks.STONE);
		check(h, !h.getBlockState(AT).getValue(Opacity.OPAQUE), "after a change above it should still be see-through: " + h.getBlockState(AT));
		h.succeed();
	}

	/** A secret block whose state disagrees with its looks (a world saved before) is put right by a check. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aWrongStateIsPutRight(GameTestHelper h) {
		floor(h);
		h.setBlock(AT, ModBlocks.SOLID_BLOCK);
		secret(h, AT).setDisguise(all(Blocks.GLASS));
		h.getLevel().setBlock(h.absolutePos(AT), h.getBlockState(AT).setValue(Opacity.OPAQUE, true), Block.UPDATE_ALL);
		check(h, h.getBlockState(AT).getValue(Opacity.OPAQUE), "the test should start from a wrong state");
		Opacity.sync(h.getLevel(), h.absolutePos(AT));
		check(h, !h.getBlockState(AT).getValue(Opacity.OPAQUE), "the check should make it see-through");
		check(h, secret(h, AT).getState(Direction.NORTH).is(Blocks.GLASS), "the disguise should stay");
		h.succeed();
	}

	private static boolean seeThrough(GameTestHelper h, BlockPos rel) {
		BlockState state = h.getBlockState(rel);
		return state.hasProperty(Opacity.OPAQUE) && !state.getValue(Opacity.OPAQUE) && lightBlock(h, rel) == 0;
	}

	/** A Secret Piston that looks like glass pushes out a see-through head and stays see-through out and back. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void aGlassLookPistonMoves(GameTestHelper h) {
		floor(h);
		BlockPos base = new BlockPos(2, 1, 3);
		h.setBlock(base, ModBlocks.SECRET_PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING, Direction.EAST));
		secret(h, base).setDisguise(all(Blocks.GLASS));
		check(h, seeThrough(h, base), "the glass-look piston should be see-through");
		h.setBlock(base.west(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(6, () -> {
			check(h, h.getBlockState(base.east()).is(ModBlocks.SECRET_PISTON_HEAD), "the piston should be out, east is " + h.getBlockState(base.east()));
			check(h, seeThrough(h, base), "the pushed-out base should stay see-through: " + h.getBlockState(base));
			check(h, seeThrough(h, base.east()), "its head should be see-through: " + h.getBlockState(base.east()));
			h.setBlock(base.west(), Blocks.AIR);
		});
		h.runAfterDelay(14, () -> {
			check(h, h.getBlockState(base.east()).isAir(), "the piston should be back, east is " + h.getBlockState(base.east()));
			check(h, seeThrough(h, base), "the retracted base should stay see-through: " + h.getBlockState(base));
			h.succeed();
		});
	}

	/** A Secret Gate that looks like glass builds see-through segments. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void aGlassLookGateBuildsSeeThroughSegments(GameTestHelper h) {
		floor(h);
		BlockPos gate = new BlockPos(1, 1, 3);
		h.setBlock(gate, ModBlocks.SECRET_GATE.defaultBlockState().setValue(net.calibermc.secretblocks.blocks.SecretGate.FACING, Direction.EAST));
		secret(h, gate).setDisguise(all(Blocks.GLASS));
		h.setBlock(gate.north(), Blocks.REDSTONE_BLOCK);
		h.succeedWhen(() -> {
			for (int i = 1; i <= 3; i++) {
				BlockPos segment = gate.east(i);
				check(h, h.getBlockState(segment).is(ModBlocks.SECRET_GATE_SEGMENT), "segment " + i + " not built yet");
				check(h, seeThrough(h, segment), "segment " + i + " should be see-through: " + h.getBlockState(segment));
			}
			check(h, seeThrough(h, gate), "the gate should be see-through");
		});
	}

	/** A Secret Hopper that looks like glass moves items as usual and stays see-through. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
	public void aGlassLookHopperMovesItems(GameTestHelper h) {
		floor(h);
		BlockPos hopper = new BlockPos(3, 2, 3);
		h.setBlock(hopper.below(), Blocks.CHEST);
		h.setBlock(hopper, ModBlocks.SECRET_HOPPER);
		secret(h, hopper).setDisguise(all(Blocks.GLASS));
		h.setBlock(hopper.above(), Blocks.CHEST);
		net.minecraft.world.Container top = (net.minecraft.world.Container) h.getBlockEntity(hopper.above());
		top.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 5));
		h.succeedWhen(() -> {
			net.minecraft.world.Container bottom = (net.minecraft.world.Container) h.getBlockEntity(hopper.below());
			check(h, bottom.countItem(net.minecraft.world.item.Items.COBBLESTONE) == 5, "all five should reach the chest below, " + bottom.countItem(net.minecraft.world.item.Items.COBBLESTONE) + " so far");
			check(h, seeThrough(h, hopper), "the hopper should stay see-through: " + h.getBlockState(hopper));
		});
	}

	/** The check run as a chunk loads notes a wrong secret block, and the end of the tick puts it right. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theChunkLoadCheckPutsItRight(GameTestHelper h) {
		floor(h);
		h.setBlock(AT, ModBlocks.SOLID_BLOCK);
		secret(h, AT).setDisguise(all(Blocks.GLASS));
		h.getLevel().setBlock(h.absolutePos(AT), h.getBlockState(AT).setValue(Opacity.OPAQUE, true), Block.UPDATE_ALL);
		check(h, h.getBlockState(AT).getValue(Opacity.OPAQUE), "the test should start from a wrong state");
		Opacity.checkChunk(h.getLevel(), h.getLevel().getChunkAt(h.absolutePos(AT)));
		check(h, h.getBlockState(AT).getValue(Opacity.OPAQUE), "the check alone must not change the chunk");
		Opacity.putRight(h.getLevel());
		check(h, seeThrough(h, AT), "the end of the tick should make it see-through: " + h.getBlockState(AT));
		h.succeed();
	}
}
