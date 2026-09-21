package com.shouyun.inventorylens.container;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/** Detached server copies. Consumers only render these stacks; they never mutate them. */
public record ContainerSnapshot(ResolvedContainer container, List<ItemStack> items) {
	public ContainerSnapshot {
		if (items.size() != container.type().slots()) {
			throw new IllegalArgumentException("Snapshot slot count does not match container type");
		}
		items = items.stream().map(ItemStack::copy).toList();
	}
}
