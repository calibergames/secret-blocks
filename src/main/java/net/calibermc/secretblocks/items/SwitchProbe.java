package net.calibermc.secretblocks.items;

import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SwitchProbe extends Item {

	public SwitchProbe(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {

		Level world = context.getLevel();
		BlockPos blockPos = context.getClickedPos();
		BlockState blockState = world.getBlockState(blockPos);
		Block block = blockState.getBlock();
		ItemStack itemStack = context.getItemInHand();
		Direction dir = context.getClickedFace();

		if (block instanceof SecretBlock) {
			if (world.getBlockEntity(blockPos) instanceof DisguisedBlockEntity secretBlockEntity) {

				if (world.isClientSide()) {
					return InteractionResult.SUCCESS; // the server applies it; clients get it through the block-entity sync
				}

				if (itemStack.getItem() == ModItems.SWITCH_PROBE) {
					if (itemStack.hasTag()) {
						secretBlockEntity.setState(dir, getState(itemStack));
						secretBlockEntity.setDirection(dir, getDirection(itemStack));
					} else {
						BlockState stateAdjacent = secretBlockEntity.getState(dir);
						putStateAndDirection(itemStack, stateAdjacent, dir);
					}
				} else if (itemStack.getItem() == ModItems.SWITCH_PROBE_ROTATION_MODE) {
					secretBlockEntity.setRotation(dir, secretBlockEntity.getRotation(dir) == 270 ? 0 : secretBlockEntity.getRotation(dir) + 90);
				}
				return InteractionResult.SUCCESS;

			} else {
				return InteractionResult.FAIL;
			}
		} else if (block != Blocks.AIR && itemStack.getItem() != ModItems.SWITCH_PROBE_ROTATION_MODE) {
			if (!world.isClientSide()) {
				putStateAndDirection(itemStack, blockState, dir); // the server's copy of the stack syncs to the client
			}
			return InteractionResult.SUCCESS;
		} else {
			return InteractionResult.FAIL;
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		if (user.isShiftKeyDown()) {
			user.setItemInHand(hand, new ItemStack(user.getItemInHand(hand).getItem() == ModItems.SWITCH_PROBE ? ModItems.SWITCH_PROBE_ROTATION_MODE : ModItems.SWITCH_PROBE));
		}
		return super.use(world, user, hand);
	}

	private static void putStateAndDirection(ItemStack itemStack, BlockState state, Direction dir) {
		CompoundTag tag = itemStack.getOrCreateTag();
		tag.put("setState", NbtUtils.writeBlockState(state));
		tag.putString("setDirection", dir.name().toLowerCase());
	}

	private static BlockState getState(ItemStack itemStack) {
		return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), itemStack.getOrCreateTagElement("setState"));
	}

	private static Direction getDirection(ItemStack itemStack) {
		CompoundTag tag = itemStack.getOrCreateTag();
		Direction tempDir = Direction.NORTH;

		tempDir = Disguise.byName(tag.getString("setDirection"));

		return tempDir;
	}

}
