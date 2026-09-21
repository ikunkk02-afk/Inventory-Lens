package com.shouyun.inventorylens.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import net.minecraft.client.Minecraft;

/** Draws only adapter-provided geometry, never container-specific coordinates or state. */
public final class WorldContainerGuiRenderer {
    public void render(Minecraft minecraft, PoseStack pose, WorldItemRenderer items,
            ContainerSnapshot snapshot, ContainerGuiDefinition gui) {
        items.blit(pose, gui.texture(), 0, 0, gui.width(), gui.bodyHeight(), 0, 0, 256, 256, 0);
        items.blit(pose, gui.texture(), 0, gui.bodyHeight(), gui.width(), gui.footerHeight(),
                0, gui.footerV(), 256, 256, 0);
        items.flush();
        for (var element : gui.progressElements()) {
            items.guiSprite(minecraft, pose, element, 0.02F);
        }
        items.flush();
        int titleX = gui.centeredTitle() ? (gui.width() - minecraft.font.width(snapshot.title())) / 2 : gui.titleX();
        items.label(minecraft, pose, snapshot.title(), titleX, gui.titleY(), 0xFF404040);
        int seed = snapshot.container().identity().position().hashCode();
        for (var slot : gui.slotLayout()) {
            var stack = snapshot.items().get(slot.index());
            if (!stack.isEmpty()) items.render(minecraft, pose, stack, minecraft.level, null,
                    slot.x(), slot.y(), seed + slot.index());
        }
        items.flush();
    }
}
