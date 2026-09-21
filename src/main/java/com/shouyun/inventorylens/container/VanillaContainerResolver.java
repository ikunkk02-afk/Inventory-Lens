package com.shouyun.inventorylens.container;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;

/** Resolves only the hit block and its possible partner. Never reads client inventory data. */
public final class VanillaContainerResolver {
	private VanillaContainerResolver() {
	}

	@Nullable
	public static ResolvedContainer resolve(Level level, BlockPos position) {
		return resolve(level.dimension(), level, level::hasChunkAt, position);
	}

	@Nullable
	public static ResolvedContainer resolve(ResourceKey<Level> dimension, BlockGetter blocks,
			Predicate<BlockPos> loaded, BlockPos position) {
		if (!loaded.test(position)) {
			return null;
		}
		BlockState state = blocks.getBlockState(position);
		if (state.is(Blocks.BARREL)) {
			return new ResolvedContainer(new ContainerIdentity(dimension, position), ContainerType.BARREL,
					List.of(position), state.getValue(BarrelBlock.FACING));
		}
		if (!state.is(Blocks.CHEST)) {
			return null;
		}
		Direction facing = state.getValue(ChestBlock.FACING);
		ChestType chestType = state.getValue(ChestBlock.TYPE);
		if (chestType == ChestType.SINGLE) {
			return new ResolvedContainer(new ContainerIdentity(dimension, position), ContainerType.CHEST,
					List.of(position), facing);
		}
		BlockPos partner = position.relative(ChestBlock.getConnectedDirection(state));
		if (!loaded.test(partner)) {
			return null;
		}
		BlockState other = blocks.getBlockState(partner);
		if (!other.is(Blocks.CHEST) || other.getValue(ChestBlock.FACING) != facing
				|| other.getValue(ChestBlock.TYPE) != chestType.getOpposite()
				|| !partner.relative(ChestBlock.getConnectedDirection(other)).equals(position)) {
			return null;
		}
		// ChestBlock.getBlockType maps RIGHT to FIRST and LEFT to SECOND in CompoundContainer.
		List<BlockPos> members = chestType == ChestType.RIGHT ? List.of(position, partner) : List.of(partner, position);
		BlockPos canonical = position.compareTo(partner) <= 0 ? position : partner;
		return new ResolvedContainer(new ContainerIdentity(dimension, canonical), ContainerType.DOUBLE_CHEST,
				members, facing);
	}
}
