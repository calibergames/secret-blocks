package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.SecretPiston;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.blocks.entity.PistonCarry;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.registry.Counterparts;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Piston and Secret Sticky Piston. Most tests run a vanilla
 * piston beside the secret one, set up alike, and compare. Layout: stone
 * floor at y = 0.
 */
public class PistonGameTests {

	private static final Block PISTON = ModBlocks.SECRET_PISTON;
	private static final Block STICKY = ModBlocks.SECRET_STICKY_PISTON;
	private static final Block HEAD = ModBlocks.SECRET_PISTON_HEAD;

	private static Map<Direction, BlockState> all(Block block) {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			faces.put(dir, block.defaultBlockState());
		}
		return faces;
	}

	private static BlockState facing(Block piston, Direction direction) {
		return piston.defaultBlockState().setValue(PistonBaseBlock.FACING, direction);
	}

	/** A piston at {@code rel}; a secret one gets the disguise {@code disguise}. */
	private static void piston(GameTestHelper h, BlockPos rel, Block piston, Direction direction, Block disguise) {
		h.setBlock(rel, facing(piston, direction));
		if (piston instanceof SecretPiston) {
			secret(h, rel).setDisguise(all(disguise));
		}
	}

	private static boolean extended(GameTestHelper h, BlockPos rel) {
		return h.getBlockState(rel).getValue(PistonBaseBlock.EXTENDED);
	}

	private static String name(GameTestHelper h, BlockPos rel) {
		return Optional.ofNullable(h.getBlockState(rel).getBlock()).map(Block::toString).orElse("?") + " at " + rel;
	}

	/** Pushes 12 blocks and refuses 13, as a vanilla piston. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void pushLimitMatchesVanilla(GameTestHelper h) {
		floor(h);
		BlockPos[] pistons = {new BlockPos(1, 1, 1), new BlockPos(5, 1, 1), new BlockPos(1, 1, 5), new BlockPos(5, 1, 5)};
		Block[] kinds = {PISTON, Blocks.PISTON, PISTON, Blocks.PISTON};
		int[] stacks = {12, 12, 13, 13};
		for (int i = 0; i < 4; i++) {
			piston(h, pistons[i], kinds[i], Direction.UP, Blocks.BRICKS);
			for (int y = 1; y <= stacks[i]; y++) {
				h.setBlock(pistons[i].above(y), Blocks.OAK_PLANKS);
			}
		}
		h.runAfterDelay(2, () -> {
			for (BlockPos pos : pistons) {
				h.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(8, () -> {
			for (int i = 0; i < 4; i++) {
				boolean pushed = stacks[i] == 12;
				check(h, extended(h, pistons[i]) == pushed, name(h, pistons[i]) + (pushed ? " should push 12 blocks" : " should refuse 13 blocks"));
				if (pushed) {
					checkBlock(h, pistons[i].above(1), i == 0 ? HEAD : Blocks.PISTON_HEAD);
					checkBlock(h, pistons[i].above(13), Blocks.OAK_PLANKS);
				} else {
					checkBlock(h, pistons[i].above(1), Blocks.OAK_PLANKS);
				}
			}
			for (int i = 0; i < 4; i++) {
				for (int y = 1; y <= 14; y++) {
					h.setBlock(pistons[i].above(y), Blocks.AIR);
				}
				h.setBlock(pistons[i].east(), Blocks.AIR);
			}
			h.succeed();
		});
	}

	/**
	 * The sticky one pushes a block and pulls it back, as vanilla's does; the
	 * plain one leaves it. Base and head keep the disguise all along.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void stickyPullMatchesVanilla(GameTestHelper h) {
		floor(h);
		BlockPos[] pistons = {new BlockPos(1, 1, 1), new BlockPos(5, 1, 1), new BlockPos(1, 1, 5), new BlockPos(5, 1, 5)};
		Block[] kinds = {STICKY, Blocks.STICKY_PISTON, PISTON, Blocks.PISTON};
		for (int i = 0; i < 4; i++) {
			piston(h, pistons[i], kinds[i], Direction.UP, Blocks.BRICKS);
			h.setBlock(pistons[i].above(), Blocks.OAK_PLANKS);
		}
		h.runAfterDelay(2, () -> {
			for (BlockPos pos : pistons) {
				h.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(7, () -> {
			for (BlockPos pos : pistons) {
				check(h, extended(h, pos), name(h, pos) + " should be extended");
				checkBlock(h, pos.above(2), Blocks.OAK_PLANKS);
			}
			checkFaces(h, pistons[0].above(), Blocks.BRICKS);
			checkFaces(h, pistons[2].above(), Blocks.BRICKS);
			check(h, h.getBlockState(pistons[0].above()).getValue(PistonHeadBlock.TYPE) == PistonType.STICKY, "the sticky one's head should be sticky");
			for (BlockPos pos : pistons) {
				h.setBlock(pos.east(), Blocks.AIR);
			}
		});
		h.runAfterDelay(12, () -> {
			for (int i = 0; i < 4; i++) {
				boolean sticky = i < 2;
				check(h, !extended(h, pistons[i]), name(h, pistons[i]) + " should be retracted");
				checkBlock(h, pistons[i].above(), sticky ? Blocks.OAK_PLANKS : Blocks.AIR);
				checkBlock(h, pistons[i].above(2), sticky ? Blocks.AIR : Blocks.OAK_PLANKS);
			}
			checkFaces(h, pistons[0], Blocks.BRICKS);
			checkFaces(h, pistons[2], Blocks.BRICKS);
			h.succeed();
		});
	}

	/**
	 * Powered from the side it extends; quasi-connectivity as vanilla: a
	 * redstone block above the space above it powers it, but only once
	 * something updates it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void sidePowerAndQuasiConnectivityMatchVanilla(GameTestHelper h) {
		floor(h);
		BlockPos[] side = {new BlockPos(1, 1, 1), new BlockPos(1, 1, 4)};
		BlockPos[] quasi = {new BlockPos(5, 1, 1), new BlockPos(5, 1, 4)};
		Block[] kinds = {PISTON, Blocks.PISTON};
		for (int i = 0; i < 2; i++) {
			piston(h, side[i], kinds[i], Direction.EAST, Blocks.BRICKS);
			piston(h, quasi[i], kinds[i], Direction.EAST, Blocks.BRICKS);
		}
		h.runAfterDelay(2, () -> {
			for (int i = 0; i < 2; i++) {
				h.setBlock(side[i].south(), Blocks.REDSTONE_BLOCK);
				h.setBlock(quasi[i].above(2), Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(6, () -> {
			for (int i = 0; i < 2; i++) {
				check(h, extended(h, side[i]), name(h, side[i]) + " powered from the side should extend");
				check(h, !extended(h, quasi[i]), name(h, quasi[i]) + " should not notice the power above until updated");
				// A neighbour update: a block placed and removed beside it.
				h.setBlock(quasi[i].west(), Blocks.STONE);
				h.setBlock(quasi[i].west(), Blocks.AIR);
			}
		});
		h.runAfterDelay(10, () -> {
			for (int i = 0; i < 2; i++) {
				check(h, extended(h, quasi[i]), name(h, quasi[i]) + " updated should extend by quasi-connectivity");
				h.setBlock(side[i].south(), Blocks.AIR);
				h.setBlock(quasi[i].above(2), Blocks.AIR);
			}
			h.succeed();
		});
	}

	/**
	 * The head belongs to its base: breaking the head breaks the base (which
	 * drops itself), and breaking the base removes the head, as vanilla.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theHeadAndBaseGoTogether(GameTestHelper h) {
		floor(h);
		BlockPos[] bases = {new BlockPos(1, 1, 1), new BlockPos(5, 1, 1), new BlockPos(1, 1, 5), new BlockPos(5, 1, 5)};
		Block[] kinds = {PISTON, Blocks.PISTON, PISTON, Blocks.PISTON};
		for (int i = 0; i < 4; i++) {
			piston(h, bases[i], kinds[i], Direction.UP, Blocks.BRICKS);
			h.setBlock(bases[i].east(), Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(6, () -> {
			for (BlockPos base : bases) {
				check(h, extended(h, base), name(h, base) + " should be extended");
			}
			// Row z = 1: break the head. Row z = 5: break the base.
			for (int i = 0; i < 2; i++) {
				h.getLevel().destroyBlock(h.absolutePos(bases[i].above()), true);
				h.getLevel().destroyBlock(h.absolutePos(bases[i + 2]), true);
			}
		});
		h.runAfterDelay(8, () -> {
			for (BlockPos base : bases) {
				checkBlock(h, base, Blocks.AIR);
				checkBlock(h, base.above(), Blocks.AIR);
			}
			for (int i = 0; i < 4; i++) {
				Block item = kinds[i];
				List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(bases[i])).inflate(1.5), e -> e.getItem().is(item.asItem()));
				check(h, drops.size() == 1 && drops.get(0).getItem().getCount() == 1, "the base at " + bases[i] + " should drop one " + item + ", dropped " + drops.size());
				drops.forEach(ItemEntity::discard);
				h.setBlock(bases[i].east(), Blocks.AIR);
			}
			h.succeed();
		});
	}

	/**
	 * A head can stay only on a base of its own kind and type, extended and
	 * facing it, as vanilla's: a sticky head on a plain base cannot, nor a
	 * Secret Piston head on a vanilla piston or the reverse.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aHeadNeedsABaseOfItsType(GameTestHelper h) {
		floor(h);
		BlockPos base = new BlockPos(3, 1, 3);
		BlockPos head = base.above();
		Block[] bases = {PISTON, STICKY, Blocks.PISTON, Blocks.STICKY_PISTON};
		Block[] heads = {HEAD, Blocks.PISTON_HEAD};
		for (Block baseBlock : bases) {
			for (Block headBlock : heads) {
				for (PistonType type : PistonType.values()) {
					h.setBlock(head, Blocks.AIR);
					h.getLevel().setBlock(h.absolutePos(base), facing(baseBlock, Direction.UP).setValue(PistonBaseBlock.EXTENDED, true), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
					BlockState headState = headBlock.defaultBlockState().setValue(PistonHeadBlock.FACING, Direction.UP).setValue(PistonHeadBlock.TYPE, type);
					boolean secretPair = baseBlock instanceof SecretPiston && headBlock == HEAD;
					boolean vanillaPair = !(baseBlock instanceof SecretPiston) && headBlock == Blocks.PISTON_HEAD;
					boolean stickyBase = baseBlock == STICKY || baseBlock == Blocks.STICKY_PISTON;
					boolean fits = (secretPair || vanillaPair) && stickyBase == (type == PistonType.STICKY);
					boolean survives = headState.canSurvive(h.getLevel(), h.absolutePos(head));
					check(h, survives == fits, headState + " on " + baseBlock + (fits ? " should stay" : " should not stay"));
				}
			}
		}
		h.getLevel().setBlock(h.absolutePos(base), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		h.succeed();
	}

	/** Re-disguised while extended (a probe, later the wand), the base passes its new disguise to the head. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theHeadFollowsTheBaseDisguise(GameTestHelper h) {
		floor(h);
		BlockPos base = new BlockPos(3, 1, 3);
		piston(h, base, PISTON, Direction.UP, Blocks.BRICKS);
		h.setBlock(base.east(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			checkFaces(h, base.above(), Blocks.BRICKS);
			secret(h, base).setDisguise(all(Blocks.OAK_PLANKS));
			checkFaces(h, base.above(), Blocks.OAK_PLANKS);
			h.setBlock(base.east(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * A one-tick pulse (the head is still moving when the retract comes, so
	 * vanilla ends the move at once): the Secret Pistons end as vanilla's beside
	 * them (a sticky one leaves its block behind), and keep their disguise.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aOneTickPulseMatchesVanilla(GameTestHelper h) {
		floor(h);
		BlockPos[] bases = {new BlockPos(1, 1, 1), new BlockPos(5, 1, 1), new BlockPos(1, 1, 5), new BlockPos(5, 1, 5)};
		Block[] kinds = {PISTON, Blocks.PISTON, STICKY, Blocks.STICKY_PISTON};
		for (int i = 0; i < 4; i++) {
			piston(h, bases[i], kinds[i], Direction.UP, Blocks.BRICKS);
			h.setBlock(bases[i].above(), Blocks.OAK_PLANKS);
		}
		h.runAfterDelay(2, () -> {
			for (BlockPos base : bases) {
				h.setBlock(base.east(), Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(3, () -> {
			for (BlockPos base : bases) {
				h.setBlock(base.east(), Blocks.AIR);
			}
		});
		h.runAfterDelay(12, () -> {
			for (int pair = 0; pair < 2; pair++) {
				BlockPos secret = bases[pair * 2];
				BlockPos vanilla = bases[pair * 2 + 1];
				check(h, !extended(h, secret) && !extended(h, vanilla), "both of pair " + pair + " should be retracted");
				for (int y = 1; y <= 3; y++) {
					Block secretBlock = h.getBlockState(secret.above(y)).getBlock();
					Block vanillaBlock = h.getBlockState(vanilla.above(y)).getBlock();
					check(h, secretBlock == vanillaBlock, "pair " + pair + ", " + y + " above: the Secret Piston left " + secretBlock + ", vanilla's " + vanillaBlock);
				}
				checkFaces(h, secret, Blocks.BRICKS);
			}
			for (BlockPos base : bases) {
				for (int y = 1; y <= 3; y++) {
					h.setBlock(base.above(y), Blocks.AIR);
				}
			}
			h.succeed();
		});
	}

	/**
	 * A one-tick pulse on a sticky piston pushing a Secret Piston: vanilla ends
	 * the pushed block's move early and leaves it out there ("spits" it); the
	 * Secret Piston left there keeps its disguise.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aBlockLeftByAOneTickPulseKeepsItsDisguise(GameTestHelper h) {
		floor(h);
		BlockPos pusher = new BlockPos(3, 1, 3);
		piston(h, pusher, Blocks.STICKY_PISTON, Direction.UP, null);
		piston(h, pusher.above(), PISTON, Direction.NORTH, Blocks.BRICKS);
		h.runAfterDelay(2, () -> h.setBlock(pusher.east(), Blocks.REDSTONE_BLOCK));
		h.runAfterDelay(3, () -> h.setBlock(pusher.east(), Blocks.AIR));
		h.runAfterDelay(12, () -> {
			check(h, !extended(h, pusher), "control: the sticky piston is retracted");
			checkBlock(h, pusher.above(), Blocks.AIR);
			checkBlock(h, pusher.above(2), PISTON);
			checkFaces(h, pusher.above(2), Blocks.BRICKS);
			h.setBlock(pusher.above(2), Blocks.AIR);
			h.succeed();
		});
	}

	/** In creative, breaking the head removes the base too and nothing drops, as vanilla. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void creativeBreakingTheHeadRemovesTheBase(GameTestHelper h) {
		floor(h);
		BlockPos[] bases = {new BlockPos(2, 1, 2), new BlockPos(5, 1, 2)};
		Block[] kinds = {PISTON, Blocks.PISTON};
		for (int i = 0; i < 2; i++) {
			piston(h, bases[i], kinds[i], Direction.UP, Blocks.BRICKS);
			h.setBlock(bases[i].east(), Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(5, () -> {
			for (BlockPos base : bases) {
				check(h, extended(h, base), name(h, base) + " should be extended");
				ServerPlayer player = player(h, new Vec3(base.getX() + 0.5, 1, 6.5), Vec3.atCenterOf(base.above()));
				// Creative's ability (setGameMode would send a packet; the test player has no connection).
				player.getAbilities().instabuild = true;
				// What a creative break does (ServerPlayerGameMode.destroyBlock; the test player has no connection to send packets on).
				BlockPos head = h.absolutePos(base.above());
				BlockState headState = h.getLevel().getBlockState(head);
				headState.getBlock().playerWillDestroy(h.getLevel(), head, headState, player);
				h.getLevel().removeBlock(head, false);
				headState.getBlock().destroy(h.getLevel(), head, headState);
			}
		});
		h.runAfterDelay(7, () -> {
			for (BlockPos base : bases) {
				checkBlock(h, base, Blocks.AIR);
				checkBlock(h, base.above(), Blocks.AIR);
				h.setBlock(base.east(), Blocks.AIR);
			}
			check(h, h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(8, 4, 8))).inflate(2)).isEmpty(),
					"nothing should drop in creative");
			h.succeed();
		});
	}

	/** Extending and retracting twice, the base keeps its disguise and the head wears it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theDisguiseSurvivesExtendingAndRetracting(GameTestHelper h) {
		floor(h);
		BlockPos base = new BlockPos(3, 1, 3);
		piston(h, base, PISTON, Direction.UP, Blocks.BRICKS);
		for (int cycle = 0; cycle < 2; cycle++) {
			int t = 2 + cycle * 12;
			h.runAfterDelay(t, () -> h.setBlock(base.east(), Blocks.REDSTONE_BLOCK));
			h.runAfterDelay(t + 5, () -> {
				check(h, extended(h, base), "it should be extended");
				checkBlock(h, base.above(), HEAD);
				checkFaces(h, base, Blocks.BRICKS);
				checkFaces(h, base.above(), Blocks.BRICKS);
				check(h, h.getBlockEntity(base.above()) instanceof SecretBlockEntity, "the head should have a disguise block entity");
				h.setBlock(base.east(), Blocks.AIR);
			});
			h.runAfterDelay(t + 10, () -> {
				check(h, !extended(h, base), "it should be retracted");
				checkBlock(h, base.above(), Blocks.AIR);
				checkFaces(h, base, Blocks.BRICKS);
			});
		}
		h.runAfterDelay(2 + 2 * 12, h::succeed);
	}

	/**
	 * A vanilla piston pushes a retracted Secret Piston, which keeps its
	 * disguise; a Secret Sticky Piston pulls it back, and it still does. An
	 * extended Secret Piston cannot be pushed, as an extended vanilla one.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aSecretPistonIsPushedLikeAPiston(GameTestHelper h) {
		floor(h);
		BlockPos pusher = new BlockPos(1, 1, 2);
		BlockPos pushed = new BlockPos(2, 1, 2);
		piston(h, pusher, STICKY, Direction.EAST, Blocks.OAK_PLANKS);
		piston(h, pushed, PISTON, Direction.UP, Blocks.BRICKS);
		BlockPos vanillaPusher = new BlockPos(1, 1, 5);
		BlockPos extendedOne = new BlockPos(2, 1, 5);
		piston(h, vanillaPusher, Blocks.PISTON, Direction.EAST, null);
		piston(h, extendedOne, PISTON, Direction.UP, Blocks.BRICKS);
		h.setBlock(extendedOne.south(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(4, () -> {
			check(h, extended(h, extendedOne), "control: the second Secret Piston is extended");
			h.setBlock(pusher.north(), Blocks.REDSTONE_BLOCK);
			h.setBlock(vanillaPusher.north(), Blocks.REDSTONE_BLOCK);
		});
		h.runAfterDelay(9, () -> {
			checkBlock(h, pushed, HEAD);
			checkBlock(h, pushed.east(), PISTON);
			checkFaces(h, pushed.east(), Blocks.BRICKS);
			check(h, !extended(h, vanillaPusher), "the vanilla piston should not push an extended Secret Piston");
			checkBlock(h, extendedOne, PISTON);
			h.setBlock(pusher.north(), Blocks.AIR);
		});
		h.runAfterDelay(14, () -> {
			checkBlock(h, pushed, PISTON);
			checkFaces(h, pushed, Blocks.BRICKS);
			checkFaces(h, pusher, Blocks.OAK_PLANKS);
			h.setBlock(vanillaPusher.north(), Blocks.AIR);
			h.setBlock(extendedOne.south(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * Saved and loaded in the middle of the move (the moving head's block
	 * entity rebuilt from its saved data), the head still wears the disguise
	 * when it lands.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aMoveSavedHalfwayKeepsTheDisguise(GameTestHelper h) {
		floor(h);
		BlockPos base = new BlockPos(3, 1, 3);
		piston(h, base, PISTON, Direction.UP, Blocks.BRICKS);
		h.setBlock(base.east(), Blocks.REDSTONE_BLOCK);
		boolean[] reloaded = {false};
		h.onEachTick(() -> {
			if (!reloaded[0] && h.getBlockEntity(base.above()) instanceof PistonMovingBlockEntity moving) {
				CompoundTag saved = moving.saveWithFullMetadata();
				check(h, saved.contains("SecretBlocksDisguise"), "the moving head should save the disguise: " + saved);
				BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(base.above()), h.getBlockState(base.above()), saved);
				check(h, loaded instanceof PistonCarry carry && carry.secretblocks$getCarried() != null, "it should load back with the disguise");
				h.getLevel().setBlockEntity(loaded);
				reloaded[0] = true;
			}
		});
		h.runAfterDelay(8, () -> {
			check(h, reloaded[0], "control: the move was caught halfway");
			checkBlock(h, base.above(), HEAD);
			checkFaces(h, base.above(), Blocks.BRICKS);
			h.setBlock(base.east(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * Placed by a player facing a powered spot it extends at once (vanilla looks
	 * at its power when placed), and its head wears the disguise from the
	 * placement.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void placedPoweredItExtendsWithItsDisguise(GameTestHelper h) {
		floor(h);
		h.setBlock(new BlockPos(3, 0, 3), Blocks.BRICKS);
		h.setBlock(new BlockPos(2, 1, 3), Blocks.REDSTONE_BLOCK);
		// Looking down at the floor: the piston faces up (towards the player, as vanilla's).
		place(h, PISTON.asItem(), new Vec3(3.5, 3, 3.5), new BlockPos(3, 0, 3), Direction.UP, new Vec3(3.5, 1.0, 3.5));
		h.runAfterDelay(6, () -> {
			BlockPos base = new BlockPos(3, 1, 3);
			checkBlock(h, base, PISTON);
			check(h, h.getBlockState(base).getValue(PistonBaseBlock.FACING) == Direction.UP, "it should face up, faces " + h.getBlockState(base).getValue(PistonBaseBlock.FACING));
			check(h, extended(h, base), "placed powered, it should extend");
			checkFaces(h, base, Blocks.BRICKS);
			checkFaces(h, base.above(), Blocks.BRICKS);
			h.setBlock(new BlockPos(2, 1, 3), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * Set beside a redstone block (no player, no neighbour update: a command or
	 * a structure), it extends, as a vanilla piston does.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void setBesidePowerItExtends(GameTestHelper h) {
		floor(h);
		BlockPos[] bases = {new BlockPos(2, 1, 2), new BlockPos(5, 1, 2)};
		Block[] kinds = {PISTON, Blocks.PISTON};
		for (int i = 0; i < 2; i++) {
			h.setBlock(bases[i].west(), Blocks.REDSTONE_BLOCK);
			h.setBlock(bases[i], facing(kinds[i], Direction.UP));
		}
		h.runAfterDelay(5, () -> {
			for (BlockPos base : bases) {
				check(h, extended(h, base), name(h, base) + " set beside power should extend");
				h.setBlock(base.west(), Blocks.AIR);
			}
			h.succeed();
		});
	}

	/**
	 * While it moves, the Secret Piston's head collides as a full block (its own
	 * shape); vanilla's moving head collides as its thin plate and rod.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theMovingHeadCollidesAsAFullBlock(GameTestHelper h) {
		floor(h);
		BlockPos[] bases = {new BlockPos(2, 1, 2), new BlockPos(5, 1, 2)};
		Block[] kinds = {PISTON, Blocks.PISTON};
		double[] volumes = {-1, -1};
		for (int i = 0; i < 2; i++) {
			piston(h, bases[i], kinds[i], Direction.UP, Blocks.BRICKS);
			h.setBlock(bases[i].east(), Blocks.REDSTONE_BLOCK);
		}
		h.onEachTick(() -> {
			for (int i = 0; i < 2; i++) {
				if (volumes[i] < 0 && h.getBlockEntity(bases[i].above()) instanceof PistonMovingBlockEntity moving) {
					volumes[i] = moving.getCollisionShape(h.getLevel(), h.absolutePos(bases[i].above())).toAabbs().stream()
							.mapToDouble(box -> box.getXsize() * box.getYsize() * box.getZsize()).sum();
				}
			}
		});
		h.runAfterDelay(8, () -> {
			check(h, volumes[1] > 0 && volumes[1] < 0.5, "control: vanilla's moving head is thin, volume " + volumes[1]);
			check(h, volumes[0] > 0.99, "the Secret Piston's moving head should collide as a full block, volume " + volumes[0]);
			for (BlockPos base : bases) {
				h.setBlock(base.east(), Blocks.AIR);
			}
			h.succeed();
		});
	}

	/**
	 * What they are made of: drop without a tool, not conductors, full
	 * blocks, the shared block entity; the head has no item, drops nothing,
	 * gives the piston on pick-block and collides as a full block; counterparts
	 * and recipes.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void propertiesAndRecipes(GameTestHelper h) {
		floor(h);
		BlockPos rel = new BlockPos(2, 1, 2);
		for (Block block : new Block[] {PISTON, STICKY}) {
			h.setBlock(rel, block);
			BlockState state = h.getBlockState(rel);
			BlockPos abs = h.absolutePos(rel);
			check(h, !state.requiresCorrectToolForDrops(), block + " should drop without a tool");
			check(h, !state.isRedstoneConductor(h.getLevel(), abs) && state.isCollisionShapeFullBlock(h.getLevel(), abs), block + " should be a full block and not a conductor");
			check(h, h.getBlockEntity(rel) instanceof SecretBlockEntity, block + " should have the shared block entity");
		}
		check(h, Counterparts.vanillaOf(PISTON).orElse(null) == Blocks.PISTON && Counterparts.vanillaOf(STICKY).orElse(null) == Blocks.STICKY_PISTON, "counterparts: the vanilla pistons");
		check(h, Counterparts.hasNone(HEAD), "the head has no counterpart");
		check(h, HEAD.asItem() == Items.AIR, "the head has no item");
		BlockState head = HEAD.defaultBlockState().setValue(PistonHeadBlock.TYPE, PistonType.STICKY);
		h.setBlock(rel, head);
		check(h, h.getBlockState(rel).isCollisionShapeFullBlock(h.getLevel(), h.absolutePos(rel)), "the head should collide as a full block");
		check(h, h.getBlockState(rel).getPistonPushReaction() == PushReaction.BLOCK, "pistons should neither push nor break the head, as vanilla's");
		ItemStack picked = HEAD.getCloneItemStack(h.getLevel(), h.absolutePos(rel), head);
		check(h, picked.is(STICKY.asItem()), "pick-block on a sticky head should give the Secret Sticky Piston, gives " + picked);
		h.getLevel().destroyBlock(h.absolutePos(rel), true);
		check(h, h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(1.5)).isEmpty(), "a broken head on its own should drop nothing");
		Optional<ItemStack> plain = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.PISTON);
		Optional<ItemStack> sticky = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.STICKY_PISTON);
		check(h, plain.isPresent() && plain.get().is(PISTON.asItem()), "Dummy Block + piston should craft a Secret Piston, got " + plain);
		check(h, sticky.isPresent() && sticky.get().is(STICKY.asItem()), "Dummy Block + sticky piston should craft a Secret Sticky Piston, got " + sticky);
		h.succeed();
	}
}
