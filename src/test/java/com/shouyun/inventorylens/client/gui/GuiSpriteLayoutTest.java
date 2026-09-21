package com.shouyun.inventorylens.client.gui;

import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiSpriteLayoutTest {
    private static ContainerGuiDefinition.SpriteElement element(int fw, int fh, int u, int v, int w, int h) {
        return new ContainerGuiDefinition.SpriteElement(ResourceLocation.withDefaultNamespace("test"), fw, fh, u, v, 10, 20, w, h);
    }
    @Test void flameCropsFromBottomRatherThanStretchingFullSprite() {
        var q = GuiSpriteLayout.quads(element(14, 14, 0, 6, 14, 8), new GuiSpriteScaling.Stretch()).getFirst();
        assertEquals(6F/14, q.v0()); assertEquals(1F, q.v1()); assertEquals(8, q.height());
    }
    @Test void resourcePackTileMetadataRepeatsAndClipsLastTile() {
        var quads = GuiSpriteLayout.quads(element(24, 16, 0, 0, 10, 7), new GuiSpriteScaling.Tile(4, 4));
        assertEquals(6, quads.size());
        assertEquals(70, quads.stream().mapToInt(q -> q.width()*q.height()).sum());
        assertEquals(0.5F, quads.getLast().u1()); assertEquals(0.75F, quads.getLast().v1());
    }
    @Test void nineSliceKeepsBordersAndCoversExactlyTheDestination() {
        var quads = GuiSpriteLayout.quads(element(18, 18, 0, 0, 23, 19),
                new GuiSpriteScaling.NineSlice(18, 18, new GuiSpriteScaling.NineSlice.Border(2, 2, 2, 2)));
        assertEquals(23*19, quads.stream().mapToInt(q -> q.width()*q.height()).sum());
        assertEquals(2, quads.getFirst().width()); assertEquals(2F/18, quads.getFirst().u1());
        for (var q : quads) {
            assertTrue(q.x() >= 10 && q.y() >= 20 && q.x()+q.width() <= 33 && q.y()+q.height() <= 39);
            assertTrue(q.u0() >= 0 && q.v0() >= 0 && q.u1() <= 1 && q.v1() <= 1);
        }
    }
}
