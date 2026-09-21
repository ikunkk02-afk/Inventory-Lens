package com.shouyun.inventorylens.mixin;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Explicit read-only access to 1.21.1's vanilla menu data source, without constructing a menu. */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface FurnaceDataAccessor {
    @Accessor("dataAccess") ContainerData inventorylens$getFurnaceData();
}
