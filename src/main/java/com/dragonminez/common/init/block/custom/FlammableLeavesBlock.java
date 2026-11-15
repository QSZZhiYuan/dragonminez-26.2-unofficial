package com.dragonminez.common.init.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public class FlammableLeavesBlock extends LeavesBlock {

	/**
	 * 26.2: {@code LeavesBlock} became abstract. It now requires a leaf-particle chance in the
	 * constructor and an implementation of {@code spawnFallingLeavesParticle}; the chance below is
	 * the one vanilla's own leaf blocks use.
	 */
	public FlammableLeavesBlock(Properties properties) {
		super(0.005F, properties);
	}

	/** 26.2: LeavesBlock declares {@code codec()} abstract. */
	public static final com.mojang.serialization.MapCodec<FlammableLeavesBlock> CODEC =
			simpleCodec(FlammableLeavesBlock::new);

	@Override
	public com.mojang.serialization.MapCodec<? extends LeavesBlock> codec() {
		return CODEC;
	}

	@Override
	protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
		// The DMZ leaf blocks do not emit falling-leaf particles.
	}

	@Override
	public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return true;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return 60;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return 5;
	}
}
