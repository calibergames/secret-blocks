package net.calibermc.secretblocks.blocks.entity;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * A block entity that carries a {@link Disguise}. Implemented by
 * {@link SecretBlockEntity} (most secret blocks) and by secret blocks that
 * need a vanilla block entity of their own, such as
 * {@link SecretDispenserBlockEntity}. Placement, the probes and the renderer
 * work through this interface. Only {@link BlockEntity} subclasses implement it.
 *
 * Every change to the disguise is saved and synced: {@link #markDisguiseChanged()} marks
 * the block entity changed (so the chunk is saved) and sends it to clients.
 * Earlier, the single-face setters (the probes' path) only synced, so
 * probe edits were lost on reload.
 */
public interface DisguisedBlockEntity {

	Disguise getDisguise();

	/** Implemented by {@link BlockEntity}. */
	void setChanged();

	default BlockState getState(Direction face) {
		return getDisguise().getState(face);
	}

	default void setState(Direction face, BlockState state) {
		getDisguise().setState(face, state);
		markDisguiseChanged();
	}

	default void setState(BlockState state) {
		for (Direction face : Direction.values()) {
			getDisguise().setState(face, state);
		}
		markDisguiseChanged();
	}

	/**
	 * Set the disguise of all six faces at once (placement). Marks the block
	 * entity changed so the chunk saves, and syncs clients through the vanilla
	 * block-entity update.
	 */
	default void setDisguise(Map<Direction, BlockState> faces) {
		faces.forEach((face, state) -> {
			if (state != null) {
				getDisguise().setState(face, state);
			}
		});
		markDisguiseChanged();
	}

	default Direction getDir(Direction face) {
		return getDisguise().getDirection(face);
	}

	default void setDirection(Direction face, Direction direction) {
		getDisguise().setDirection(face, direction);
		markDisguiseChanged();
	}

	default void setDirection(Direction direction) {
		for (Direction face : Direction.values()) {
			getDisguise().setDirection(face, direction);
		}
		markDisguiseChanged();
	}

	/** The vanilla block the Camouflage Wand made this block from, when it was not the usual counterpart; else {@code null}. */
	@org.jetbrains.annotations.Nullable
	default Block getOriginal() {
		return getDisguise().getOriginal();
	}

	default void setOriginal(@org.jetbrains.annotations.Nullable Block original) {
		getDisguise().setOriginal(original);
		markDisguiseChanged();
	}

	default int getRotation(Direction face) {
		return getDisguise().getRotation(face);
	}

	default void setRotation(Direction face, int rotation) {
		getDisguise().setRotation(face, rotation);
		markDisguiseChanged();
	}

	default void setRotation(int rotation) {
		for (Direction face : Direction.values()) {
			getDisguise().setRotation(face, rotation);
		}
		markDisguiseChanged();
	}

	/** The disguise changed: save it with the chunk and send it to clients. */
	default void markDisguiseChanged() {
		setChanged();
		update();
	}

	/** Server: send the block entity to clients. Client: redraw the block. */
	default void update() {
		BlockEntity self = (BlockEntity) this;
		Level level = self.getLevel();
		if (!level.isClientSide()) {
			((ServerLevel) level).getChunkSource().blockChanged(self.getBlockPos());
		} else {
			// The level renderer marks the block's section dirty.
			level.sendBlockUpdated(self.getBlockPos(), self.getBlockState(), self.getBlockState(), Block.UPDATE_ALL);
		}
	}

	/** Call from {@link BlockEntity#setChanged()}: a saved change is also sent to clients. */
	static void sendOnChange(BlockEntity self) {
		if (self.hasLevel() && !self.getLevel().isClientSide()) {
			((ServerLevel) self.getLevel()).getChunkSource().blockChanged(self.getBlockPos());
		}
	}

	/** Call at the end of {@link BlockEntity#load}: a client redraws the block with the new disguise. */
	static void redrawAfterLoad(BlockEntity self) {
		Level level = self.getLevel();
		if (level != null && level.isClientSide()) {
			level.sendBlockUpdated(self.getBlockPos(), null, null, 0);
		}
	}
}
