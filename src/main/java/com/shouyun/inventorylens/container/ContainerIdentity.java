package com.shouyun.inventorylens.container;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record ContainerIdentity(ResourceKey<Level> dimension, BlockPos position) {
	public ContainerIdentity {
		position = position.immutable();
	}
}
