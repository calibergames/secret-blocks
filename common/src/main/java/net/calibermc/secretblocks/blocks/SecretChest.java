package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretChestBlockEntity;
import net.calibermc.secretblocks.screen.SecretChestScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SecretChest extends BaseEntityBlock implements EntityBlock, SecretBlock {

    public SecretChest(Properties settings) {
        super(settings);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SecretChestBlockEntity(pos, state);
    }

    /**
     * Open the chest's screen through {@link #getMenuProvider}, the one way this
     * block's menu is made.
     */
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide) {
            MenuProvider provider = state.getMenuProvider(world, pos);
            // A locked chest opens only for a player holding the key, as vanilla's.
            if (provider != null && world.getBlockEntity(pos) instanceof SecretChestBlockEntity chest
                    && BaseContainerBlockEntity.canUnlock(player, chest.getLockKey(), provider.getDisplayName())) {
                player.openMenu(provider);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** A chest placed from a renamed item takes that name, as vanilla's. */
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (stack.hasCustomHoverName() && world.getBlockEntity(pos) instanceof SecretChestBlockEntity chest) {
            chest.setCustomName(stack.getHoverName());
        }
    }

    /**
     * The chest's menu. The server also asks for this directly when a
     * spectator right-clicks the block, without calling {@link #use}. The
     * title is the chest's custom name, or the block's own name, so the Secret
     * Trapped Chest shows as one.
     */
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof SecretChestBlockEntity chest)) {
            return null;
        }
        return new SimpleMenuProvider((syncId, inv, player) -> new SecretChestScreenHandler(syncId, inv, chest),
                chest.getCustomName() != null ? chest.getCustomName() : getName());
    }

    /**
     * The viewer count's scheduled recheck (vanilla's {@code ContainerOpenersCounter}
     * asks for a block tick 5 ticks after the chest is opened and while anyone
     * has it open): players who left without closing the screen are dropped.
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof SecretChestBlockEntity chest) {
            chest.recheckOpen();
        }
    }

    // The contents drop, with a comparator update, in SecretChestBlockEntity.dropStoredItems
    // (called for every secret block as it is removed, before this); BaseEntityBlock.onRemove
    // then removes the block entity.

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
        return stateFrom.getBlock() instanceof SecretBlock || super.skipRendering(state, stateFrom, direction);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

}

