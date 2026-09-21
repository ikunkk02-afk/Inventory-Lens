package com.shouyun.inventorylens.client.interaction;

import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/** Camera ray against the exact pixel plane used by the world renderer. */
public final class WorldGuiHitTester {
    private WorldGuiHitTester() { }
    public record Hit(boolean inside, int slot, double x, double y) {
        public static final Hit NONE = new Hit(false, -1, 0, 0);
    }

    public static Hit hit(Vec3 eye, Vec3 look, Vec3 center, Quaternionfc rotation,
            double pixelScale, ContainerGuiDefinition gui) {
        if (pixelScale <= 0) return Hit.NONE;
        Vector3f axis = new Vector3f();
        rotation.transform(axis.set(0, 0, 1));
        Vec3 normal = new Vec3(axis.x, axis.y, axis.z);
        double denominator = look.dot(normal);
        if (Math.abs(denominator) < 1.0e-7) return Hit.NONE;
        double distance = center.subtract(eye).dot(normal) / denominator;
        if (distance <= 0 || distance > 6) return Hit.NONE;
        Vec3 offset = eye.add(look.scale(distance)).subtract(center);
        rotation.transform(axis.set(1, 0, 0));
        double x = offset.dot(new Vec3(axis.x, axis.y, axis.z)) / pixelScale + gui.width() / 2.0;
        rotation.transform(axis.set(0, 1, 0));
        double y = -offset.dot(new Vec3(axis.x, axis.y, axis.z)) / pixelScale + gui.height() / 2.0;
        if (x < 0 || x >= gui.width() || y < 0 || y >= gui.height()) return Hit.NONE;
        for (var slot : gui.slotLayout()) {
            if (x >= slot.x() && x < slot.x() + 16 && y >= slot.y() && y < slot.y() + 16)
                return new Hit(true, slot.index(), x, y);
        }
        return new Hit(true, -1, x, y);
    }
}
