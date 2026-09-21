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
public final class VanillaContainerResolver implements ContainerResolver {
	private static final java.util.Map<net.minecraft.world.level.block.Block, ContainerType> SIMPLE = new java.util.HashMap<>();
    static {
        SIMPLE.put(Blocks.CRAFTER, ContainerType.CRAFTER);
        SIMPLE.put(Blocks.BREWING_STAND, ContainerType.BREWING_STAND);
        SIMPLE.put(Blocks.BLAST_FURNACE, ContainerType.BLAST_FURNACE);
        SIMPLE.put(Blocks.SMOKER, ContainerType.SMOKER);
        SIMPLE.put(Blocks.FURNACE, ContainerType.FURNACE);
        SIMPLE.put(Blocks.ENDER_CHEST, ContainerType.ENDER_CHEST);
        SIMPLE.put(Blocks.DISPENSER, ContainerType.DISPENSER);
        SIMPLE.put(Blocks.DROPPER, ContainerType.DROPPER);
        SIMPLE.put(Blocks.HOPPER, ContainerType.HOPPER);
        for (var block : java.util.List.of(Blocks.SHULKER_BOX,Blocks.WHITE_SHULKER_BOX,Blocks.ORANGE_SHULKER_BOX,Blocks.MAGENTA_SHULKER_BOX,Blocks.LIGHT_BLUE_SHULKER_BOX,Blocks.YELLOW_SHULKER_BOX,Blocks.LIME_SHULKER_BOX,Blocks.PINK_SHULKER_BOX,Blocks.GRAY_SHULKER_BOX,Blocks.LIGHT_GRAY_SHULKER_BOX,Blocks.CYAN_SHULKER_BOX,Blocks.PURPLE_SHULKER_BOX,Blocks.BLUE_SHULKER_BOX,Blocks.BROWN_SHULKER_BOX,Blocks.GREEN_SHULKER_BOX,Blocks.RED_SHULKER_BOX,Blocks.BLACK_SHULKER_BOX)) SIMPLE.put(block, ContainerType.SHULKER_BOX);
    }

    public VanillaContainerResolver() {
	}

    @Override public ResolvedContainer find(ResourceKey<Level> dimension, BlockGetter blocks,
            Predicate<BlockPos> loaded, BlockPos position) { return resolve(dimension, blocks, loaded, position); }
    @Override public boolean supports(ContainerType type) {
        return SIMPLE.containsValue(type) || type == ContainerType.CHEST || type == ContainerType.DOUBLE_CHEST || type == ContainerType.BARREL
                || type == ContainerType.TRAPPED_CHEST || type == ContainerType.DOUBLE_TRAPPED_CHEST;
    }
    @Override public ContainerSnapshot read(ResolvedContainer target, ContainerReadContext context) {
        return VanillaContainerReader.read(target, context);
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
        ContainerType simpleType = SIMPLE.get(state.getBlock());
        if (simpleType != null) {
            Direction direction = Direction.NORTH;
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING))
                direction = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
            else if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING))
                direction = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
            if (state.is(Blocks.HOPPER)) direction = state.getValue(net.minecraft.world.level.block.HopperBlock.FACING);
            if (state.is(Blocks.CRAFTER)) direction = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ORIENTATION).front();
            return new ResolvedContainer(new ContainerIdentity(dimension, position), simpleType, List.of(position), direction);
        }
		if (state.is(Blocks.BARREL)) {
			return new ResolvedContainer(new ContainerIdentity(dimension, position), ContainerType.BARREL,
					List.of(position), state.getValue(BarrelBlock.FACING));
		}
		if (!(state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST))) {
			return null;
		}
		Direction facing = state.getValue(ChestBlock.FACING);
		ChestType chestType = state.getValue(ChestBlock.TYPE);
		if (chestType == ChestType.SINGLE) {
			return new ResolvedContainer(new ContainerIdentity(dimension, position), (state.is(Blocks.TRAPPED_CHEST) ? ContainerType.TRAPPED_CHEST : ContainerType.CHEST),
					List.of(position), facing);
		}
		BlockPos partner = position.relative(ChestBlock.getConnectedDirection(state));
		if (!loaded.test(partner)) {
			return null;
		}
		BlockState other = blocks.getBlockState(partner);
		if (!other.is(state.getBlock()) || other.getValue(ChestBlock.FACING) != facing
				|| other.getValue(ChestBlock.TYPE) != chestType.getOpposite()
				|| !partner.relative(ChestBlock.getConnectedDirection(other)).equals(position)) {
			return null;
		}
		// ChestBlock.getBlockType maps RIGHT to FIRST and LEFT to SECOND in CompoundContainer.
		List<BlockPos> members = chestType == ChestType.RIGHT ? List.of(position, partner) : List.of(partner, position);
		BlockPos canonical = position.compareTo(partner) <= 0 ? position : partner;
		return new ResolvedContainer(new ContainerIdentity(dimension, canonical), (state.is(Blocks.TRAPPED_CHEST) ? ContainerType.DOUBLE_TRAPPED_CHEST : ContainerType.DOUBLE_CHEST),
				members, facing);
	}
}
