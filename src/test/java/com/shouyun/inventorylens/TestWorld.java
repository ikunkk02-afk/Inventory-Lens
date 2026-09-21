package com.shouyun.inventorylens;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** Real vanilla block shapes, with an in-memory world and explicitly observable access. */
public final class TestWorld implements BlockGetter {
	static {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	public final Map<BlockPos, BlockState> states = new HashMap<>();
	public final Map<BlockPos, BlockEntity> entities = new HashMap<>();
	public final Set<BlockPos> reads = new HashSet<>();
	public final Set<BlockPos> unloaded = new HashSet<>();
	public int inventoryLookups;

	public static void bootstrap() {
		// Loading this fixture initializes the vanilla registries exactly once.
	}

	public boolean loaded(BlockPos pos) {
		return !unloaded.contains(pos);
	}

	public void put(BlockEntity entity) {
		states.put(entity.getBlockPos(), entity.getBlockState());
		entities.put(entity.getBlockPos(), entity);
	}

	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		inventoryLookups++;
		return entities.get(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		if (!loaded(pos)) {
			throw new AssertionError("Read from unloaded position " + pos);
		}
		reads.add(pos.immutable());
		return states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}

	@Override
	public int getHeight() {
		return 384;
	}

	@Override
	public int getMinBuildHeight() {
		return -64;
	}
}
