package com.shouyun.inventorylens.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition.SpriteElement;

/** GuiGraphics.blitSprite scaling semantics, emitted as depth-tested world quads instead of screen blits. */
public final class GuiSpriteLayout {
    private GuiSpriteLayout() { }
    public record Quad(int x, int y, int width, int height, float u0, float v0, float u1, float v1) { }
    public static List<Quad> quads(SpriteElement e, GuiSpriteScaling scaling) {
        List<Quad> result = new ArrayList<>();
        if (e.width() <= 0 || e.height() <= 0) return result;
        if (scaling instanceof GuiSpriteScaling.Stretch) {
            result.add(new Quad(e.x(), e.y(), e.width(), e.height(), (float)e.u() / e.fullWidth(),
                    (float)e.v() / e.fullHeight(), (float)(e.u() + e.width()) / e.fullWidth(),
                    (float)(e.v() + e.height()) / e.fullHeight()));
        } else if (scaling instanceof GuiSpriteScaling.Tile tile) {
            tile(result, e.x(), e.y(), e.width(), e.height(), 0, 0,
                    tile.width(), tile.height(), tile.width(), tile.height());
        } else if (scaling instanceof GuiSpriteScaling.NineSlice nine) {
            // Like vanilla, non-stretch metadata uses the destination rectangle, not progress UV cropping.
            int left = e.width() == nine.width() ? 0 : Math.min(nine.border().left(), e.width() / 2);
            int right = e.width() == nine.width() ? 0 : Math.min(nine.border().right(), e.width() / 2);
            int top = e.height() == nine.height() ? 0 : Math.min(nine.border().top(), e.height() / 2);
            int bottom = e.height() == nine.height() ? 0 : Math.min(nine.border().bottom(), e.height() / 2);
            int[] dx = {0, left, e.width() - right, e.width()};
            int[] dy = {0, top, e.height() - bottom, e.height()};
            int[] sx = {0, left, nine.width() - right, nine.width()};
            int[] sy = {0, top, nine.height() - bottom, nine.height()};
            for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) {
                tile(result, e.x() + dx[x], e.y() + dy[y], dx[x + 1] - dx[x], dy[y + 1] - dy[y],
                        sx[x], sy[y], sx[x + 1] - sx[x], sy[y + 1] - sy[y], nine.width(), nine.height());
            }
        }
        return result;
    }
    private static void tile(List<Quad> out, int x, int y, int width, int height,
            int u, int v, int tileWidth, int tileHeight, int fullWidth, int fullHeight) {
        if (width <= 0 || height <= 0 || tileWidth <= 0 || tileHeight <= 0) return;
        for (int dx = 0; dx < width; dx += tileWidth) for (int dy = 0; dy < height; dy += tileHeight) {
            int w = Math.min(tileWidth, width - dx), h = Math.min(tileHeight, height - dy);
            out.add(new Quad(x + dx, y + dy, w, h, (float)u / fullWidth, (float)v / fullHeight,
                    (float)(u + w) / fullWidth, (float)(v + h) / fullHeight));
        }
    }
}
