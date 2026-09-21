package com.shouyun.inventorylens.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Draws only adapter-provided geometry, never container-specific coordinates or state. */
public final class WorldContainerGuiRenderer {
    public void render(Minecraft minecraft, PoseStack pose, WorldItemRenderer items,
            ContainerSnapshot snapshot, ContainerGuiDefinition gui) {
        render(minecraft, pose, items, snapshot.items(), snapshot.title(),
                snapshot.container().identity().position().hashCode(), gui, -1);
    }

    public void render(Minecraft minecraft, PoseStack pose, WorldItemRenderer items,
            List<ItemStack> stacks, Component title, int seed, ContainerGuiDefinition gui, int hoveredSlot) {
        items.blit(pose, gui.texture(), 0, 0, gui.width(), gui.bodyHeight(), 0, 0, 256, 256, 0);
        items.blit(pose, gui.texture(), 0, gui.bodyHeight(), gui.width(), gui.footerHeight(),
                0, gui.footerV(), 256, 256, 0);
        items.flush();
        for (var element : gui.progressElements()) {
            items.guiSprite(minecraft, pose, element, 0.02F);
        }
        items.flush();
        int titleX = gui.centeredTitle() ? (gui.width() - minecraft.font.width(title)) / 2 : gui.titleX();
        items.label(minecraft, pose, title, titleX, gui.titleY(), 0xFF404040);
        for (var slot : gui.slotLayout()) {
            var stack = stacks.get(slot.index());
            if (!stack.isEmpty()) items.render(minecraft, pose, stack, minecraft.level, null,
                    slot.x(), slot.y(), seed + slot.index());
        }
        if (hoveredSlot >= 0) {
            for (var slot : gui.slotLayout()) {
                if (slot.index() == hoveredSlot) {
                    items.fill(pose, slot.x(), slot.y(), slot.x() + 16, slot.y() + 16, 0.42F, 0x80FFFFFF);
                    break;
                }
            }
        }
        items.flush();
    }
}
