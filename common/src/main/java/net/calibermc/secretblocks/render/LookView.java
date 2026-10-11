package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/**
 * The world as a disguise's own model sees it: every secret block reads as
 * the look of its face toward {@code face} (the side being drawn), so models
 * that look at their neighbours (connected textures) treat a wall of secret
 * glass, or secret glass in a real glass wall, as glass. Everything else is
 * the world itself.
 */
final class LookView implements BlockAndTintGetter {

	private final BlockAndTintGetter world;
	private final Direction face;

	LookView(BlockAndTintGetter world, Direction face) {
		this.world = world;
		this.face = face;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return DisguiseLook.looks(world, pos, world.getBlockState(pos), face);
	}

	@Override
	@Nullable
	public BlockEntity getBlockEntity(BlockPos pos) {
		return world.getBlockEntity(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return world.getFluidState(pos);
	}

	@Override
	public float getShade(Direction direction, boolean shade) {
		return world.getShade(direction, shade);
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return world.getLightEngine();
	}

	@Override
	public int getBlockTint(BlockPos pos, ColorResolver resolver) {
		return world.getBlockTint(pos, resolver);
	}

	@Override
	public int getHeight() {
		return world.getHeight();
	}

	@Override
	public int getMinBuildHeight() {
		return world.getMinBuildHeight();
	}
}
