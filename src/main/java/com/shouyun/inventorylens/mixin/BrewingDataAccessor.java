package com.shouyun.inventorylens.mixin;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BrewingStandBlockEntity.class)
public interface BrewingDataAccessor {
    @Accessor("dataAccess") ContainerData inventorylens$getBrewingData();
}
