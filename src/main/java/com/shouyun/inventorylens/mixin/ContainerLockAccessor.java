package com.shouyun.inventorylens.mixin;

import net.minecraft.world.LockCode;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read the vanilla permission without canOpen's sound and action-bar side effects. */
@Mixin(BaseContainerBlockEntity.class)
public interface ContainerLockAccessor {
	@Accessor("lockKey")
	LockCode inventorylens$getLockKey();
}
