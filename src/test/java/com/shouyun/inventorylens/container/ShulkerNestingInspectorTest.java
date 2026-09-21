package com.shouyun.inventorylens.container;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import com.shouyun.inventorylens.TestWorld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShulkerNestingInspectorTest {
    @BeforeAll static void initialize() { TestWorld.bootstrap(); }

    private static ItemStack containing(ItemStack child) {
        ItemStack box = new ItemStack(Items.PURPLE_SHULKER_BOX);
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < 27; i++) contents.add(ItemStack.EMPTY);
        contents.set(12, child);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return box;
    }

    @Test void readsTwentySevenSlotsAndPreservesComponents() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 32);
        diamonds.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
        var contents = ShulkerNestingInspector.getContents(containing(diamonds));
        assertEquals(27, contents.size());
        assertTrue(contents.get(0).isEmpty());
        assertEquals(32, contents.get(12).getCount());
        assertEquals(Component.literal("Keepsake"), contents.get(12).get(DataComponents.CUSTOM_NAME));
        assertTrue(ShulkerNestingInspector.getContents(new ItemStack(Items.SHULKER_BOX)).stream().allMatch(ItemStack::isEmpty));
    }

    @Test void acceptsEightLayersAndRejectsNinth() {
        ItemStack nested = new ItemStack(Items.SHULKER_BOX);
        for (int i = 1; i < 8; i++) nested = containing(nested);
        assertEquals(8, ShulkerNestingInspector.getNestingDepth(nested));
        assertFalse(ShulkerNestingInspector.mayInsertIntoWorldShulker(nested));
        assertTrue(ShulkerNestingInspector.mayInsertIntoWorldShulker(
                ShulkerNestingInspector.getContents(nested).get(12)));
        assertFalse(ShulkerNestingInspector.isShulkerBox(new ItemStack(Items.DIAMOND)));
    }
}
