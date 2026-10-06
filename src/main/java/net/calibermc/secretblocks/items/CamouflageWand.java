package net.calibermc.secretblocks.items;

import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Camouflage Wand: a creative and operator tool (no recipe or loot). Sneak +
 * use on a block loads it as the wand's disguise; on a secret block it loads
 * the face that was clicked, as the Switch Probe reads it. Plain use turns a
 * vanilla block into its secret counterpart, wearing the loaded disguise, or
 * a secret block back ({@link WandConversion}).
 *
 * {@link #onUseBlock} is registered as a use-block hook (Fabric's
 * {@code UseBlockCallback}), which runs ahead of the clicked block's own use
 * on both sides, so a plain use (converting) will reach the wand even on
 * chests, doors and levers, which take the click for themselves. The client only lets the click through to the server,
 * which checks the player and changes the stack; the stack syncs back. No
 * packet of its own.
 */
public class CamouflageWand extends Item {

	/** The loaded disguise: a block state, as {@link NbtUtils#writeBlockState} writes it. */
	public static final String DISGUISE = "disguise";

	private static final String MESSAGES = "message.secretblocks.camouflage_wand.";

	public CamouflageWand(Properties settings) {
		super(settings);
	}

	/**
	 * The use-block hook. Returns {@code PASS} unless the player holds the wand
	 * in {@code hand}; then the server loads the disguise (sneaking) or
	 * converts the block, or refuses with a message, and in every case the
	 * block's own use does not run.
	 */
	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(ModItems.CAMOUFLAGE_WAND) || player.isSpectator()) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS; // the server decides; the block and the stack sync back
		}
		if (!player.isShiftKeyDown()) {
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.FAIL;
			}
			WandConversion.Result result = WandConversion.convert(serverPlayer, level, hand, hit);
			if (result.reason() != null) {
				return refuse(player, result.reason());
			}
			player.displayClientMessage(Component.translatable(MESSAGES + "converted", result.converted().getName()), true);
			return InteractionResult.SUCCESS;
		}
		if (!mayUse(player)) {
			return refuse(player, "not_allowed");
		}
		BlockState state = shownState(level, hit.getBlockPos(), hit);
		if (!isLoadable(state)) {
			return refuse(player, "nothing_to_load");
		}
		stack.getOrCreateTag().put(DISGUISE, NbtUtils.writeBlockState(state));
		player.displayClientMessage(Component.translatable(MESSAGES + "loaded", state.getBlock().getName()), true);
		return InteractionResult.SUCCESS;
	}

	/** Creative mode or operator (permission level 2). */
	public static boolean mayUse(Player player) {
		return player.isCreative() || player.hasPermissions(2);
	}

	/**
	 * The state a click loads: the clicked face's disguise on a secret block,
	 * otherwise the block itself.
	 */
	private static BlockState shownState(Level level, BlockPos pos, BlockHitResult hit) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof SecretBlock && level.getBlockEntity(pos) instanceof DisguisedBlockEntity secret) {
			return secret.getState(hit.getDirection());
		}
		return state;
	}

	/**
	 * A state the wand may carry: not air (an empty secret face, or a block id
	 * the game does not know, which reads back as air) and not a secret block.
	 */
	public static boolean isLoadable(BlockState state) {
		return !state.isAir() && !(state.getBlock() instanceof SecretBlock);
	}

	/**
	 * The disguise the wand carries, read again from its NBT and checked with
	 * {@link #isLoadable}, since a stack's NBT can come from anywhere
	 * ({@code /give}, other mods, an old world).
	 */
	public static Optional<BlockState> loadedDisguise(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(DISGUISE, Tag.TAG_COMPOUND)) {
			return Optional.empty();
		}
		CompoundTag disguise = tag.getCompound(DISGUISE);
		// readBlockState throws on a malformed id; an unknown but well-formed one reads as air.
		if (!disguise.contains("Name", Tag.TAG_STRING) || ResourceLocation.tryParse(disguise.getString("Name")) == null) {
			return Optional.empty();
		}
		BlockState state = NbtUtils.readBlockState(disguise);
		return isLoadable(state) ? Optional.of(state) : Optional.empty();
	}

	private static InteractionResult refuse(Player player, String reason) {
		player.displayClientMessage(Component.translatable(MESSAGES + reason), true);
		return InteractionResult.FAIL;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(loadedDisguise(stack)
				.map(state -> Component.translatable(MESSAGES + "tooltip.loaded", state.getBlock().getName()))
				.orElse(Component.translatable(MESSAGES + "tooltip.empty"))
				.withStyle(ChatFormatting.GRAY));
	}
}
