package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.entity.SecretChestBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.screen.ModScreenHandlers;
import net.calibermc.secretblocks.screen.SecretChestScreenHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Trapped Chest signals how many players look into it, as a
 * vanilla trapped chest does, and the Secret Chest ends a viewing like a
 * vanilla chest.
 *
 * Viewers are the game-test mock players, added to the level beside the chest
 * so the counter's recheck (which looks for players within 5 blocks who have
 * the chest's menu open) finds them, and so their own tick closes a menu that
 * is no longer valid. The signal is read from redstone wire beside the chest.
 */
public class TrappedChestGameTests implements FabricGameTest {

	private static final Block SECRET = ModBlocks.SECRET_TRAPPED_CHEST;

	/**
	 * A mock player standing in the level at {@code rel}; every viewer is
	 * discarded by {@link #cleanUp}. Leaving the game closes its open menu, as
	 * a server player's does ({@code ServerPlayer.doCloseContainer}).
	 */
	private static Player viewer(GameTestHelper h, List<Player> viewers, Vec3 rel) {
		Player player = new Player(h.getLevel(), BlockPos.ZERO, 0.0F, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-viewer")) {
			@Override
			public boolean isSpectator() {
				return false;
			}

			@Override
			public boolean isCreative() {
				return true;
			}

			@Override
			public boolean isLocalPlayer() {
				return true;
			}

			@Override
			protected void doCloseContainer() {
				containerMenu.removed(this);
				containerMenu = inventoryMenu;
			}
		};
		Vec3 at = abs(h, rel);
		player.moveTo(at.x, at.y, at.z, 0, 0);
		h.getLevel().addFreshEntity(player);
		viewers.add(player);
		return player;
	}

	/** Open the chest at {@code chest} for {@code player}, as its screen does: the menu starts the viewing. */
	private static void open(GameTestHelper h, Player player, BlockPos chest) {
		BlockEntity entity = h.getBlockEntity(chest);
		if (entity instanceof SecretChestBlockEntity secret) {
			player.containerMenu = new SecretChestScreenHandler(1, player.getInventory(), secret);
		} else {
			player.containerMenu = ChestMenu.threeRows(1, player.getInventory(), (ChestBlockEntity) entity);
		}
	}

	/** Close the screen as a player does: the menu ends the viewing. */
	private static void close(Player player) {
		player.containerMenu.removed(player);
		player.containerMenu = player.inventoryMenu;
	}

	private static void cleanUp(List<Player> viewers) {
		viewers.forEach(Player::discard);
	}

	private static int wire(GameTestHelper h, int x, int y, int z) {
		return h.getBlockState(new BlockPos(x, y, z)).getValue(RedStoneWireBlock.POWER);
	}

	private static void both(GameTestHelper h, String when, int expected, int secret, int vanilla) {
		check(h, vanilla == expected, "control, " + when + ": the vanilla trapped chest should give " + expected + ", gives " + vanilla);
		check(h, secret == expected, when + ": the Secret Trapped Chest should give " + expected + ", gives " + secret);
	}

	/**
	 * 0 closed, 1 with one viewer, 2 with two, 1 after one closes the screen, 0
	 * after the other leaves the game. Each count is held across the 5-tick
	 * rechecks. A vanilla trapped chest beside it goes through the same steps.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void signalCountsViewers(GameTestHelper h) {
		floor(h);
		// Chest x = 2 with wire at x = 3; Secret on z = 1, vanilla on z = 5. Viewers stand at x = 1.
		BlockPos secret = new BlockPos(2, 1, 1);
		BlockPos vanilla = new BlockPos(2, 1, 5);
		h.setBlock(secret, SECRET);
		h.setBlock(vanilla, Blocks.TRAPPED_CHEST);
		h.setBlock(3, 1, 1, Blocks.REDSTONE_WIRE);
		h.setBlock(3, 1, 5, Blocks.REDSTONE_WIRE);
		List<Player> viewers = new ArrayList<>();
		Player a = viewer(h, viewers, new Vec3(1.5, 1, 1.5));
		Player b = viewer(h, viewers, new Vec3(1.5, 1, 0.5));
		Player c = viewer(h, viewers, new Vec3(1.5, 1, 5.5));
		Player d = viewer(h, viewers, new Vec3(1.5, 1, 6.5));
		h.runAfterDelay(4, () -> {
			both(h, "closed", 0, wire(h, 3, 1, 1), wire(h, 3, 1, 5));
			open(h, a, secret);
			open(h, c, vanilla);
			// At once, not only after a recheck: pulse circuits depend on it.
			both(h, "the moment it is opened", 1, wire(h, 3, 1, 1), wire(h, 3, 1, 5));
		});
		h.runAfterDelay(16, () -> {
			both(h, "one viewer, 12 ticks on", 1, wire(h, 3, 1, 1), wire(h, 3, 1, 5));
			open(h, b, secret);
			open(h, d, vanilla);
		});
		h.runAfterDelay(28, () -> {
			both(h, "two viewers, 12 ticks on", 2, wire(h, 3, 1, 1), wire(h, 3, 1, 5));
			close(a);
			close(c);
			both(h, "the moment one closes the screen", 1, wire(h, 3, 1, 1), wire(h, 3, 1, 5));
		});
		h.runAfterDelay(40, () -> {
			both(h, "one closed the screen", 1, wire(h, 3, 1, 1), wire(h, 3, 1, 5));
			// The other leaves the game with the screen open.
			b.discard();
			d.discard();
			both(h, "the moment the last viewer leaves the game", 0, wire(h, 3, 1, 1), wire(h, 3, 1, 5));
		});
		h.runAfterDelay(52, () -> {
			both(h, "the last viewer left the game", 0, wire(h, 3, 1, 1), wire(h, 3, 1, 5));
			cleanUp(viewers);
			h.succeed();
		});
	}

	/**
	 * A viewer who is moved far away without closing the screen: the menu is no
	 * longer valid and the recheck takes the signal back to 0, as for a vanilla
	 * trapped chest.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void leavingWithoutClosingEndsTheSignal(GameTestHelper h) {
		floor(h);
		BlockPos secret = new BlockPos(2, 1, 1);
		BlockPos vanilla = new BlockPos(2, 1, 5);
		h.setBlock(secret, SECRET);
		h.setBlock(vanilla, Blocks.TRAPPED_CHEST);
		h.setBlock(3, 1, 1, Blocks.REDSTONE_WIRE);
		h.setBlock(3, 1, 5, Blocks.REDSTONE_WIRE);
		List<Player> viewers = new ArrayList<>();
		Player a = viewer(h, viewers, new Vec3(1.5, 1, 1.5));
		Player c = viewer(h, viewers, new Vec3(1.5, 1, 5.5));
		h.runAfterDelay(4, () -> {
			open(h, a, secret);
			open(h, c, vanilla);
		});
		h.runAfterDelay(12, () -> {
			both(h, "open", 1, wire(h, 3, 1, 1), wire(h, 3, 1, 5));
			for (Player player : viewers) {
				player.setNoGravity(true);
				player.moveTo(player.getX(), player.getY() + 40, player.getZ(), 0, 0);
			}
		});
		h.runAfterDelay(30, () -> {
			both(h, "the viewer is 40 blocks away", 0, wire(h, 3, 1, 1), wire(h, 3, 1, 5));
			check(h, a.containerMenu == a.inventoryMenu, "the Secret Trapped Chest's screen should have closed for a player 40 blocks away");
			cleanUp(viewers);
			h.succeed();
		});
	}

	/**
	 * Broken while open: the signal goes, the screen closes, and the contents
	 * drop once (5 gold ingots).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void brokenWhileOpen(GameTestHelper h) {
		floor(h);
		BlockPos secret = new BlockPos(2, 1, 2);
		h.setBlock(secret, SECRET);
		h.setBlock(3, 1, 2, Blocks.REDSTONE_WIRE);
		((Container) h.getBlockEntity(secret)).setItem(3, new ItemStack(Items.GOLD_INGOT, 5));
		List<Player> viewers = new ArrayList<>();
		Player a = viewer(h, viewers, new Vec3(1.5, 1, 2.5));
		h.runAfterDelay(4, () -> open(h, a, secret));
		h.runAfterDelay(12, () -> {
			check(h, wire(h, 3, 1, 2) == 1, "open, the wire should carry 1, carries " + wire(h, 3, 1, 2));
			h.setBlock(secret, Blocks.AIR);
		});
		h.runAfterDelay(20, () -> {
			check(h, wire(h, 3, 1, 2) == 0, "with the chest gone the wire should carry 0, carries " + wire(h, 3, 1, 2));
			check(h, a.containerMenu == a.inventoryMenu, "the screen of a broken chest should have closed");
			int gold = 0;
			List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(secret)).inflate(3), e -> e.getItem().is(Items.GOLD_INGOT));
			for (ItemEntity drop : drops) {
				gold += drop.getItem().getCount();
				drop.discard();
			}
			// The viewer stands beside the chest and picks up what it drops.
			gold += a.getInventory().countItem(Items.GOLD_INGOT);
			check(h, gold == 5, "the broken chest should drop its 5 gold ingots once; found " + gold + " on the ground and with the viewer");
			cleanUp(viewers);
			h.succeed();
		});
	}

	/**
	 * Strong power into the block below only: a lamp beside the block under an
	 * open chest lights (Secret and vanilla), and the block at the chest's side
	 * is not strongly powered, so a lamp beyond it stays off.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void strongPowerBelowOnly(GameTestHelper h) {
		floor(h);
		// Chest at (2, 2, z) on stone (2, 1, z); lamp A at (3, 1, z) beside that stone.
		// Stone at (1, 2, z) beside the chest; lamp B at (0, 2, z) beyond it. Secret z = 1, vanilla z = 5.
		BlockPos[] chests = {new BlockPos(2, 2, 1), new BlockPos(2, 2, 5)};
		List<Player> viewers = new ArrayList<>();
		List<Player> players = new ArrayList<>();
		for (BlockPos chest : chests) {
			int z = chest.getZ();
			h.setBlock(2, 1, z, Blocks.STONE);
			h.setBlock(chest, z == 1 ? SECRET : Blocks.TRAPPED_CHEST);
			h.setBlock(3, 1, z, Blocks.REDSTONE_LAMP);
			h.setBlock(1, 2, z, Blocks.STONE);
			h.setBlock(0, 2, z, Blocks.REDSTONE_LAMP);
			h.setBlock(2, 1, z + 1, Blocks.STONE);
			players.add(viewer(h, viewers, new Vec3(2.5, 2, z + 1.5)));
		}
		h.runAfterDelay(4, () -> {
			check(h, !lit(h, 3, 1, 1) && !lit(h, 3, 1, 5), "closed, the lamps beside the blocks below should be off");
			open(h, players.get(0), chests[0]);
			open(h, players.get(1), chests[1]);
		});
		h.runAfterDelay(14, () -> {
			check(h, lit(h, 3, 1, 5), "control: the lamp beside the block under an open vanilla trapped chest should light");
			check(h, lit(h, 3, 1, 1), "the lamp beside the block under the open Secret Trapped Chest should light");
			check(h, !lit(h, 0, 2, 5), "control: a vanilla trapped chest gives no strong power to the block at its side");
			check(h, !lit(h, 0, 2, 1), "the Secret Trapped Chest must give no strong power to the block at its side");
			BlockState state = h.getBlockState(chests[0]);
			check(h, !state.isRedstoneConductor(h.getLevel(), h.absolutePos(chests[0])), "the Secret Trapped Chest must not be a redstone conductor");
			for (Direction direction : Direction.values()) {
				int strong = state.getDirectSignal(h.getLevel(), h.absolutePos(chests[0]), direction);
				check(h, strong == (direction == Direction.UP ? 1 : 0), "strong power asked from " + direction + " should be " + (direction == Direction.UP ? 1 : 0) + ", is " + strong);
				check(h, state.getSignal(h.getLevel(), h.absolutePos(chests[0]), direction) == 1, "weak power on every side should be 1 with one viewer");
			}
			cleanUp(viewers);
			h.succeed();
		});
	}

	private static boolean lit(GameTestHelper h, int x, int y, int z) {
		return h.getBlockState(new BlockPos(x, y, z)).getValue(RedstoneLampBlock.LIT);
	}

	/**
	 * It is a chest: a comparator reads its contents (full reads 15), and next to
	 * a Secret Chest and another Secret Trapped Chest each stays a single 27-slot
	 * chest with its own contents.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aSingleChestThatComparatorsRead(GameTestHelper h) {
		floor(h);
		BlockPos[] row = {new BlockPos(2, 1, 2), new BlockPos(3, 1, 2), new BlockPos(4, 1, 2)};
		h.setBlock(row[0], SECRET);
		h.setBlock(row[1], ModBlocks.SECRET_CHEST);
		h.setBlock(row[2], SECRET);
		h.setBlock(2, 1, 3, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));
		for (BlockPos pos : row) {
			check(h, h.getBlockEntity(pos) instanceof SecretChestBlockEntity chest && chest.getContainerSize() == 27, "each chest in the row should be a 27-slot secret chest");
		}
		Container first = (Container) h.getBlockEntity(row[0]);
		for (int slot = 0; slot < 27; slot++) {
			first.setItem(slot, new ItemStack(Items.BRICK, 64));
		}
		check(h, ((Container) h.getBlockEntity(row[1])).isEmpty() && ((Container) h.getBlockEntity(row[2])).isEmpty(), "filling one chest must not fill its neighbours");
		h.runAfterDelay(8, () -> {
			int output = ((ComparatorBlockEntity) h.getBlockEntity(new BlockPos(2, 1, 3))).getOutputSignal();
			check(h, output == 15, "a comparator behind the full Secret Trapped Chest should read 15, reads " + output);
			first.clearContent();
			h.succeed();
		});
	}

	/**
	 * The Secret Chest is not trapped: opened, it gives no signal. And it ends a
	 * viewing like a vanilla chest now: the count goes back to 0 when the screen
	 * closes.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void thePlainSecretChestGivesNoSignal(GameTestHelper h) {
		floor(h);
		BlockPos chest = new BlockPos(2, 1, 2);
		h.setBlock(chest, ModBlocks.SECRET_CHEST);
		h.setBlock(3, 1, 2, Blocks.REDSTONE_WIRE);
		List<Player> viewers = new ArrayList<>();
		Player a = viewer(h, viewers, new Vec3(1.5, 1, 2.5));
		h.runAfterDelay(4, () -> open(h, a, chest));
		h.runAfterDelay(12, () -> {
			SecretChestBlockEntity entity = (SecretChestBlockEntity) h.getBlockEntity(chest);
			check(h, entity.getOpenerCount() == 1, "the open Secret Chest should count 1 viewer, counts " + entity.getOpenerCount());
			check(h, wire(h, 3, 1, 2) == 0 && !h.getBlockState(chest).isSignalSource(), "the plain Secret Chest must give no signal when open");
			close(a);
			check(h, entity.getOpenerCount() == 0, "closing the screen should end the viewing, the count is " + entity.getOpenerCount());
			cleanUp(viewers);
			h.succeed();
		});
	}

	/**
	 * The Secret Chest's screen closes like a vanilla chest's: valid beside the
	 * chest, not valid more than 8 blocks away or once the chest is gone.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theScreenClosesWhenFarOrGone(GameTestHelper h) {
		floor(h);
		for (Block block : new Block[] {ModBlocks.SECRET_CHEST, SECRET}) {
			BlockPos pos = new BlockPos(2, 1, 2);
			h.setBlock(pos, block);
			SecretChestBlockEntity chest = (SecretChestBlockEntity) h.getBlockEntity(pos);
			Player player = h.makeMockPlayer();
			Vec3 near = abs(h, new Vec3(1.5, 1, 2.5));
			player.moveTo(near.x, near.y, near.z, 0, 0);
			check(h, chest.stillValid(player), block + ": the screen should stay open for a player beside the chest");
			player.moveTo(near.x + 9, near.y, near.z, 0, 0);
			check(h, !chest.stillValid(player), block + ": the screen should close for a player more than 8 blocks away");
			player.moveTo(near.x, near.y, near.z, 0, 0);
			h.setBlock(pos, Blocks.AIR);
			check(h, !chest.stillValid(player), block + ": the screen should close once the chest is gone");
		}
		h.succeed();
	}

	/**
	 * A slot set directly (as {@code /item} does) reaches a comparator without
	 * anything else calling setChanged, as with a vanilla chest.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aSetSlotReachesComparators(GameTestHelper h) {
		floor(h);
		// Chest x = 1, comparator x = 2 (reads the chest at its west). Secret Chest z = 1, vanilla chest z = 5.
		for (int z : new int[] {1, 5}) {
			h.setBlock(1, 1, z, z == 1 ? ModBlocks.SECRET_CHEST : Blocks.CHEST);
			h.setBlock(2, 1, z, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
		}
		h.runAfterDelay(4, () -> {
			for (int z : new int[] {1, 5}) {
				((Container) h.getBlockEntity(new BlockPos(1, 1, z))).setItem(0, new ItemStack(Items.BRICK, 64));
			}
		});
		h.runAfterDelay(12, () -> {
			int vanilla = ((ComparatorBlockEntity) h.getBlockEntity(new BlockPos(2, 1, 5))).getOutputSignal();
			int secret = ((ComparatorBlockEntity) h.getBlockEntity(new BlockPos(2, 1, 1))).getOutputSignal();
			check(h, vanilla == 1, "control: one stack set into a vanilla chest should read 1, reads " + vanilla);
			check(h, secret == 1, "one stack set into the Secret Chest should read 1 without a separate setChanged, reads " + secret);
			for (int z : new int[] {1, 5}) {
				((Container) h.getBlockEntity(new BlockPos(1, 1, z))).clearContent();
			}
			h.succeed();
		});
	}

	/** Sixteen viewers give 15, the most a redstone signal can be, as for a vanilla trapped chest. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void sixteenViewersGiveFifteen(GameTestHelper h) {
		floor(h);
		BlockPos secret = new BlockPos(2, 1, 1);
		BlockPos vanilla = new BlockPos(2, 1, 5);
		h.setBlock(secret, SECRET);
		h.setBlock(vanilla, Blocks.TRAPPED_CHEST);
		h.setBlock(3, 1, 1, Blocks.REDSTONE_WIRE);
		h.setBlock(3, 1, 5, Blocks.REDSTONE_WIRE);
		List<Player> viewers = new ArrayList<>();
		h.runAfterDelay(4, () -> {
			for (int i = 0; i < 16; i++) {
				open(h, viewer(h, viewers, new Vec3(1.5, 1, 1.5)), secret);
				open(h, viewer(h, viewers, new Vec3(1.5, 1, 5.5)), vanilla);
			}
		});
		h.runAfterDelay(16, () -> {
			SecretChestBlockEntity entity = (SecretChestBlockEntity) h.getBlockEntity(secret);
			check(h, entity.getOpenerCount() == 16, "the Secret Trapped Chest should count 16 viewers, counts " + entity.getOpenerCount());
			both(h, "sixteen viewers", 15, wire(h, 3, 1, 1), wire(h, 3, 1, 5));
			cleanUp(viewers);
			h.succeed();
		});
	}

	/**
	 * The menu a spectator gets: the server asks the block for its menu provider
	 * directly (it never calls use). It opens the chest's own menu type on this
	 * chest, titled with the block's name; the type is a plain one, so the
	 * open-screen packet needs nothing extra on any loader. Use itself is a
	 * smoke check here: a mock player opens no menu.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theMenuProviderOpensThisChest(GameTestHelper h) {
		floor(h);
		BlockPos pos = new BlockPos(2, 1, 2);
		for (Block block : new Block[] {ModBlocks.SECRET_CHEST, SECRET}) {
			h.setBlock(pos, block);
			var provider = h.getBlockState(pos).getMenuProvider(h.getLevel(), h.absolutePos(pos));
			check(h, provider != null, block + ": the block should give a menu provider");
			check(h, provider.getDisplayName().getString().equals(block.getName().getString()), block + ": the screen title should be the block's name, is " + provider.getDisplayName().getString());
			Player player = h.makeMockPlayer();
			var menu = provider.createMenu(1, player.getInventory(), player);
			check(h, menu instanceof SecretChestScreenHandler handler && handler.getContainer() == h.getBlockEntity(pos), block + ": the menu should show this chest");
			check(h, menu.getType() == ModScreenHandlers.SECRET_CHEST_SCREEN_HANDLER && menu.getType().getClass() == net.minecraft.world.inventory.MenuType.class,
					block + ": the menu should be the chest's plain menu type, is " + menu.getType().getClass().getName());
			menu.removed(player);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false);
			var result = h.getBlockState(pos).use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
			check(h, result.consumesAction(), block + ": use should be consumed, got " + result);
		}
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void recipe(GameTestHelper h) {
		Optional<ItemStack> made = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.TRAPPED_CHEST);
		check(h, made.isPresent() && made.get().is(SECRET.asItem()), "Dummy Block + trapped chest should make the Secret Trapped Chest, got " + made);
		Optional<ItemStack> plain = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.CHEST);
		check(h, plain.isPresent() && plain.get().is(ModBlocks.SECRET_CHEST.asItem()), "Dummy Block + chest should still make the Secret Chest, got " + plain);
		h.succeed();
	}
}
