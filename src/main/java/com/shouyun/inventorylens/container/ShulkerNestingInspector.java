package com.shouyun.inventorylens.container;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Bounded inspection of vanilla container components. Never mutates an input stack. */
public final class ShulkerNestingInspector {
    public static final int MAX_NESTING_DEPTH = 8;
    public static final int MAX_PREVIEW_DEPTH = 5;
    public static final int MAX_VISITED_STACKS = 4096;
    private ShulkerNestingInspector() { }

    public static boolean isShulkerBox(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem block
                && block.getBlock() instanceof ShulkerBoxBlock;
    }

    public static NonNullList<ItemStack> getContents(ItemStack stack) {
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        if (isShulkerBox(stack)) {
            stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        }
        return contents;
    }

    /** Returns -1 for malformed, cyclic or over-budget input. A shulker itself has depth one. */
    public static int getNestingDepth(ItemStack stack) {
        return depth(stack, 0, new Budget());
    }

    public static boolean containsNestedShulker(ItemStack stack) {
        return getNestingDepth(stack) > 1;
    }

    public static boolean validateDepth(ItemStack stack, int maxDepth) {
        int depth = getNestingDepth(stack);
        return depth >= 0 && depth <= maxDepth;
    }

    /** The world shulker is one additional level around the inserted item. */
    public static boolean mayInsertIntoWorldShulker(ItemStack stack) {
        return isShulkerBox(stack) && validateDepth(stack, MAX_NESTING_DEPTH - 1);
    }

    private static int depth(ItemStack stack, int parentDepth, Budget budget) {
        if (parentDepth > MAX_NESTING_DEPTH) return -1;
        if (!isShulkerBox(stack)) return 0;
        if (!budget.active.add(stack)) return -1;
        try {
            int maximum = 1;
            // Even a malformed component has a hard traversal bound.
            for (ItemStack child : getContents(stack)) {
                if (++budget.visited > MAX_VISITED_STACKS) return -1;
                if (!isShulkerBox(child)) continue;
                int childDepth = depth(child, parentDepth + 1, budget);
                if (childDepth < 0) return -1;
                maximum = Math.max(maximum, 1 + childDepth);
            }
            return maximum;
        } finally {
            budget.active.remove(stack);
        }
    }

    private static final class Budget {
        int visited;
        final Set<ItemStack> active = Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
