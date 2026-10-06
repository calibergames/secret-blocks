package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.entity.SecretHopperBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Hopper is a vanilla hopper (same transfer code and timing)
 * shaped as a full, disguised block. Layout: stone floor at y = 0; the hopper at
 * {@link #HOPPER} faces down unless a test says otherwise.
 */
public class HopperGameTests implements FabricGameTest {

	private static final BlockPos HOPPER = new BlockPos(3, 2, 3);

	private static BlockState facing(Direction dir) {
		return ModBlocks.SECRET_HOPPER.defaultBlockState().setValue(HopperBlock.FACING, dir);
	}

	private static SecretHopperBlockEntity hopper(GameTestHelper h, BlockPos rel) {
		BlockEntity entity = h.getBlockEntity(rel);
		if (entity instanceof SecretHopperBlockEntity hopper) {
			return hopper;
		}
		h.fail("expected a SecretHopperBlockEntity, found " + entity, rel);
		throw new AssertionError("unreachable");
	}

	private static Container container(GameTestHelper h, BlockPos rel) {
		return (Container) h.getBlockEntity(rel);
	}

	private static int count(Container container, Item item) {
		int n = 0;
		for (int i = 0; i < container.getContainerSize(); i++) {
			if (container.getItem(i).is(item)) {
				n += container.getItem(i).getCount();
			}
		}
		return n;
	}

	private static Map<Direction, BlockState> all(Block block) {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			faces.put(dir, block.defaultBlockState());
		}
		return faces;
	}

	/** Chest above → Secret Hopper → chest below. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void movesItemsFromTheChestAboveToTheChestBelow(GameTestHelper h) {
		floor(h);
		h.setBlock(HOPPER.above(), Blocks.CHEST);
		h.setBlock(HOPPER, facing(Direction.DOWN));
		h.setBlock(HOPPER.below(), Blocks.CHEST);
		container(h, HOPPER.above()).setItem(0, new ItemStack(Items.DIAMOND, 3));
		h.succeedWhen(() -> {
			check(h, count(container(h, HOPPER.below()), Items.DIAMOND) == 3, "all 3 diamonds should reach the chest below");
			check(h, count(container(h, HOPPER.above()), Items.DIAMOND) == 0, "the chest above should be empty");
		});
	}

	/** Facing sideways, it pushes into the container it faces. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void pushesSidewaysIntoTheContainerItFaces(GameTestHelper h) {
		floor(h);
		h.setBlock(HOPPER, facing(Direction.EAST));
		h.setBlock(HOPPER.east(), Blocks.CHEST);
		h.setBlock(HOPPER.below(), Blocks.CHEST);
		hopper(h, HOPPER).setItem(0, new ItemStack(Items.DIAMOND, 2));
		h.succeedWhen(() -> {
			check(h, count(container(h, HOPPER.east()), Items.DIAMOND) == 2, "both diamonds should go east");
			check(h, count(container(h, HOPPER.below()), Items.DIAMOND) == 0, "nothing should go down");
		});
	}

	/** Even as a full block, it collects an item resting on top. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void collectsAnItemRestingOnTop(GameTestHelper h) {
		floor(h);
		h.setBlock(HOPPER, facing(Direction.DOWN));
		h.setBlock(HOPPER.below(), Blocks.CHEST);
		h.spawnItem(Items.DIAMOND, 3.5f, 3.2f, 3.5f);
		h.succeedWhen(() -> check(h, count(container(h, HOPPER.below()), Items.DIAMOND) == 1,
				"the diamond on top should end up in the chest below"));
	}

	/** Powered by redstone, it is locked: nothing moves. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lockedWhilePowered(GameTestHelper h) {
		floor(h);
		h.setBlock(HOPPER, facing(Direction.DOWN));
		h.setBlock(HOPPER.below(), Blocks.CHEST);
		h.setBlock(HOPPER.east(), Blocks.REDSTONE_BLOCK);
		hopper(h, HOPPER).setItem(0, new ItemStack(Items.DIAMOND, 2));
		h.runAfterDelay(40, () -> {
			check(h, !h.getBlockState(HOPPER).getValue(HopperBlock.ENABLED), "the hopper should be disabled");
			check(h, count(container(h, HOPPER.below()), Items.DIAMOND) == 0, "a powered hopper must not push");
			h.succeed();
		});
	}

	/**
	 * Same code, same timing: a vanilla hopper and a Secret Hopper, side by
	 * side, empty into chests at the same rate (one item per 8 ticks).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void transfersAtTheVanillaRate(GameTestHelper h) {
		floor(h);
		BlockPos vanilla = new BlockPos(5, 2, 3);
		h.setBlock(HOPPER, facing(Direction.DOWN));
		h.setBlock(vanilla, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		h.setBlock(HOPPER.below(), Blocks.CHEST);
		h.setBlock(vanilla.below(), Blocks.CHEST);
		hopper(h, HOPPER).setItem(0, new ItemStack(Items.DIAMOND, 10));
		container(h, vanilla).setItem(0, new ItemStack(Items.DIAMOND, 10));
		h.onEachTick(() -> {
			int secret = count(container(h, HOPPER.below()), Items.DIAMOND);
			int plain = count(container(h, vanilla.below()), Items.DIAMOND);
			check(h, secret == plain, "tick " + h.getTick() + ": secret hopper moved " + secret + ", vanilla " + plain);
		});
		h.runAfterDelay(40, () -> {
			int moved = count(container(h, vanilla.below()), Items.DIAMOND);
			check(h, moved >= 4 && moved < 10, "both should be part way (8-tick cooldown), vanilla moved " + moved);
			h.succeed();
		});
	}

	/**
	 * Hopper chains: the hopper-to-hopper cooldown logic in vanilla
	 * {@code HopperBlockEntity} (it checks {@code instanceof HopperBlockEntity})
	 * applies to the Secret Hopper too. Three vertical chains, chest → hopper →
	 * hopper → chest, with 10 diamonds each: vanilla + vanilla, Secret on top,
	 * Secret below. The bottom chests must fill at exactly the same rate on
	 * every tick.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void hopperChainsMatchVanillaTiming(GameTestHelper h) {
		floor(h);
		BlockState vanillaDown = Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN);
		BlockState secretDown = facing(Direction.DOWN);
		int[] columns = {1, 3, 5};
		BlockState[][] hoppers = {{vanillaDown, vanillaDown}, {secretDown, vanillaDown}, {vanillaDown, secretDown}};
		for (int i = 0; i < columns.length; i++) {
			int x = columns[i];
			h.setBlock(new BlockPos(x, 1, 3), Blocks.CHEST);
			h.setBlock(new BlockPos(x, 2, 3), hoppers[i][1]);
			h.setBlock(new BlockPos(x, 3, 3), hoppers[i][0]);
			h.setBlock(new BlockPos(x, 4, 3), Blocks.CHEST);
		}
		for (int x : columns) {
			container(h, new BlockPos(x, 4, 3)).setItem(0, new ItemStack(Items.DIAMOND, 10));
		}
		h.onEachTick(() -> {
			int plain = count(container(h, new BlockPos(1, 1, 3)), Items.DIAMOND);
			int secretTop = count(container(h, new BlockPos(3, 1, 3)), Items.DIAMOND);
			int secretBottom = count(container(h, new BlockPos(5, 1, 3)), Items.DIAMOND);
			check(h, secretTop == plain && secretBottom == plain,
					"tick " + h.getTick() + ": vanilla chain " + plain + ", Secret on top " + secretTop + ", Secret below " + secretBottom);
		});
		h.runAfterDelay(120, () -> {
			int moved = count(container(h, new BlockPos(1, 1, 3)), Items.DIAMOND);
			check(h, moved >= 5, "the chains should have moved items through both hoppers, vanilla moved " + moved);
			h.succeed();
		});
	}

	/** Placed through the item on a block's side, it faces like a vanilla hopper and copies the block. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void facingMatchesVanilla(GameTestHelper h) {
		floor(h);
		h.setBlock(new BlockPos(1, 1, 2), Blocks.BRICKS);
		h.setBlock(new BlockPos(5, 1, 2), Blocks.BRICKS);
		place(h, ModBlocks.SECRET_HOPPER.asItem(), new Vec3(1.5, 1, 6.5), new BlockPos(1, 1, 2), Direction.SOUTH, new Vec3(1.5, 1.5, 3.0));
		place(h, Items.HOPPER, new Vec3(5.5, 1, 6.5), new BlockPos(5, 1, 2), Direction.SOUTH, new Vec3(5.5, 1.5, 3.0));
		checkBlock(h, new BlockPos(1, 1, 3), ModBlocks.SECRET_HOPPER);
		Direction secret = h.getBlockState(new BlockPos(1, 1, 3)).getValue(HopperBlock.FACING);
		Direction vanilla = h.getBlockState(new BlockPos(5, 1, 3)).getValue(HopperBlock.FACING);
		check(h, secret == vanilla && secret == Direction.NORTH, "secret hopper faces " + secret + ", vanilla faces " + vanilla);
		checkFaces(h, new BlockPos(1, 1, 3), Blocks.BRICKS);
		h.succeed();
	}

	/** Toggling ENABLED changes the block state; the disguise must survive it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void keepsDisguiseWhenLockedAndUnlocked(GameTestHelper h) {
		floor(h);
		h.setBlock(HOPPER, facing(Direction.DOWN));
		hopper(h, HOPPER).setDisguise(all(Blocks.BRICKS));
		h.setBlock(HOPPER.east(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(10, () -> {
			check(h, !h.getBlockState(HOPPER).getValue(HopperBlock.ENABLED), "the hopper should be locked");
			checkFaces(h, HOPPER, Blocks.BRICKS);
			h.setBlock(HOPPER.east(), Blocks.AIR);
		});
		h.runAfterDelay(20, () -> {
			check(h, h.getBlockState(HOPPER).getValue(HopperBlock.ENABLED), "the hopper should be unlocked");
			checkFaces(h, HOPPER, Blocks.BRICKS);
			h.succeed();
		});
	}

	/** It saves as its own type, so it loads back as a Secret Hopper with its disguise and contents. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void savesAndLoadsAsASecretHopper(GameTestHelper h) {
		h.setBlock(HOPPER, facing(Direction.DOWN));
		hopper(h, HOPPER).setDisguise(all(Blocks.BRICKS));
		hopper(h, HOPPER).setItem(4, new ItemStack(Items.DIAMOND, 7));
		CompoundTag saved = hopper(h, HOPPER).saveWithFullMetadata();
		check(h, "secretblocks:secret_hopper".equals(saved.getString("id")), "saved id should be secretblocks:secret_hopper, got " + saved.getString("id"));
		BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(HOPPER), h.getBlockState(HOPPER), saved);
		check(h, loaded instanceof SecretHopperBlockEntity, "loads back as a SecretHopperBlockEntity, got " + loaded);
		SecretHopperBlockEntity copy = (SecretHopperBlockEntity) loaded;
		check(h, ItemStack.matches(copy.getItem(4), new ItemStack(Items.DIAMOND, 7)), "slot 4 should hold 7 diamonds");
		check(h, copy.getState(Direction.UP).is(Blocks.BRICKS), "the disguise should survive");
		check(h, copy instanceof HopperBlockEntity, "it is a vanilla hopper block entity");
		h.succeed();
	}

	/** It opens the vanilla 5-slot hopper screen, titled with its own name. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void opensTheVanillaHopperScreen(GameTestHelper h) {
		h.setBlock(HOPPER, facing(Direction.DOWN));
		Player player = h.makeMockPlayer();
		AbstractContainerMenu menu = hopper(h, HOPPER).createMenu(0, player.getInventory(), player);
		check(h, menu instanceof HopperMenu, "expected a vanilla HopperMenu, got " + menu);
		check(h, hopper(h, HOPPER).getDisplayName().getContents() instanceof TranslatableContents title
				&& title.getKey().equals("block.secretblocks.secret_hopper"), "title should be Secret Hopper, got " + hopper(h, HOPPER).getDisplayName());
		h.succeed();
	}

	/** Breaking it drops its contents and itself, keeping a custom name like vanilla. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void dropsContentsAndItselfWithItsName(GameTestHelper h) {
		floor(h);
		h.setBlock(HOPPER.below(), Blocks.STONE);
		h.setBlock(HOPPER, facing(Direction.DOWN));
		hopper(h, HOPPER).setItem(2, new ItemStack(Items.DIAMOND, 3));
		hopper(h, HOPPER).setCustomName(Component.literal("Sorter"));
		h.getLevel().destroyBlock(h.absolutePos(HOPPER), true);
		checkBlock(h, HOPPER, Blocks.AIR);
		AABB around = new AABB(h.absolutePos(HOPPER)).inflate(1.5);
		int diamonds = h.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(Items.DIAMOND))
				.stream().mapToInt(e -> e.getItem().getCount()).sum();
		check(h, diamonds == 3, "3 diamonds should drop, got " + diamonds);
		List<ItemEntity> self = h.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(ModBlocks.SECRET_HOPPER.asItem()));
		check(h, self.size() == 1 && self.get(0).getItem().getHoverName().getString().equals("Sorter"),
				"the hopper should drop itself named Sorter");
		h.succeed();
	}

	/** Clients receive the disguise, never the contents. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void clientsGetTheDisguiseNotTheContents(GameTestHelper h) {
		h.setBlock(HOPPER, facing(Direction.DOWN));
		hopper(h, HOPPER).setDisguise(all(Blocks.BRICKS));
		hopper(h, HOPPER).setItem(0, new ItemStack(Items.DIAMOND, 5));
		CompoundTag tag = hopper(h, HOPPER).getUpdateTag();
		CompoundTag packet = hopper(h, HOPPER).getUpdatePacket().getTag();
		check(h, tag.contains("northState") && !tag.contains("Items"), "update tag should carry the disguise only: " + tag);
		check(h, packet != null && packet.contains("northState") && !packet.contains("Items"), "update packet should carry the disguise only: " + packet);
		h.succeed();
	}

	/** As tough as a vanilla hopper: same hardness and blast resistance, needs a pickaxe. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void asToughAsAVanillaHopper(GameTestHelper h) {
		BlockState secret = ModBlocks.SECRET_HOPPER.defaultBlockState();
		BlockState vanilla = Blocks.HOPPER.defaultBlockState();
		BlockPos pos = h.absolutePos(HOPPER);
		check(h, secret.getDestroySpeed(h.getLevel(), pos) == vanilla.getDestroySpeed(h.getLevel(), pos), "hardness should match a vanilla hopper");
		check(h, ModBlocks.SECRET_HOPPER.getExplosionResistance() == Blocks.HOPPER.getExplosionResistance(), "blast resistance should match a vanilla hopper");
		check(h, secret.requiresCorrectToolForDrops(), "it should need the correct tool to drop");
		check(h, secret.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), "a pickaxe should be the correct tool");
		check(h, secret.getSoundType() == net.minecraft.world.level.block.SoundType.METAL, "it should sound like metal");
		h.succeed();
	}

	/** Dummy Block + hopper. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void recipeWorks(GameTestHelper h) {
		AbstractContainerMenu menu = new AbstractContainerMenu(null, 0) {
			@Override
			public ItemStack quickMoveStack(Player player, int index) {
				return ItemStack.EMPTY;
			}

			@Override
			public boolean stillValid(Player player) {
				return true;
			}
		};
		CraftingContainer grid = new CraftingContainer(menu, 3, 3);
		grid.setItem(0, new ItemStack(ModBlocks.SOLID_BLOCK));
		grid.setItem(1, new ItemStack(Items.HOPPER));
		Optional<ItemStack> result = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, h.getLevel())
				.map((CraftingRecipe recipe) -> recipe.assemble(grid));
		check(h, result.isPresent() && result.get().is(ModBlocks.SECRET_HOPPER.asItem()),
				"Dummy Block + hopper should craft a Secret Hopper, got " + result);
		h.succeed();
	}
}
