package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.entity.SecretDispenserBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Dispenser is a vanilla dispenser that keeps a disguise. Layout:
 * stone floor at y = 0; the dispenser at {@link #DISPENSER} faces north
 * unless a test says otherwise.
 */
public class DispenserGameTests {

	private static final BlockPos DISPENSER = new BlockPos(3, 1, 3);

	private static BlockState facingNorth() {
		return ModBlocks.SECRET_DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.NORTH);
	}

	private static SecretDispenserBlockEntity dispenser(GameTestHelper h) {
		BlockEntity entity = h.getBlockEntity(DISPENSER);
		if (entity instanceof SecretDispenserBlockEntity dispenser) {
			return dispenser;
		}
		h.fail("expected a SecretDispenserBlockEntity, found " + entity, DISPENSER);
		throw new AssertionError("unreachable");
	}

	private static Map<Direction, BlockState> all(Block block) {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			faces.put(dir, block.defaultBlockState());
		}
		return faces;
	}

	private static List<ItemEntity> itemsNear(GameTestHelper h, BlockPos rel, net.minecraft.world.item.Item item) {
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(1.5), e -> e.getItem().is(item));
	}

	/** Placed through the item, it faces the way a vanilla dispenser would, and copies the clicked block. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void facingMatchesVanilla(GameTestHelper h) {
		floor(h);
		place(h, ModBlocks.SECRET_DISPENSER.asItem(), new Vec3(1.5, 1, 6.5), new BlockPos(1, 0, 3), Direction.UP, new Vec3(1.5, 1.0, 3.5));
		place(h, Items.DISPENSER, new Vec3(5.5, 1, 6.5), new BlockPos(5, 0, 3), Direction.UP, new Vec3(5.5, 1.0, 3.5));
		place(h, ModBlocks.SECRET_DISPENSER.asItem(), new Vec3(1.5, 1, 1.9), new BlockPos(1, 0, 1), Direction.UP, new Vec3(1.5, 1.0, 1.5));
		place(h, Items.DISPENSER, new Vec3(5.5, 1, 1.9), new BlockPos(5, 0, 1), Direction.UP, new Vec3(5.5, 1.0, 1.5));
		for (int z : new int[] {3, 1}) {
			checkBlock(h, new BlockPos(1, 1, z), ModBlocks.SECRET_DISPENSER);
			Direction secret = h.getBlockState(new BlockPos(1, 1, z)).getValue(DispenserBlock.FACING);
			Direction vanilla = h.getBlockState(new BlockPos(5, 1, z)).getValue(DispenserBlock.FACING);
			check(h, secret == vanilla, "secret dispenser faces " + secret + ", vanilla faces " + vanilla);
		}
		checkFaces(h, new BlockPos(1, 1, 3), Blocks.STONE);
		h.succeed();
	}

	/**
	 * A redstone signal dispenses one item out of the facing side; holding the
	 * signal does not repeat it. The dispensed item is caught as soon as it
	 * appears: game tests share one world, and a flying item can reach a
	 * neighbouring test's area (which may clear items) if checked late.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void dispensesOncePerRisingEdge(GameTestHelper h) {
		floor(h);
		h.setBlock(DISPENSER, facingNorth());
		dispenser(h).setItem(0, new ItemStack(Items.DIAMOND, 5));
		h.setBlock(DISPENSER.east(), Blocks.REDSTONE_BLOCK);
		double northEdge = h.absolutePos(DISPENSER).getZ();
		h.startSequence()
				.thenWaitUntil(() -> {
					List<ItemEntity> out = h.getLevel().getEntitiesOfClass(ItemEntity.class,
							new AABB(h.absolutePos(DISPENSER)).inflate(3), e -> e.getItem().is(Items.DIAMOND));
					check(h, out.size() == 1, "one diamond item should have been dispensed, found " + out.size());
					check(h, out.get(0).getZ() < northEdge, "the diamond should leave through the north (facing) side");
				})
				.thenIdle(20)
				.thenExecute(() -> check(h, dispenser(h).getItem(0).getCount() == 4,
						"exactly one diamond should have left while the signal was held, slot holds " + dispenser(h).getItem(0)))
				.thenSucceed();
	}

	/** Vanilla dispense behaviors apply: an arrow is shot, not dropped. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void shootsArrows(GameTestHelper h) {
		floor(h);
		h.setBlock(DISPENSER, facingNorth());
		dispenser(h).setItem(0, new ItemStack(Items.ARROW));
		h.setBlock(DISPENSER.east(), Blocks.REDSTONE_BLOCK);
		h.succeedWhen(() -> check(h, !h.getLevel().getEntitiesOfClass(Arrow.class, new AABB(h.absolutePos(DISPENSER)).inflate(8)).isEmpty(),
				"an arrow should have been shot"));
	}

	/** Dispensing toggles the TRIGGERED state; the disguise must survive it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void keepsDisguiseWhenTriggered(GameTestHelper h) {
		floor(h);
		h.setBlock(DISPENSER, facingNorth());
		dispenser(h).setDisguise(all(Blocks.BRICKS));
		dispenser(h).setItem(0, new ItemStack(Items.DIAMOND));
		h.setBlock(DISPENSER.east(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(20, () -> {
			check(h, h.getBlockState(DISPENSER).getValue(DispenserBlock.TRIGGERED), "the dispenser should be triggered");
			checkFaces(h, DISPENSER, Blocks.BRICKS);
			h.setBlock(DISPENSER.east(), Blocks.AIR);
		});
		h.runAfterDelay(30, () -> {
			check(h, !h.getBlockState(DISPENSER).getValue(DispenserBlock.TRIGGERED), "the dispenser should be untriggered");
			checkFaces(h, DISPENSER, Blocks.BRICKS);
			h.succeed();
		});
	}

	/** It opens the vanilla 3×3 dispenser screen, titled with its own name. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void opensTheVanillaDispenserScreen(GameTestHelper h) {
		h.setBlock(DISPENSER, facingNorth());
		Player player = h.makeMockPlayer();
		AbstractContainerMenu menu = dispenser(h).createMenu(0, player.getInventory(), player);
		check(h, menu instanceof DispenserMenu, "expected a vanilla DispenserMenu, got " + menu);
		check(h, dispenser(h).getDisplayName().getContents() instanceof TranslatableContents title
				&& title.getKey().equals("block.secretblocks.secret_dispenser"), "title should be Secret Dispenser, got " + dispenser(h).getDisplayName());
		h.succeed();
	}

	/** Contents and disguise survive a save and load of the block entity. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void contentsAndDisguiseSurviveSaveAndLoad(GameTestHelper h) {
		h.setBlock(DISPENSER, facingNorth());
		dispenser(h).setDisguise(all(Blocks.BRICKS));
		dispenser(h).setItem(0, new ItemStack(Items.DIAMOND, 5));
		dispenser(h).setItem(8, new ItemStack(Items.ARROW, 64));
		dispenser(h).setDirection(Direction.NORTH, Direction.UP);
		dispenser(h).setRotation(Direction.EAST, 180);
		CompoundTag saved = dispenser(h).saveWithFullMetadata();
		BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(DISPENSER), h.getBlockState(DISPENSER), saved);
		check(h, loaded instanceof SecretDispenserBlockEntity, "loads back as a SecretDispenserBlockEntity, got " + loaded);
		SecretDispenserBlockEntity copy = (SecretDispenserBlockEntity) loaded;
		check(h, ItemStack.matches(copy.getItem(0), new ItemStack(Items.DIAMOND, 5)), "slot 0 should hold 5 diamonds");
		check(h, ItemStack.matches(copy.getItem(8), new ItemStack(Items.ARROW, 64)), "slot 8 should hold 64 arrows");
		for (Direction dir : Direction.values()) {
			check(h, copy.getState(dir).is(Blocks.BRICKS), dir + " face should still be bricks");
		}
		check(h, copy.getDir(Direction.NORTH) == Direction.UP, "north face direction should survive");
		check(h, copy.getRotation(Direction.EAST) == 180, "east face rotation should survive");
		h.succeed();
	}

	/** Breaking it drops its contents and itself. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void dropsContentsAndItselfWhenBroken(GameTestHelper h) {
		floor(h);
		h.setBlock(DISPENSER, facingNorth());
		dispenser(h).setItem(4, new ItemStack(Items.DIAMOND, 3));
		h.getLevel().destroyBlock(h.absolutePos(DISPENSER), true);
		checkBlock(h, DISPENSER, Blocks.AIR);
		int diamonds = itemsNear(h, DISPENSER, Items.DIAMOND).stream().mapToInt(e -> e.getItem().getCount()).sum();
		check(h, diamonds == 3, "3 diamonds should drop, got " + diamonds);
		check(h, !itemsNear(h, DISPENSER, ModBlocks.SECRET_DISPENSER.asItem()).isEmpty(), "the dispenser should drop itself");
		h.succeed();
	}

	/** A comparator reads its contents. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void comparatorReadsIt(GameTestHelper h) {
		floor(h);
		BlockPos comparator = DISPENSER.north();
		BlockPos lamp = comparator.north();
		h.setBlock(DISPENSER, facingNorth());
		h.setBlock(lamp, Blocks.REDSTONE_LAMP);
		h.setBlock(comparator, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.SOUTH));
		dispenser(h).setItem(0, new ItemStack(Items.DIAMOND, 64));
		dispenser(h).setChanged();
		h.succeedWhen(() -> check(h, h.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "the lamp behind the comparator should light"));
	}

	/** Clients receive the disguise, never the contents. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void clientsGetTheDisguiseNotTheContents(GameTestHelper h) {
		h.setBlock(DISPENSER, facingNorth());
		dispenser(h).setDisguise(all(Blocks.BRICKS));
		dispenser(h).setItem(0, new ItemStack(Items.DIAMOND, 5));
		CompoundTag update = dispenser(h).getUpdateTag();
		check(h, update.contains("northState") && !update.contains("Items"), "update tag should carry the disguise only: " + update);
		h.succeed();
	}

	/** The block-entity update packet also carries the disguise only. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void updatePacketCarriesTheDisguiseOnly(GameTestHelper h) {
		h.setBlock(DISPENSER, facingNorth());
		dispenser(h).setDisguise(all(Blocks.BRICKS));
		dispenser(h).setItem(0, new ItemStack(Items.DIAMOND, 5));
		CompoundTag packet = dispenser(h).getUpdatePacket().getTag();
		check(h, packet != null && packet.getCompound("northState").getString("Name").equals("minecraft:bricks") && !packet.contains("Items"),
				"update packet should carry the disguise only: " + packet);
		h.succeed();
	}

	/** Setting the disguise (placement) marks the chunk for saving. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void setDisguiseMarksChunkForSaving(GameTestHelper h) {
		h.setBlock(DISPENSER, facingNorth());
		net.minecraft.world.level.chunk.LevelChunk chunk = h.getLevel().getChunkAt(h.absolutePos(DISPENSER));
		chunk.setUnsaved(false);
		dispenser(h).setDisguise(all(Blocks.BRICKS));
		check(h, chunk.isUnsaved(), "setDisguise should mark the chunk for saving");
		h.succeed();
	}

	/** Like a vanilla dispenser, a renamed Secret Dispenser keeps its name when broken. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void keepsItsCustomNameWhenBroken(GameTestHelper h) {
		floor(h);
		h.setBlock(DISPENSER, facingNorth());
		dispenser(h).setCustomName(net.minecraft.network.chat.Component.literal("Trap"));
		h.getLevel().destroyBlock(h.absolutePos(DISPENSER), true);
		List<ItemEntity> drops = itemsNear(h, DISPENSER, ModBlocks.SECRET_DISPENSER.asItem());
		check(h, drops.size() == 1 && drops.get(0).getItem().getHoverName().getString().equals("Trap"),
				"the dropped dispenser should be named Trap, got " + drops.stream().map(e -> e.getItem().getHoverName().getString()).toList());
		h.succeed();
	}

	/** Dummy Block + dispenser. */
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
		CraftingContainer grid = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 3, 3);
		grid.setItem(0, new ItemStack(ModBlocks.SOLID_BLOCK));
		grid.setItem(1, new ItemStack(Items.DISPENSER));
		Optional<ItemStack> result = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, h.getLevel())
				.map((CraftingRecipe recipe) -> recipe.assemble(grid, h.getLevel().registryAccess()));
		check(h, result.isPresent() && result.get().is(ModBlocks.SECRET_DISPENSER.asItem()),
				"Dummy Block + dispenser should craft a Secret Dispenser, got " + result);
		h.succeed();
	}
}
