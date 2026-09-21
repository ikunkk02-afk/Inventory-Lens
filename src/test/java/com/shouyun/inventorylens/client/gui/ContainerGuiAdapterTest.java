package com.shouyun.inventorylens.client.gui;

import java.util.Collections;
import java.util.List;
import com.shouyun.inventorylens.TestWorld;
import com.shouyun.inventorylens.container.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContainerGuiAdapterTest {
    @BeforeAll static void bootstrap() { TestWorld.bootstrap(); }
    @Test void allSlotsFitTheirCroppedOriginalBackgroundsWithoutPlayerInventory() {
        for (var type : ContainerType.values()) {
            var gui = definition(type, ContainerProperties.empty(type.propertiesKind()));
            assertNotNull(gui, type.id().toString());
            assertEquals("minecraft", gui.texture().getNamespace());
            assertEquals(type.slots(), gui.slotLayout().size());
            assertTrue(gui.height() < gui.guiHeight());
            for (var slot : gui.slotLayout()) {
                assertTrue(slot.x() >= 0 && slot.y() >= 0);
                assertTrue(slot.x() + 16 <= gui.width());
                assertTrue(slot.y() + 16 <= gui.bodyHeight(), type.id() + " slot " + slot.index());
            }
        }
        assertEquals(132, definition(ContainerType.DOUBLE_CHEST, new ContainerProperties.None()).height());
        assertEquals(45, definition(ContainerType.HOPPER, new ContainerProperties.None()).height());
        assertEquals(new ContainerGuiDefinition.SlotPosition(0, 44, 20), definition(ContainerType.HOPPER, new ContainerProperties.None()).slotLayout().getFirst());
        assertEquals(definition(ContainerType.DISPENSER, new ContainerProperties.None()), definition(ContainerType.DROPPER, new ContainerProperties.None()));
    }
    @Test void furnaceUsesVanillaRoundingClampingAndZeroDurationFallback() {
        assertEquals(0, FurnaceGuiAdapter.flameHeight(new ContainerProperties.Furnace(0, 1600, 0, 200)));
        assertEquals(14, FurnaceGuiAdapter.flameHeight(new ContainerProperties.Furnace(200, 0, 0, 0)));
        assertEquals(8, FurnaceGuiAdapter.flameHeight(new ContainerProperties.Furnace(100, 200, 0, 0)));
        assertEquals(0, FurnaceGuiAdapter.arrowWidth(new ContainerProperties.Furnace(0, 0, 50, 0)));
        assertEquals(6, FurnaceGuiAdapter.arrowWidth(new ContainerProperties.Furnace(0, 0, 50, 200)));
        assertEquals(24, FurnaceGuiAdapter.arrowWidth(new ContainerProperties.Furnace(0, 0, 201, 200)));
        for (var type : List.of(ContainerType.FURNACE, ContainerType.BLAST_FURNACE, ContainerType.SMOKER)) {
            var gui = definition(type, new ContainerProperties.Furnace(100, 200, 50, 200));
            assertEquals(2, gui.progressElements().size());
            assertEquals("container/" + type.id().getPath() + "/lit_progress", gui.progressElements().getFirst().sprite().getPath());
            assertEquals(42, gui.progressElements().getFirst().y());
            assertEquals(6, gui.progressElements().get(1).width());
        }
    }
    @Test void brewingIncludesFuelProgressAndVanillaBubbleCycle() {
        assertTrue(definition(ContainerType.BREWING_STAND, new ContainerProperties.Brewing(0, 0)).progressElements().isEmpty());
        var elements = definition(ContainerType.BREWING_STAND, new ContainerProperties.Brewing(200, 10)).progressElements();
        assertEquals(3, elements.size());
        assertEquals(9, elements.get(0).width());
        assertEquals(14, elements.get(1).height());
        assertEquals(20, elements.get(2).height());
        assertEquals(18, definition(ContainerType.BREWING_STAND, new ContainerProperties.Brewing(0, Integer.MAX_VALUE)).progressElements().getFirst().width());
        assertTrue(definition(ContainerType.BREWING_STAND, new ContainerProperties.Brewing(-1, -1)).progressElements().isEmpty());
    }
    @Test void crafterReplacesOnlyDisabledSlotsAndUsesVanillaRedstoneSprites() {
        var gui = definition(ContainerType.CRAFTER, new ContainerProperties.Crafter(273, true));
        assertEquals(6, gui.slotLayout().size());
        assertTrue(gui.slotLayout().stream().noneMatch(s -> s.index() == 0 || s.index() == 4 || s.index() == 8));
        assertEquals(4, gui.progressElements().size());
        assertEquals("container/crafter/disabled_slot", gui.progressElements().getFirst().sprite().getPath());
        assertEquals(25, gui.progressElements().getFirst().x());
        assertEquals(16, gui.progressElements().getFirst().y());
        assertEquals("container/crafter/powered_redstone", gui.progressElements().getLast().sprite().getPath());
        assertEquals(97, gui.progressElements().getLast().x());
        assertEquals(35, gui.progressElements().getLast().y());
        assertEquals(0, definition(ContainerType.CRAFTER, new ContainerProperties.Crafter(511, false)).slotLayout().size());
    }
    static ContainerGuiDefinition definition(ContainerType type, ContainerProperties properties) {
        var target = new ResolvedContainer(new ContainerIdentity(Level.OVERWORLD, BlockPos.ZERO), type,
                type.memberCount() == 2 ? List.of(BlockPos.ZERO, BlockPos.ZERO.east()) : List.of(BlockPos.ZERO), Direction.NORTH);
        return ContainerGuiAdapterRegistry.definition(new ContainerSnapshot(target, Collections.nCopies(type.slots(), ItemStack.EMPTY),
                type.gui(), Component.translatable(type.translationKey()), properties));
    }
}
