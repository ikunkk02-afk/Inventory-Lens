package com.shouyun.inventorylens.mixin;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(CrafterBlockEntity.class)
public interface CrafterDataAccessor {
    @Accessor("containerData") ContainerData inventorylens$getCrafterData();
}
