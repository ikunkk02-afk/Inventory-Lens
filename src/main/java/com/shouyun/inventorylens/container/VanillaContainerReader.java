package com.shouyun.inventorylens.container;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;

/** Called only after all members pass loaded-chunk, sight, lock and loot checks. */
final class VanillaContainerReader {
    private static final java.util.Map<ContainerType, Class<? extends BlockEntity>> ENTITIES = new java.util.HashMap<>();
    static {
        ENTITIES.put(ContainerType.CRAFTER, CrafterBlockEntity.class);
        ENTITIES.put(ContainerType.BREWING_STAND, BrewingStandBlockEntity.class);
        ENTITIES.put(ContainerType.BLAST_FURNACE, BlastFurnaceBlockEntity.class);
        ENTITIES.put(ContainerType.SMOKER, SmokerBlockEntity.class);
        ENTITIES.put(ContainerType.FURNACE, FurnaceBlockEntity.class);
        ENTITIES.put(ContainerType.ENDER_CHEST, EnderChestBlockEntity.class);
        ENTITIES.put(ContainerType.DISPENSER, DispenserBlockEntity.class);
        ENTITIES.put(ContainerType.DROPPER, DropperBlockEntity.class);
        ENTITIES.put(ContainerType.HOPPER, HopperBlockEntity.class);
        ENTITIES.put(ContainerType.BARREL, BarrelBlockEntity.class);
        ENTITIES.put(ContainerType.CHEST, ChestBlockEntity.class);
        ENTITIES.put(ContainerType.DOUBLE_CHEST, ChestBlockEntity.class);
        ENTITIES.put(ContainerType.TRAPPED_CHEST, TrappedChestBlockEntity.class);
        ENTITIES.put(ContainerType.DOUBLE_TRAPPED_CHEST, TrappedChestBlockEntity.class);
        ENTITIES.put(ContainerType.SHULKER_BOX, ShulkerBoxBlockEntity.class);
    }
    private VanillaContainerReader() { }
    static ContainerSnapshot read(ResolvedContainer target, ContainerReadContext context) {
        List<Container> members = new ArrayList<>();
        Component title = Component.translatable(target.type().translationKey());
        boolean named = false;
        for (var position : target.members()) {
            BlockEntity entity = context.blocks().getBlockEntity(position);
            boolean expected = ENTITIES.containsKey(target.type()) && ENTITIES.get(target.type()).isInstance(entity);
            if (!expected) return null;
            Container inventory = target.type() == ContainerType.ENDER_CHEST ? context.enderInventory()
                    : entity instanceof Container c ? c : null;
            if (inventory == null) return null;
            members.add(inventory);
            if (!named && entity instanceof BaseContainerBlockEntity base && base.hasCustomName()) {
                title = base.getCustomName(); named = true;
            }
        }
        Container inventory = members.size() == 2 ? new CompoundContainer(members.get(0), members.get(1)) : members.getFirst();
        if (inventory.getContainerSize() != target.type().slots()) return null;
        List<ItemStack> items = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) items.add(inventory.getItem(slot));
        return new ContainerSnapshot(target, items, target.type().gui(), title,
                context.properties().apply(context.blocks().getBlockEntity(target.members().getFirst())));
    }
}
