package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.MiningFace;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.gametest.support.mixin.ServerPlayerGameModeAccessor;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * A secret block gives itself away no more than its look does: it takes as
 * long to mine as the block it looks like (the face being mined), resists
 * a blast as the face toward it, and always drops itself. Unbreakable looks
 * count as stone. Layout: stone floor at y = 0, the secret block at
 * (3, 1, 3), the vanilla block it is compared with at (5, 1, 3).
 */
public class DisguiseLookGameTests {

	private static final BlockPos SECRET = new BlockPos(3, 1, 3);
	private static final BlockPos VANILLA = new BlockPos(5, 1, 3);

	private static final Block[] LOOKS = {Blocks.STONE, Blocks.DIRT, Blocks.OBSIDIAN, Blocks.GLASS, Blocks.OAK_PLANKS,
			Blocks.IRON_BLOCK, Blocks.SAND, Blocks.WHITE_WOOL};

	private static final ItemStack[] TOOLS = {ItemStack.EMPTY, new ItemStack(Items.WOODEN_PICKAXE),
			new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_SHOVEL), new ItemStack(Items.DIAMOND_AXE)};

	private static Map<Direction, BlockState> all(BlockState state) {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			faces.put(dir, state);
		}
		return faces;
	}

	/** A survival player north of the secret block, mining {@code face} with {@code tool}. */
	private static ServerPlayer miner(GameTestHelper h, Direction face, ItemStack tool) {
		ServerPlayer player = player(h, new Vec3(3.5, 1, 1.5), new Vec3(3.5, 1.5, 3.0));
		player.setGameMode(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
		((MiningFace) player).secretblocks$setMiningFace(face);
		return player;
	}

	private static float progress(GameTestHelper h, ServerPlayer player, BlockPos rel) {
		return h.getBlockState(rel).getDestroyProgress(player, h.getLevel(), h.absolutePos(rel));
	}

	/** Every registered secret block (the ones that wear a disguise once set). */
	private static List<Block> disguisedBlocks() {
		List<Block> blocks = new ArrayList<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block instanceof SecretBlock && BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(SecretBlocks.MOD_ID)) {
				blocks.add(block);
			}
		}
		return blocks;
	}

	/**
	 * Each secret block, with each look and each tool, takes exactly as long
	 * to mine as the block it looks like.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void miningTakesAsLongAsTheLook(GameTestHelper h) {
		floor(h);
		List<String> problems = new ArrayList<>();
		int checked = 0;
		for (Block block : disguisedBlocks()) {
			h.setBlock(SECRET, block);
			if (!(h.getBlockEntity(SECRET) instanceof DisguisedBlockEntity secret)) {
				continue;
			}
			checked++;
			for (Block look : LOOKS) {
				secret.setDisguise(all(look.defaultBlockState()));
				h.setBlock(VANILLA, look);
				for (ItemStack tool : TOOLS) {
					ServerPlayer player = miner(h, Direction.NORTH, tool);
					float mine = progress(h, player, SECRET);
					float theirs = progress(h, player, VANILLA);
					if (mine != theirs) {
						problems.add(BuiltInRegistries.BLOCK.getKey(block).getPath() + " as " + BuiltInRegistries.BLOCK.getKey(look).getPath()
								+ " with " + tool + ": " + mine + " per tick, the block " + theirs);
					}
				}
			}
			h.setBlock(SECRET, Blocks.AIR);
			h.setBlock(SECRET.above(), Blocks.AIR);
		}
		check(h, checked >= 25, "expected every disguised secret block, checked " + checked);
		check(h, problems.isEmpty(), problems.size() + " differ, first: " + problems.stream().limit(3).toList());
		h.succeed();
	}

	/** The face being mined decides: obsidian on the north face, dirt on the others. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void eachFaceMinesAsItsLook(GameTestHelper h) {
		floor(h);
		h.setBlock(SECRET, ModBlocks.SOLID_BLOCK);
		Map<Direction, BlockState> faces = all(Blocks.DIRT.defaultBlockState());
		faces.put(Direction.NORTH, Blocks.OBSIDIAN.defaultBlockState());
		secret(h, SECRET).setDisguise(faces);
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		h.setBlock(VANILLA, Blocks.OBSIDIAN);
		float obsidian = progress(h, miner(h, Direction.NORTH, pickaxe), VANILLA);
		h.setBlock(VANILLA, Blocks.DIRT);
		float dirt = progress(h, miner(h, Direction.NORTH, pickaxe), VANILLA);
		check(h, progress(h, miner(h, Direction.NORTH, pickaxe), SECRET) == obsidian, "the north face should mine as obsidian");
		check(h, progress(h, miner(h, Direction.UP, pickaxe), SECRET) == dirt, "the top should mine as dirt");
		// No dig action seen: the face toward the player (north, where they stand).
		ServerPlayer fresh = miner(h, Direction.UP, pickaxe);
		((MiningFace) fresh).secretblocks$setMiningFace(null);
		check(h, progress(h, fresh, SECRET) == obsidian, "with no dig action, the face toward the player should decide");
		h.succeed();
	}

	private static void dig(ServerPlayer player, GameTestHelper h, BlockPos rel, ServerboundPlayerActionPacket.Action action, Direction face) {
		player.gameMode.handleBlockBreakAction(h.absolutePos(rel), action, face, h.getLevel().getMaxBuildHeight(), 0);
	}

	/**
	 * The server takes the face from the dig action that starts mining: a
	 * refused start (out of reach) and an abort keep it, and a face the
	 * player cannot be hitting (behind the block) counts as the face toward
	 * them. Obsidian on the north face (toward the player), dirt elsewhere.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theServerTakesTheFaceFromTheStartAction(GameTestHelper h) {
		floor(h);
		h.setBlock(SECRET, ModBlocks.SOLID_BLOCK);
		Map<Direction, BlockState> faces = all(Blocks.DIRT.defaultBlockState());
		faces.put(Direction.NORTH, Blocks.OBSIDIAN.defaultBlockState());
		secret(h, SECRET).setDisguise(faces);
		ServerPlayer player = miner(h, Direction.UP, ItemStack.EMPTY);
		((MiningFace) player).secretblocks$setMiningFace(null);
		h.setBlock(VANILLA, Blocks.OBSIDIAN);
		float obsidian = progress(h, player, VANILLA);
		h.setBlock(VANILLA, Blocks.DIRT);
		float dirt = progress(h, player, VANILLA);
		dig(player, h, SECRET, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.NORTH);
		check(h, ((MiningFace) player).secretblocks$getMiningFace() == Direction.NORTH, "a start on the north face should keep north");
		check(h, progress(h, player, SECRET) == obsidian, "mining the north face should take as long as obsidian");
		dig(player, h, new BlockPos(3, 1, 40), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.UP);
		check(h, ((MiningFace) player).secretblocks$getMiningFace() == Direction.NORTH, "a refused start should keep the face");
		dig(player, h, SECRET, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, Direction.UP);
		check(h, ((MiningFace) player).secretblocks$getMiningFace() == Direction.NORTH, "an abort should keep the face");
		dig(player, h, SECRET, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.SOUTH);
		check(h, ((MiningFace) player).secretblocks$getMiningFace() == Direction.NORTH,
				"a face behind the block should count as the face toward the player, got " + ((MiningFace) player).secretblocks$getMiningFace());
		dig(player, h, SECRET, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, Direction.UP);
		// Standing on it, the top is a face a player can hit.
		ServerPlayer above = player(h, new Vec3(3.5, 2, 3.5), new Vec3(3.5, 2.0, 3.6));
		above.setGameMode(GameType.SURVIVAL);
		dig(above, h, SECRET, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.UP);
		check(h, ((MiningFace) above).secretblocks$getMiningFace() == Direction.UP, "a start on the top from above should keep up");
		check(h, progress(h, above, SECRET) == dirt, "mining the top should take as long as dirt");
		h.succeed();
	}

	/** A creative break takes its face from the start action too. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aCreativeBreakTakesTheFaceFromTheStartAction(GameTestHelper h) {
		floor(h);
		h.setBlock(SECRET, ModBlocks.SOLID_BLOCK);
		ServerPlayer player = miner(h, Direction.UP, ItemStack.EMPTY);
		((ServerPlayerGameModeAccessor) player.gameMode).secretblocksTest$setGameModeForPlayer(GameType.CREATIVE);
		((MiningFace) player).secretblocks$setMiningFace(Direction.UP);
		dig(player, h, SECRET, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.NORTH);
		check(h, ((MiningFace) player).secretblocks$getMiningFace() == Direction.NORTH, "a creative start on the north face should keep north");
		checkBlock(h, SECRET, Blocks.AIR);
		h.succeed();
	}

	/**
	 * The server allows the eyes up to 0.3 block behind a face's plane (players
	 * move between ticks): 0.2 behind the top keeps the top, 0.4 behind does not.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theFaceCheckAllowsALittleMovement(GameTestHelper h) {
		floor(h);
		h.setBlock(SECRET, ModBlocks.SOLID_BLOCK);
		for (double behind : new double[] {0.2, 0.4}) {
			// South of the block, eyes `behind` below its top (y 2).
			ServerPlayer player = player(h, new Vec3(3.5, 2 - behind - 1.62, 5.5), new Vec3(3.5, 1.5, 4.0));
			player.setGameMode(GameType.SURVIVAL);
			dig(player, h, SECRET, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.UP);
			Direction kept = ((MiningFace) player).secretblocks$getMiningFace();
			Direction expected = behind < 0.3 ? Direction.UP : Direction.SOUTH;
			check(h, kept == expected, "eyes " + behind + " below the top: expected " + expected + ", got " + kept);
			dig(player, h, SECRET, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, Direction.UP);
		}
		h.succeed();
	}

	/** An unbreakable look (bedrock, barrier) mines as stone, so no look makes a secret block unbreakable. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void unbreakableLooksMineAsStone(GameTestHelper h) {
		floor(h);
		h.setBlock(SECRET, ModBlocks.SOLID_BLOCK);
		h.setBlock(VANILLA, Blocks.STONE);
		for (Block look : new Block[] {Blocks.BEDROCK, Blocks.BARRIER, Blocks.END_PORTAL_FRAME}) {
			secret(h, SECRET).setDisguise(all(look.defaultBlockState()));
			for (ItemStack tool : TOOLS) {
				ServerPlayer player = miner(h, Direction.NORTH, tool);
				float mine = progress(h, player, SECRET);
				check(h, mine > 0 && mine == progress(h, player, VANILLA), look + " with " + tool + " should mine as stone, got " + mine);
			}
		}
		h.succeed();
	}

	/** Broken by hand, a secret block that looks like stone drops itself, the ones that once needed a pickaxe too. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void alwaysDropsItself(GameTestHelper h) {
		floor(h);
		List<String> problems = new ArrayList<>();
		for (Block block : new Block[] {ModBlocks.SOLID_BLOCK, ModBlocks.SECRET_HOPPER, ModBlocks.SECRET_PISTON,
				ModBlocks.SECRET_STICKY_PISTON, ModBlocks.IRON_TRAPDOOR_BLOCK, ModBlocks.SECRET_CHEST}) {
			h.setBlock(SECRET, block);
			secret(h, SECRET).setDisguise(all(Blocks.STONE.defaultBlockState()));
			ServerPlayer player = miner(h, Direction.NORTH, ItemStack.EMPTY);
			player.gameMode.destroyBlock(h.absolutePos(SECRET));
			AABB around = new AABB(h.absolutePos(SECRET)).inflate(2);
			if (h.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(block.asItem())).isEmpty()) {
				problems.add(BuiltInRegistries.BLOCK.getKey(block).getPath());
			}
			h.getLevel().getEntitiesOfClass(ItemEntity.class, around).forEach(ItemEntity::discard);
			h.setBlock(SECRET, Blocks.AIR);
		}
		check(h, problems.isEmpty(), "broken by hand, these dropped nothing: " + problems);
		h.succeed();
	}

	/** How many items the block at {@code rel} drops, blown up by an explosion of {@code radius} (0: broken without one). */
	private static int drops(GameTestHelper h, BlockPos rel, float radius) {
		BlockPos abs = h.absolutePos(rel);
		net.minecraft.world.level.storage.loot.LootParams.Builder loot = new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(abs))
				.withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
				.withOptionalParameter(LootContextParams.BLOCK_ENTITY, h.getLevel().getBlockEntity(abs));
		if (radius > 0) {
			loot.withParameter(LootContextParams.EXPLOSION_RADIUS, radius);
		}
		return h.getBlockState(rel).getDrops(loot).stream().mapToInt(ItemStack::getCount).sum();
	}

	/**
	 * Blown up, every secret block that drops anything drops only by
	 * vanilla's chance (one in the explosion's radius): out of 50 drops at
	 * radius 1000 each keeps fewer than five; broken without an explosion it
	 * drops every time.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void explosionsDropAsVanilla(GameTestHelper h) {
		floor(h);
		List<String> problems = new ArrayList<>();
		int checked = 0;
		for (Block block : disguisedBlocks()) {
			if (block.getLootTable().equals(BuiltInLootTables.EMPTY)) {
				continue;
			}
			h.setBlock(SECRET, block);
			int exploded = 0;
			int plain = 0;
			for (int i = 0; i < 50; i++) {
				exploded += drops(h, SECRET, 1000.0f);
				plain += Math.min(1, drops(h, SECRET, 0));
			}
			checked++;
			if (exploded >= 5 || plain != 50) {
				problems.add(BuiltInRegistries.BLOCK.getKey(block).getPath() + ": " + exploded + " of 50 survived a big blast, " + plain + " of 50 dropped without one");
			}
			h.setBlock(SECRET, Blocks.AIR);
		}
		check(h, checked >= 25, "expected every secret block with drops, checked " + checked);
		check(h, problems.isEmpty(), "not as vanilla: " + problems);
		h.succeed();
	}

	private static float resistance(GameTestHelper h, Vec3 centerRel) {
		Vec3 center = abs(h, centerRel);
		Explosion explosion = new Explosion(h.getLevel(), null, center.x, center.y, center.z, 4.0F, false, Explosion.BlockInteraction.DESTROY);
		BlockPos pos = h.absolutePos(SECRET);
		return new ExplosionDamageCalculator()
				.getBlockExplosionResistance(explosion, h.getLevel(), pos, h.getLevel().getBlockState(pos), Fluids.EMPTY.defaultFluidState())
				.orElseThrow();
	}

	/** Blast resistance is the look's on the face toward the blast; an unbreakable look counts as stone. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void blastResistanceFollowsTheFaceTowardTheBlast(GameTestHelper h) {
		floor(h);
		h.setBlock(SECRET, ModBlocks.SOLID_BLOCK);
		Map<Direction, BlockState> faces = all(Blocks.DIRT.defaultBlockState());
		faces.put(Direction.NORTH, Blocks.OBSIDIAN.defaultBlockState());
		faces.put(Direction.UP, Blocks.BEDROCK.defaultBlockState());
		secret(h, SECRET).setDisguise(faces);
		Vec3 north = new Vec3(3.5, 1.5, 0.5);
		Vec3 south = new Vec3(3.5, 1.5, 6.5);
		Vec3 above = new Vec3(3.5, 4.5, 3.5);
		check(h, resistance(h, north) == Blocks.OBSIDIAN.getExplosionResistance(), "a blast to the north meets obsidian, got " + resistance(h, north));
		check(h, resistance(h, south) == Blocks.DIRT.getExplosionResistance(), "a blast to the south meets dirt, got " + resistance(h, south));
		check(h, resistance(h, above) == Blocks.STONE.getExplosionResistance(), "a blast above meets bedrock, counted as stone, got " + resistance(h, above));
		h.succeed();
	}

	/** TNT beside them: a secret block that looks like dirt breaks, one that looks like obsidian stays. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void tntBreaksTheLookItWouldBreak(GameTestHelper h) {
		floor(h);
		BlockPos dirt = new BlockPos(2, 1, 3);
		BlockPos obsidian = new BlockPos(4, 1, 3);
		h.setBlock(dirt, ModBlocks.SOLID_BLOCK);
		secret(h, dirt).setDisguise(all(Blocks.DIRT.defaultBlockState()));
		h.setBlock(obsidian, ModBlocks.SOLID_BLOCK);
		secret(h, obsidian).setDisguise(all(Blocks.OBSIDIAN.defaultBlockState()));
		Vec3 center = abs(h, new Vec3(3.5, 1.5, 3.5));
		h.getLevel().explode(null, center.x, center.y, center.z, 4.0F, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
		checkBlock(h, dirt, Blocks.AIR);
		checkBlock(h, obsidian, ModBlocks.SOLID_BLOCK);
		h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(8)).forEach(ItemEntity::discard);
		h.succeed();
	}
}
