package com.shouyun.inventorylens.container;

import java.util.function.Function;
import net.minecraft.world.Container;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Request-local inventory, never a global/shared player cache. */
public record ContainerReadContext(BlockGetter blocks, @Nullable Container enderInventory,
        Function<BlockEntity, ContainerProperties> properties) { }
