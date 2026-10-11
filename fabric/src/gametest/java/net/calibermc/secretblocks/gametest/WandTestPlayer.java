package net.calibermc.secretblocks.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A test player for the Camouflage Wand. The test sets its creative mode,
 * permission level, spectator mode and whether the spot is protected; its
 * action-bar messages are kept (it has no connection to send them on).
 *
 * The wand works only for players the server lists as connected, so
 * {@link #click} lists this player for the length of one call and no longer:
 * the server sends packets to listed players, and this one has no connection.
 */
final class WandTestPlayer extends ServerPlayer {

	private static final GameProfile PROFILE = new GameProfile(
			UUID.nameUUIDFromBytes("secretblocks-gametest-wand".getBytes(StandardCharsets.UTF_8)), "sb_wand");

	final boolean creative;
	final int permission;
	boolean spectator;
	boolean protectedSpot;
	final List<Component> messages = new ArrayList<>();

	WandTestPlayer(ServerLevel level, boolean creative, int permission) {
		super(level.getServer(), level, PROFILE);
		this.creative = creative;
		this.permission = permission;
	}

	/** A player standing at {@code feetRel}, sneaking or not. */
	static WandTestPlayer at(GameTestHelper h, Vec3 feetRel, boolean creative, int permission, boolean sneaking) {
		WandTestPlayer player = new WandTestPlayer(h.getLevel(), creative, permission);
		Vec3 feet = TestSupport.abs(h, feetRel);
		player.moveTo(feet.x, feet.y, feet.z, 0, 0);
		player.setShiftKeyDown(sneaking);
		return player;
	}

	@Override
	public boolean isCreative() {
		return creative;
	}

	@Override
	public boolean isSpectator() {
		return spectator;
	}

	@Override
	protected int getPermissionLevel() {
		return permission;
	}

	/** Spawn protection and the world border, as the server reports them. */
	@Override
	public boolean mayInteract(Level level, BlockPos pos) {
		return !protectedSpot && super.mayInteract(level, pos);
	}

	@Override
	public void displayClientMessage(Component message, boolean actionBar) {
		messages.add(message);
	}

	/** Sounds for this player alone (a locked chest's) go out on the connection it does not have. */
	@Override
	public void playNotifySound(net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source, float volume, float pitch) {
	}

	String lastMessageKey() {
		if (messages.isEmpty()) {
			return "";
		}
		return messages.get(messages.size() - 1).getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	/** Run {@code action} with this player listed as connected. */
	<T> T listed(Supplier<T> action) {
		List<ServerPlayer> players = getServer().getPlayerList().getPlayers();
		players.add(this);
		try {
			return action.get();
		} finally {
			players.remove(this);
		}
	}

	/**
	 * A click with {@code stack} on face {@code face} of the block at
	 * {@code clickedRel}, through the server's real use path
	 * ({@code ServerPlayerGameMode.useItemOn}), where the wand's hook runs
	 * before the block's own use; listed as connected when {@code listed}.
	 */
	InteractionResult click(GameTestHelper h, ItemStack stack, BlockPos clickedRel, Direction face, boolean listed) {
		setItemInHand(InteractionHand.MAIN_HAND, stack);
		Vec3 hitRel = Vec3.atCenterOf(clickedRel).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
		BlockHitResult hit = new BlockHitResult(TestSupport.abs(h, hitRel), face, h.absolutePos(clickedRel), false);
		Supplier<InteractionResult> use = () -> gameMode.useItemOn(this, h.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
		return listed ? listed(use) : use.get();
	}
}
