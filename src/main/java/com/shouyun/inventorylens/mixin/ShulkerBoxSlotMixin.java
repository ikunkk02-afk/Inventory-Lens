package com.shouyun.inventorylens.mixin;

import com.shouyun.inventorylens.container.ShulkerNestingInspector;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scope the exception to the actual shulker menu slot, on both logical sides. */
@Mixin(ShulkerBoxSlot.class)
public abstract class ShulkerBoxSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void inventorylens$allowBoundedShulker(ItemStack stack, CallbackInfoReturnable<Boolean> result) {
        if (ShulkerNestingInspector.isShulkerBox(stack)) {
            result.setReturnValue(ShulkerNestingInspector.mayInsertIntoWorldShulker(stack));
        }
    }
}
