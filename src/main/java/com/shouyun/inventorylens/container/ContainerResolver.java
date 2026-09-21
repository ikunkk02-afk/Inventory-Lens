package com.shouyun.inventorylens.container;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** A resolver owns recognition and reading; the provider always validates access first. */
public interface ContainerResolver {
    @Nullable ResolvedContainer find(ResourceKey<Level> dimension, BlockGetter blocks, Predicate<BlockPos> loaded, BlockPos position);
    boolean supports(ContainerType type);
    @Nullable ContainerSnapshot read(ResolvedContainer target, ContainerReadContext context);
}
