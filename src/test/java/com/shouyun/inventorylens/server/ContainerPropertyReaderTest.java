package com.shouyun.inventorylens.server;

import com.shouyun.inventorylens.container.ContainerProperties;
import net.minecraft.world.inventory.SimpleContainerData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContainerPropertyReaderTest {
    @Test void furnaceCopiesAllFourVanillaDataIndices() {
        var data = new SimpleContainerData(4);
        data.set(0, 79); data.set(1, 1600); data.set(2, 50); data.set(3, 200);
        var snapshot = ContainerPropertyReader.copy(ContainerProperties.Kind.FURNACE, data);
        assertEquals(new ContainerProperties.Furnace(79, 1600, 50, 200), snapshot);
        data.set(2, 51);
        assertEquals(50, ((ContainerProperties.Furnace)snapshot).cookingProgress());
    }
    @Test void brewingCopiesRemainingTicksAndFuel() {
        var data = new SimpleContainerData(2); data.set(0, 213); data.set(1, 17);
        assertEquals(new ContainerProperties.Brewing(213, 17), ContainerPropertyReader.copy(ContainerProperties.Kind.BREWING, data));
    }
    @Test void crafterCopiesAllDisabledSlotsAndPoweredIndexNine() {
        var data = new SimpleContainerData(10);
        data.set(0, 1); data.set(4, 1); data.set(8, 1); data.set(9, 1);
        assertEquals(new ContainerProperties.Crafter(273, true), ContainerPropertyReader.copy(ContainerProperties.Kind.CRAFTER, data));
        data.set(9, 0);
        assertEquals(new ContainerProperties.Crafter(273, false), ContainerPropertyReader.copy(ContainerProperties.Kind.CRAFTER, data));
        assertThrows(IllegalArgumentException.class, () -> new ContainerProperties.Crafter(512, false));
    }
}
