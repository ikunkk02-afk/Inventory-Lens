package com.shouyun.inventorylens.client.interaction;

import com.shouyun.inventorylens.client.gui.ShulkerBoxGuiAdapter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldGuiHitTesterTest {
    @Test void mapsCameraRayToVanillaSlotCoordinates() {
        var gui = new ShulkerBoxGuiAdapter().define(null);
        double scale = 0.005;
        Vec3 center = new Vec3(0, 0, 2);
        double pixelX = 16, pixelY = 26;
        Vec3 target = center.add((pixelX - gui.width() / 2.0) * scale,
                -(pixelY - gui.height() / 2.0) * scale, 0);
        var hit = WorldGuiHitTester.hit(Vec3.ZERO, target.normalize(), center,
                new Quaternionf(), scale, gui);
        assertTrue(hit.inside());
        assertEquals(0, hit.slot());
        assertFalse(WorldGuiHitTester.hit(Vec3.ZERO, new Vec3(1, 0, 0), center,
                new Quaternionf(), scale, gui).inside());
    }
}
