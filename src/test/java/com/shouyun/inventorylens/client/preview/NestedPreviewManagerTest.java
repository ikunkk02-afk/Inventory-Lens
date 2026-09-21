package com.shouyun.inventorylens.client.preview;

import java.util.ArrayList;
import java.util.List;
import com.shouyun.inventorylens.TestWorld;
import com.shouyun.inventorylens.client.gui.ContainerGuiAdapterRegistry;
import com.shouyun.inventorylens.container.ContainerIdentity;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.container.ContainerType;
import com.shouyun.inventorylens.container.ResolvedContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NestedPreviewManagerTest {
    @BeforeAll static void initialize() { TestWorld.bootstrap(); }

    private static ContainerSnapshot snapshot(ItemStack stack) {
        var pos = new BlockPos(0, 64, 0);
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 27; i++) items.add(ItemStack.EMPTY);
        items.set(0, stack);
        return new ContainerSnapshot(new ResolvedContainer(new ContainerIdentity(Level.OVERWORLD, pos),
                ContainerType.CHEST, List.of(pos), Direction.NORTH), items);
    }

    private static Vec3 toward(double worldX, double worldY) {
        return new Vec3(worldX, worldY, 2).normalize();
    }

    @Test void delayFocusPathAndRemovedSource() {
        ItemStack inner = new ItemStack(Items.RED_SHULKER_BOX);
        var slots = new ArrayList<ItemStack>();
        for (int i = 0; i < 27; i++) slots.add(ItemStack.EMPTY);
        slots.set(0, inner);
        ItemStack outer = new ItemStack(Items.PURPLE_SHULKER_BOX);
        outer.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        var root = snapshot(outer);
        var gui = ContainerGuiAdapterRegistry.definition(root);
        var manager = new NestedPreviewManager();
        Vec3 center = new Vec3(0, 0, 2);
        Quaternionf rotation = new Quaternionf();
        double scale = 0.005;
        // Root first slot at pixel (16, 26), relative to the panel center.
        Vec3 rootSlot = toward((16 - gui.width() / 2.0) * scale, -(26 - gui.height() / 2.0) * scale);
        assertEquals(1, manager.update(root, gui, center, rotation, scale, Vec3.ZERO, rootSlot, 0).size());
        assertEquals(1, manager.update(root, gui, center, rotation, scale, Vec3.ZERO, rootSlot, 199).size());
        assertEquals(2, manager.update(root, gui, center, rotation, scale, Vec3.ZERO, rootSlot, 200).size());
        // Crossing the 12-pixel gap retains the child until it can be aimed at.
        manager.update(root, gui, center, rotation, scale, Vec3.ZERO, toward(0.48, 0), 220);
        assertTrue(manager.retainsTarget());
        Vec3 childSlot = toward((gui.width() + 12 + 16 - gui.width() / 2.0) * scale,
                -(26 - gui.height() / 2.0) * scale);
        assertEquals(2, manager.update(root, gui, center, rotation, scale, Vec3.ZERO, childSlot, 250).size());
        assertEquals(3, manager.update(root, gui, center, rotation, scale, Vec3.ZERO, childSlot, 450).size());
        assertEquals(1, manager.update(snapshot(ItemStack.EMPTY), gui, center, rotation, scale,
                Vec3.ZERO, childSlot, 500).size());
    }
}
