package net.calibermc.secretblocks.gametest;

import com.mojang.math.Axis;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.calibermc.secretblocks.blocks.TorchLever;
import net.calibermc.secretblocks.blocks.WallTorchLever;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Torch Lever is a vanilla torch, floor and wall, that toggles like a
 * lever. Its lever half is compared with a vanilla lever fixed the same way,
 * and its torch half with a vanilla torch.
 */
public class TorchLeverGameTests {

	private static final Block FLOOR = ModBlocks.TORCH_LEVER;
	private static final Block WALL = ModBlocks.WALL_TORCH_LEVER;
	private static final Item ITEM = ModBlocks.TORCH_LEVER_ITEM;

	private static boolean lit(GameTestHelper h, int x, int y, int z) {
		return h.getBlockState(new BlockPos(x, y, z)).getValue(RedstoneLampBlock.LIT);
	}

	private static InteractionResult use(GameTestHelper h, BlockPos pos) {
		Player player = h.makeMockPlayer();
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false);
		return h.getBlockState(pos).use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
	}

	private static boolean powered(GameTestHelper h, BlockPos pos) {
		return h.getBlockState(pos).getValue(BlockStateProperties.POWERED);
	}

	private static BlockState wallLever(Direction facing) {
		return Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, facing);
	}

	/**
	 * On a floor: use turns it on and a lamp beside it lights, as does a lamp
	 * beside the block under it (strong power); the block at its side is not
	 * strongly powered, so a lamp beyond that stays off. Use again turns all of
	 * it off. A vanilla floor lever beside it does the same.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void floorTorchLeverTogglesLikeALever(GameTestHelper h) {
		floor(h);
		// Support stone (2, 1, z), the switch on it at (2, 2, z). Lamp A (3, 2, z) beside the switch, lamp B (2, 1, z + 1)
		// beside the support, stone (1, 2, z) at the switch's side with lamp C (0, 2, z) beyond it. Torch z = 1, lever z = 5.
		BlockPos torch = new BlockPos(2, 2, 1);
		BlockPos lever = new BlockPos(2, 2, 5);
		for (int z : new int[] {1, 5}) {
			h.setBlock(2, 1, z, Blocks.STONE);
			h.setBlock(3, 2, z, Blocks.REDSTONE_LAMP);
			h.setBlock(2, 1, z + 1, Blocks.REDSTONE_LAMP);
			h.setBlock(1, 2, z, Blocks.STONE);
			h.setBlock(0, 2, z, Blocks.REDSTONE_LAMP);
		}
		h.setBlock(torch, FLOOR);
		h.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));
		h.runAfterDelay(2, () -> {
			check(h, !lit(h, 3, 2, 1) && !lit(h, 2, 1, 2), "off, the Torch Lever's lamps should be dark");
			check(h, use(h, torch).consumesAction() && use(h, lever).consumesAction(), "use should toggle both switches");
			check(h, powered(h, torch), "used once, the Torch Lever should be on");
		});
		h.runAfterDelay(8, () -> {
			for (int z : new int[] {5, 1}) {
				String who = z == 5 ? "control, the vanilla lever: " : "the Torch Lever: ";
				check(h, lit(h, 3, 2, z), who + "the lamp beside it should light");
				check(h, lit(h, 2, 1, z + 1), who + "the lamp beside the block under it should light (strong power)");
				// Nudge that lamp: nothing else tells it to look, so without this it would stay dark whatever its block got.
				h.getLevel().updateNeighborsAt(h.absolutePos(new BlockPos(1, 2, z)), Blocks.STONE);
				check(h, !lit(h, 0, 2, z), who + "the lamp beyond the block at its side must stay off");
			}
			use(h, torch);
			use(h, lever);
			check(h, !powered(h, torch), "used twice, the Torch Lever should be off");
		});
		h.runAfterDelay(14, () -> {
			for (int z : new int[] {5, 1}) {
				check(h, !lit(h, 3, 2, z) && !lit(h, 2, 1, z + 1), (z == 5 ? "control: the lever's" : "the Torch Lever's") + " lamps should go out again");
			}
			h.succeed();
		});
	}

	/**
	 * On a wall: on, it lights a lamp on the far side of the wall block (strong
	 * power into the block it hangs on) and a lamp beside itself; a vanilla wall
	 * lever does the same.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void wallTorchLeverPowersItsWall(GameTestHelper h) {
		floor(h);
		// Wall stone (3, 1, z); the switch at (4, 1, z) facing east; lamp A (2, 1, z) behind the wall; lamp B (4, 1, z + 1) beside the switch.
		BlockPos torch = new BlockPos(4, 1, 1);
		BlockPos lever = new BlockPos(4, 1, 5);
		for (int z : new int[] {1, 5}) {
			h.setBlock(3, 1, z, Blocks.STONE);
			h.setBlock(2, 1, z, Blocks.REDSTONE_LAMP);
			h.setBlock(4, 1, z + 1, Blocks.REDSTONE_LAMP);
		}
		h.setBlock(torch, WALL.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST));
		h.setBlock(lever, wallLever(Direction.EAST));
		h.runAfterDelay(2, () -> {
			check(h, !lit(h, 2, 1, 1) && !lit(h, 4, 1, 2), "off, the wall Torch Lever's lamps should be dark");
			use(h, torch);
			use(h, lever);
		});
		h.runAfterDelay(8, () -> {
			for (int z : new int[] {5, 1}) {
				String who = z == 5 ? "control, the vanilla wall lever: " : "the wall Torch Lever: ";
				check(h, lit(h, 2, 1, z), who + "the lamp behind the wall block should light (strong power)");
				check(h, lit(h, 4, 1, z + 1), who + "the lamp beside it should light");
			}
			h.succeed();
		});
	}

	/** Signal 15 on every side when on, strong power only toward the block it is fixed to; nothing when off. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void signalsLikeALever(GameTestHelper h) {
		BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
		BlockState[][] pairs = {
				{FLOOR.defaultBlockState(), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR)},
				{WALL.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), wallLever(Direction.SOUTH)}};
		for (BlockState[] pair : pairs) {
			for (boolean on : new boolean[] {false, true}) {
				BlockState torch = pair[0].setValue(BlockStateProperties.POWERED, on);
				BlockState lever = pair[1].setValue(LeverBlock.POWERED, on);
				check(h, torch.isSignalSource(), torch + " should be a signal source");
				for (Direction direction : Direction.values()) {
					check(h, torch.getSignal(h.getLevel(), pos, direction) == lever.getSignal(h.getLevel(), pos, direction),
							torch + " weak power asked from " + direction + " should match the lever's " + lever.getSignal(h.getLevel(), pos, direction));
					check(h, torch.getDirectSignal(h.getLevel(), pos, direction) == lever.getDirectSignal(h.getLevel(), pos, direction),
							torch + " strong power asked from " + direction + " should match the lever's " + lever.getDirectSignal(h.getLevel(), pos, direction));
				}
				check(h, lever.getSignal(h.getLevel(), pos, Direction.NORTH) == (on ? 15 : 0), "control: a lever gives " + (on ? 15 : 0));
			}
		}
		h.succeed();
	}

	/**
	 * It is a torch: light 14 on or off, no collision, broken at once, the same
	 * shape as a vanilla torch while off (on, the outline follows the tilt),
	 * and no block entity.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void looksAndBreaksLikeATorch(GameTestHelper h) {
		floor(h);
		BlockPos pos = new BlockPos(2, 1, 2);
		BlockPos abs = h.absolutePos(pos);
		BlockState[][] pairs = {
				{FLOOR.defaultBlockState(), Blocks.TORCH.defaultBlockState()},
				{WALL.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST)}};
		for (BlockState[] pair : pairs) {
			for (boolean on : new boolean[] {false, true}) {
				BlockState torch = pair[0].setValue(BlockStateProperties.POWERED, on);
				BlockState vanilla = pair[1];
				check(h, vanilla.getLightEmission() == 14, "control: a vanilla torch gives light 14");
				check(h, torch.getLightEmission() == 14, torch + " should give light 14, gives " + torch.getLightEmission());
				VoxelShape shape = torch.getShape(h.getLevel(), abs);
				if (!on) {
					check(h, shape.equals(vanilla.getShape(h.getLevel(), abs)), torch + " should have the vanilla torch's shape");
				} else {
					// On, it tilts: the outline covers the upright one and reaches further the way it leans
					// (the floor torch east; this wall torch, facing west, further west from its wall).
					AABB tilted = shape.bounds();
					AABB upright = vanilla.getShape(h.getLevel(), abs).bounds();
					boolean wall = torch.is(WALL);
					check(h, tilted.minX <= upright.minX && tilted.maxX >= upright.maxX && tilted.minY <= upright.minY && tilted.maxY >= upright.maxY
							&& tilted.minZ <= upright.minZ && tilted.maxZ >= upright.maxZ, torch + " tilted outline " + tilted + " should cover the upright one " + upright);
					check(h, wall ? tilted.minX < upright.minX - 0.1 : tilted.maxX > upright.maxX + 0.1,
							torch + " tilted outline " + tilted + " should reach further the way it leans than " + upright);
				}
				check(h, torch.getCollisionShape(h.getLevel(), abs, CollisionContext.empty()).isEmpty(), torch + " should have no collision");
				check(h, torch.getDestroySpeed(h.getLevel(), abs) == 0 && vanilla.getDestroySpeed(h.getLevel(), abs) == 0, torch + " should break at once");
				check(h, !torch.hasBlockEntity(), torch + " is not a secret block: no block entity");
				check(h, torch.getPistonPushReaction() == vanilla.getPistonPushReaction() && vanilla.getPistonPushReaction() == net.minecraft.world.level.material.PushReaction.DESTROY,
						torch + " should be broken by a piston, as a torch is; its reaction is " + torch.getPistonPushReaction());
				check(h, torch.getSoundType() == vanilla.getSoundType(), torch + " should sound like a torch");
				check(h, !torch.blocksMotion() && !vanilla.blocksMotion(), torch + " should not block motion, as a torch (water washes it away)");
			}
		}
		// On, the wall form's outline keeps its wall edge and reaches 8.5 pixels from the wall (a wall torch: 5), on every facing.
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			AABB tilted = WALL.defaultBlockState().setValue(WallTorchBlock.FACING, facing).setValue(BlockStateProperties.POWERED, true).getShape(h.getLevel(), abs).bounds();
			AABB upright = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing).getShape(h.getLevel(), abs).bounds();
			AABB expected = switch (facing) {
				case EAST -> upright.setMaxX(8.5 / 16);
				case WEST -> upright.setMinX(7.5 / 16);
				case SOUTH -> upright.setMaxZ(8.5 / 16);
				default -> upright.setMinZ(7.5 / 16);
			};
			check(h, tilted.equals(expected), "the wall form facing " + facing + ", on, should have the outline " + expected + ", has " + tilted);
			// The flame: 0.52 out from the wall (the wall is opposite the facing), 0.74 up, centred sideways.
			Vec3 flame = WallTorchLever.tiltedFlame(facing);
			Vec3 want = new Vec3(0.5 + 0.02 * facing.getStepX(), 0.74, 0.5 + 0.02 * facing.getStepZ());
			check(h, flame.distanceTo(want) < 1.0e-6, "the wall form facing " + facing + " should show its flame at " + want + ", shows it at " + flame);
			double fromWall = facing.getAxis() == Direction.Axis.X ? (facing == Direction.EAST ? flame.x : 1 - flame.x) : (facing == Direction.SOUTH ? flame.z : 1 - flame.z);
			check(h, Math.abs(fromWall - 0.52) < 1.0e-6, "the wall form facing " + facing + " should show its flame 0.52 from its wall, shows it " + fromWall);
		}
		// The floor form leans the way it faces: its outline reaches 3 pixels further that way, and its flame sits 0.27 out that way.
		AABB uprightFloor = Blocks.TORCH.defaultBlockState().getShape(h.getLevel(), abs).bounds();
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			AABB tilted = FLOOR.defaultBlockState().setValue(TorchLever.FACING, facing).setValue(BlockStateProperties.POWERED, true).getShape(h.getLevel(), abs).bounds();
			AABB expected = switch (facing) {
				case EAST -> uprightFloor.setMaxX(13.0 / 16);
				case WEST -> uprightFloor.setMinX(3.0 / 16);
				case SOUTH -> uprightFloor.setMaxZ(13.0 / 16);
				default -> uprightFloor.setMinZ(3.0 / 16);
			};
			check(h, tilted.equals(expected), "the floor form facing " + facing + ", on, should have the outline " + expected + ", has " + tilted);
			Vec3 flame = TorchLever.tiltedFlame(facing);
			Vec3 want = new Vec3(0.5 + 0.27 * facing.getStepX(), 0.65, 0.5 + 0.27 * facing.getStepZ());
			check(h, flame.distanceTo(want) < 1.0e-6, "the floor form facing " + facing + " should show its flame at " + want + ", shows it at " + flame);
		}
		check(h, FLOOR.defaultBlockState().getValue(TorchLever.FACING) == Direction.EAST, "the floor form should face east by default (the only way it leaned before)");
		check(h, FLOOR.asItem() == ITEM && WALL.asItem() == ITEM, "both blocks should belong to the one Torch Lever item");
		h.succeed();
	}

	/**
	 * It pops off when its floor or wall block goes, dropping one Torch Lever,
	 * and a lamp it was lighting goes out. A vanilla torch beside it pops too.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void popsOffWhenItsSupportGoes(GameTestHelper h) {
		floor(h);
		// Floor form on stone (2, 1, 1) with a lamp beside it; wall form on stone (2, 1, 5), hanging east, with a lamp above it.
		// Lamps first: a lamp set beside a switch that is already on is not told to light.
		h.setBlock(3, 2, 1, Blocks.REDSTONE_LAMP);
		h.setBlock(3, 2, 5, Blocks.REDSTONE_LAMP);
		h.setBlock(2, 1, 1, Blocks.STONE);
		h.setBlock(2, 2, 1, FLOOR);
		h.setBlock(2, 1, 5, Blocks.STONE);
		h.setBlock(3, 1, 5, WALL.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST));
		use(h, new BlockPos(2, 2, 1));
		use(h, new BlockPos(3, 1, 5));
		// Controls: vanilla torches on stone at x = 5.
		h.setBlock(5, 1, 1, Blocks.STONE);
		h.setBlock(5, 2, 1, Blocks.TORCH);
		h.runAfterDelay(4, () -> {
			check(h, lit(h, 3, 2, 1) && lit(h, 3, 2, 5), "on, both Torch Levers should light their lamps");
			h.setBlock(2, 1, 1, Blocks.AIR);
			h.setBlock(2, 1, 5, Blocks.AIR);
			h.setBlock(5, 1, 1, Blocks.AIR);
		});
		h.runAfterDelay(10, () -> {
			checkBlock(h, new BlockPos(5, 2, 1), Blocks.AIR);
			checkBlock(h, new BlockPos(2, 2, 1), Blocks.AIR);
			checkBlock(h, new BlockPos(3, 1, 5), Blocks.AIR);
			check(h, !lit(h, 3, 2, 1) && !lit(h, 3, 2, 5), "with the Torch Levers gone their lamps should go out");
			int dropped = 0;
			for (ItemEntity drop : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(3, 1, 3))).inflate(5), e -> e.getItem().is(ITEM))) {
				dropped += drop.getItem().getCount();
				drop.discard();
			}
			check(h, dropped == 2, "the two popped Torch Levers should drop one item each, dropped " + dropped);
			h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(3, 1, 3))).inflate(5), e -> e.getItem().is(Items.TORCH)).forEach(ItemEntity::discard);
			h.succeed();
		});
	}

	/**
	 * On top of a straight wall it raises the wall's post, as a vanilla torch
	 * does (the wall reads the minecraft:wall_post_override tag).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aWallRaisesItsPostUnderIt(GameTestHelper h) {
		floor(h);
		// Three wall pieces in a row along x at z = 1, 3, 5; the middle one is straight (no post) unless something on it asks.
		Block[] tops = {FLOOR, Blocks.TORCH, Blocks.AIR};
		for (int i = 0; i < 3; i++) {
			int z = 1 + 2 * i;
			for (int x = 2; x <= 4; x++) {
				h.setBlock(x, 1, z, Blocks.COBBLESTONE_WALL);
			}
			h.setBlock(3, 2, z, tops[i]);
		}
		h.runAfterDelay(2, () -> {
			check(h, !h.getBlockState(new BlockPos(3, 1, 5)).getValue(net.minecraft.world.level.block.WallBlock.UP), "control: a straight wall with nothing on it has no post");
			check(h, h.getBlockState(new BlockPos(3, 1, 3)).getValue(net.minecraft.world.level.block.WallBlock.UP), "control: a vanilla torch on a straight wall raises its post");
			check(h, h.getBlockState(new BlockPos(3, 1, 1)).getValue(net.minecraft.world.level.block.WallBlock.UP), "a Torch Lever on a straight wall should raise its post, as a torch does");
			h.succeed();
		});
	}

	/**
	 * Broken while on: the block it was fixed to stops being powered, so a lamp
	 * beside that block goes out (floor and wall), as with a vanilla lever.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void brokenWhileOnTurnsItsSupportOff(GameTestHelper h) {
		floor(h);
		// Floor form: stone (2, 1, z), switch (2, 2, z), lamp (2, 1, z + 1) beside the stone. Torch Lever z = 0, lever z = 3.
		// Wall form: stone (5, 1, 6), Torch Lever (6, 1, 6) facing east, lamp (4, 1, 6) behind the stone.
		for (int z : new int[] {0, 3}) {
			h.setBlock(2, 1, z + 1, Blocks.REDSTONE_LAMP);
			h.setBlock(2, 1, z, Blocks.STONE);
		}
		h.setBlock(4, 1, 6, Blocks.REDSTONE_LAMP);
		h.setBlock(5, 1, 6, Blocks.STONE);
		BlockPos[] switches = {new BlockPos(2, 2, 0), new BlockPos(2, 2, 3), new BlockPos(6, 1, 6)};
		h.setBlock(switches[0], FLOOR);
		h.setBlock(switches[1], Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));
		h.setBlock(switches[2], WALL.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST));
		for (BlockPos pos : switches) {
			use(h, pos);
		}
		h.runAfterDelay(6, () -> {
			check(h, lit(h, 2, 1, 1) && lit(h, 2, 1, 4) && lit(h, 4, 1, 6), "on, every lamp beside a support should be lit");
			for (BlockPos pos : switches) {
				h.setBlock(pos, Blocks.AIR);
			}
		});
		h.runAfterDelay(12, () -> {
			check(h, !lit(h, 2, 1, 4), "control: with the lever broken its support's lamp should go out");
			check(h, !lit(h, 2, 1, 1), "with the floor Torch Lever broken the lamp beside its support should go out");
			check(h, !lit(h, 4, 1, 6), "with the wall Torch Lever broken the lamp behind its wall block should go out");
			h.succeed();
		});
	}

	/**
	 * One item places both forms where a vanilla torch item would place its
	 * own: on top of a block the floor form, on a wall face the wall form with
	 * the same facing.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theItemPlacesFloorAndWallForms(GameTestHelper h) {
		floor(h);
		// Floor: clicked on top of the floor block. Torch Lever x = 1, vanilla torch x = 5.
		place(h, ITEM, new Vec3(1.5, 1, 2.9), new BlockPos(1, 0, 1), Direction.UP, new Vec3(1.5, 1.0, 1.5));
		place(h, Items.TORCH, new Vec3(5.5, 1, 2.9), new BlockPos(5, 0, 1), Direction.UP, new Vec3(5.5, 1.0, 1.5));
		checkBlock(h, new BlockPos(1, 1, 1), FLOOR);
		checkBlock(h, new BlockPos(5, 1, 1), Blocks.TORCH);
		// The player stood south of it, facing north: it leans north. Another, placed facing east, leans east.
		check(h, h.getBlockState(new BlockPos(1, 1, 1)).getValue(TorchLever.FACING) == Direction.NORTH,
				"placed by a player facing north it should lean north, leans " + h.getBlockState(new BlockPos(1, 1, 1)).getValue(TorchLever.FACING));
		place(h, ITEM, new Vec3(1.5, 1, 2.5), new BlockPos(3, 0, 2), Direction.UP, new Vec3(3.5, 1.0, 2.5));
		check(h, h.getBlockState(new BlockPos(3, 1, 2)).getValue(TorchLever.FACING) == Direction.EAST,
				"placed by a player facing east it should lean east, leans " + h.getBlockState(new BlockPos(3, 1, 2)).getValue(TorchLever.FACING));
		// Wall: clicked on the south face of a stone block at z = 4, from the south.
		h.setBlock(1, 1, 4, Blocks.STONE);
		h.setBlock(5, 1, 4, Blocks.STONE);
		place(h, ITEM, new Vec3(1.5, 1, 6.5), new BlockPos(1, 1, 4), Direction.SOUTH, new Vec3(1.5, 1.5, 5.0));
		place(h, Items.TORCH, new Vec3(5.5, 1, 6.5), new BlockPos(5, 1, 4), Direction.SOUTH, new Vec3(5.5, 1.5, 5.0));
		checkBlock(h, new BlockPos(1, 1, 5), WALL);
		checkBlock(h, new BlockPos(5, 1, 5), Blocks.WALL_TORCH);
		Direction ours = h.getBlockState(new BlockPos(1, 1, 5)).getValue(WallTorchBlock.FACING);
		Direction vanilla = h.getBlockState(new BlockPos(5, 1, 5)).getValue(WallTorchBlock.FACING);
		check(h, ours == vanilla && vanilla == Direction.SOUTH, "the wall Torch Lever faces " + ours + ", the vanilla wall torch " + vanilla + " (expected south)");
		check(h, !powered(h, new BlockPos(1, 1, 1)) && !powered(h, new BlockPos(1, 1, 5)), "a newly placed Torch Lever should be off");
		h.succeed();
	}

	/**
	 * A floor Torch Lever saved before it had a direction (no facing in its saved
	 * state) loads facing east, so it still leans the way it did; and it turns
	 * with a structure.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void anOldFloorTorchLeverLoadsFacingEast(GameTestHelper h) {
		CompoundTag saved = new CompoundTag();
		saved.putString("Name", "secretblocks:torch_lever");
		CompoundTag properties = new CompoundTag();
		properties.putString("powered", "true");
		saved.put("Properties", properties);
		BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
		check(h, loaded.is(FLOOR) && loaded.getValue(BlockStateProperties.POWERED), "it should load as an on floor Torch Lever, got " + loaded);
		check(h, loaded.getValue(TorchLever.FACING) == Direction.EAST, "it should face east, faces " + loaded.getValue(TorchLever.FACING));
		// Turned or mirrored with a structure, it turns as a lever would.
		check(h, loaded.rotate(Rotation.CLOCKWISE_90).getValue(TorchLever.FACING) == Direction.SOUTH, "turned a quarter clockwise it should face south");
		check(h, loaded.mirror(Mirror.FRONT_BACK).getValue(TorchLever.FACING) == Direction.WEST, "mirrored front to back it should face west");
		h.succeed();
	}

	/** Torch + lever makes it; breaking either form drops one Torch Lever. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void recipeAndDrops(GameTestHelper h) {
		floor(h);
		Optional<ItemStack> made = craft(h, Items.TORCH, Items.LEVER);
		check(h, made.isPresent() && made.get().is(ITEM) && made.get().getCount() == 1, "torch + lever should make one Torch Lever, got " + made);
		h.setBlock(2, 1, 2, FLOOR);
		h.setBlock(4, 1, 4, Blocks.STONE);
		h.setBlock(5, 1, 4, WALL.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST));
		h.getLevel().destroyBlock(h.absolutePos(new BlockPos(2, 1, 2)), true);
		h.getLevel().destroyBlock(h.absolutePos(new BlockPos(5, 1, 4)), true);
		int dropped = 0;
		for (ItemEntity drop : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(3, 1, 3))).inflate(5), e -> e.getItem().is(ITEM))) {
			dropped += drop.getItem().getCount();
			drop.discard();
		}
		check(h, dropped == 2, "breaking the floor and the wall form should drop one Torch Lever each, dropped " + dropped);
		h.succeed();
	}

	/**
	 * The look, as far as a server can see it: every state has a blockstate
	 * variant, the off variants use vanilla's torch models, the on variants use
	 * our tilted ones, and those model files exist. (The picture itself is a
	 * client check.)
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void onStatesUseTheTiltedModels(GameTestHelper h) {
		String[][] forms = {
				{"torch_lever", "minecraft:block/torch", "secretblocks:block/torch_lever_on"},
				{"wall_torch_lever", "minecraft:block/wall_torch", "secretblocks:block/wall_torch_lever_on"}};
		for (String[] form : forms) {
			JsonObject variants = GsonHelper.getAsJsonObject(resource(h, "/assets/secretblocks/blockstates/" + form[0] + ".json"), "variants");
			int expected = 8;
			check(h, variants.size() == expected, form[0] + " should have " + expected + " variants, has " + variants.size());
			for (String key : variants.keySet()) {
				boolean on = key.contains("powered=true");
				check(h, on || key.contains("powered=false"), form[0] + " variant '" + key + "' should name powered");
				String model = GsonHelper.getAsString(variants.getAsJsonObject(key), "model");
				check(h, model.equals(on ? form[2] : form[1]), form[0] + " variant '" + key + "' should use " + (on ? form[2] : form[1]) + ", uses " + model);
			}
			// The on variants turn with their facing (vanilla's wall torch values); so do the wall form's off ones.
			// The floor form's off ones are vanilla's upright torch, which needs no turn.
			Map<String, Integer> turns = Map.of("east", 0, "south", 90, "west", 180, "north", 270);
			for (Map.Entry<String, Integer> turn : turns.entrySet()) {
				for (String powered : new String[] {"false", "true"}) {
					String key = "facing=" + turn.getKey() + ",powered=" + powered;
					check(h, variants.has(key), form[0] + " should have the variant '" + key + "'");
					int y = GsonHelper.getAsInt(variants.getAsJsonObject(key), "y", 0);
					int want = form[0].startsWith("wall") || powered.equals("true") ? turn.getValue() : 0;
					check(h, y == want, form[0] + " variant '" + key + "' should be turned " + want + ", is turned " + y);
				}
			}
			// The tilted model: the torch's three elements and texture, each turned about the foot.
			boolean wall = form[0].startsWith("wall");
			JsonObject tilted = resource(h, "/assets/secretblocks/models/block/" + form[0] + "_on.json");
			check(h, tilted.getAsJsonArray("elements").size() == 3, form[0] + "_on should have the torch's three elements");
			check(h, "minecraft:block/torch".equals(GsonHelper.getAsString(tilted.getAsJsonObject("textures"), "torch")), form[0] + "_on should use the vanilla torch texture");
			for (JsonElement element : tilted.getAsJsonArray("elements")) {
				JsonObject rotation = element.getAsJsonObject().getAsJsonObject("rotation");
				JsonArray origin = rotation.getAsJsonArray("origin");
				check(h, "z".equals(GsonHelper.getAsString(rotation, "axis")) && GsonHelper.getAsFloat(rotation, "angle") == (wall ? -45.0F : -22.5F),
						form[0] + "_on should lean " + (wall ? 45 : 22.5) + " degrees about z, has " + rotation);
				check(h, origin.get(0).getAsFloat() == (wall ? 0 : 8) && origin.get(1).getAsFloat() == (wall ? 3.5F : 0) && origin.get(2).getAsFloat() == 8,
						form[0] + "_on should turn about its foot, turns about " + origin);
			}
		}
		h.succeed();
	}

	private static JsonObject resource(GameTestHelper h, String path) {
		// Through a class of the mod itself: on some loaders each mod reads only its own files.
		try (InputStream in = net.calibermc.secretblocks.SecretBlocks.class.getResourceAsStream(path)) {
			check(h, in != null, path + " should exist");
			return GsonHelper.parse(new InputStreamReader(in, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new GameTestAssertException(path + " could not be read: " + e);
		}
	}
}
