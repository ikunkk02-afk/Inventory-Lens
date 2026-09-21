package com.shouyun.inventorylens.container;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Detached server copies. Consumers render these stacks and never mutate them. */
public record ContainerSnapshot(ResolvedContainer container, List<ItemStack> items,
        ResourceLocation gui, Component title, ContainerProperties properties) {
    public ContainerSnapshot(ResolvedContainer container, List<ItemStack> items) {
        this(container, items, container.type().gui(), Component.translatable(container.type().translationKey()),
                ContainerProperties.empty(container.type().propertiesKind()));
    }
    public ContainerSnapshot {
        if (items.size() != container.type().slots() || !gui.equals(container.type().gui())
                || properties.kind() != container.type().propertiesKind()) {
            throw new IllegalArgumentException("Snapshot does not match container definition");
        }
        items = items.stream().map(ItemStack::copy).toList();
        title = title.copy();
    }
}
