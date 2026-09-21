package com.shouyun.inventorylens.server;

import com.shouyun.inventorylens.container.ContainerProperties;
import com.shouyun.inventorylens.mixin.FurnaceDataAccessor;
import com.shouyun.inventorylens.mixin.CrafterDataAccessor;
import com.shouyun.inventorylens.mixin.BrewingDataAccessor;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.*;

/** Reads the same data slots that vanilla menus synchronize, only on the server thread. */
public final class ContainerPropertyReader {
    private ContainerPropertyReader() { }
    public static ContainerProperties read(BlockEntity entity) {
        if (entity instanceof AbstractFurnaceBlockEntity)
            return copy(ContainerProperties.Kind.FURNACE, ((FurnaceDataAccessor)entity).inventorylens$getFurnaceData());
        if (entity instanceof BrewingStandBlockEntity)
            return copy(ContainerProperties.Kind.BREWING, ((BrewingDataAccessor)entity).inventorylens$getBrewingData());
        if (entity instanceof CrafterBlockEntity)
            return copy(ContainerProperties.Kind.CRAFTER, ((CrafterDataAccessor)entity).inventorylens$getCrafterData());
        return new ContainerProperties.None();
    }
    public static ContainerProperties copy(ContainerProperties.Kind kind, ContainerData data) {
        return switch (kind) {
            case NONE -> new ContainerProperties.None();
            case FURNACE -> new ContainerProperties.Furnace(data.get(0), data.get(1), data.get(2), data.get(3));
            case BREWING -> new ContainerProperties.Brewing(data.get(0), data.get(1));
            case CRAFTER -> {
                int mask = 0;
                for (int slot = 0; slot < 9; slot++) if (data.get(slot) == 1) mask |= 1 << slot;
                yield new ContainerProperties.Crafter(mask, data.get(9) == 1);
            }
        };
    }
}
