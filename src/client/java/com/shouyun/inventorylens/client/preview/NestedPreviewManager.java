package com.shouyun.inventorylens.client.preview;

import java.util.ArrayList;
import java.util.List;
import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition;
import com.shouyun.inventorylens.client.gui.ShulkerBoxGuiAdapter;
import com.shouyun.inventorylens.client.interaction.WorldGuiHitTester;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.container.ShulkerNestingInspector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/** A stable slot path. Child contents are rebuilt only when their source stack changes. */
public final class NestedPreviewManager {
    public static final long OPEN_DELAY_MS = 200;
    public static final long CLOSE_DELAY_MS = 160;
    private static final int GAP_PIXELS = 12;
    private static final ContainerGuiDefinition SHULKER_GUI = new ShulkerBoxGuiAdapter().define(null);
    private final List<Node> path = new ArrayList<>();
    private Object rootIdentity;
    private int candidatePanel = -1, candidateSlot = -1;
    private long candidateSince, outsideSince = -1;
    private boolean focused;

    public record Panel(int level, Vec3 center, ContainerGuiDefinition gui,
            List<ItemStack> items, Component title, int hoveredSlot) { }
    private record Node(int parentLevel, int sourceSlot, ItemStack reference, ItemStack source,
            List<ItemStack> items, Component title) { }

    public List<Panel> update(ContainerSnapshot root, ContainerGuiDefinition rootGui, Vec3 center,
            Quaternionfc rotation, double pixelScale, Vec3 eye, Vec3 look, long now) {
		if (!root.container().equals(rootIdentity)) {
            reset();
			rootIdentity = root.container();
        }
        reconcile(root);
        List<Panel> panels = panels(root, rootGui, center, rotation, pixelScale);
        int active = -1;
        WorldGuiHitTester.Hit hit = WorldGuiHitTester.Hit.NONE;
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            WorldGuiHitTester.Hit result = WorldGuiHitTester.hit(eye, look, panel.center(), rotation, pixelScale, panel.gui());
            if (result.inside()) { active = i; hit = result; break; }
        }
        focused = active >= 0;
        if (focused) {
            outsideSince = -1;
            if (active < path.size()) {
                Node child = path.get(active);
                if (hit.slot() >= 0 && hit.slot() != child.sourceSlot()) trim(active);
            } else if (active > 0) {
                trim(active);
            }
            List<ItemStack> current = active == 0 ? root.items() : path.get(active - 1).items();
            int slot = hit.slot();
            if (slot >= 0 && slot < current.size() && active < ShulkerNestingInspector.MAX_PREVIEW_DEPTH
                    && ShulkerNestingInspector.isShulkerBox(current.get(slot))) {
                boolean alreadyOpen = active < path.size() && path.get(active).sourceSlot() == slot;
                if (!alreadyOpen) {
                    if (candidatePanel != active || candidateSlot != slot) {
                        candidatePanel = active; candidateSlot = slot; candidateSince = now;
                    } else if (now - candidateSince >= OPEN_DELAY_MS) {
                        trim(active);
                        ItemStack source = current.get(slot);
                        if (ShulkerNestingInspector.validateDepth(source, ShulkerNestingInspector.MAX_NESTING_DEPTH)) {
                            path.add(new Node(active, slot, source, source.copy(),
                                    ShulkerNestingInspector.getContents(source), source.getHoverName()));
                        }
                        candidatePanel = candidateSlot = -1;
                    }
                }
            } else {
                candidatePanel = candidateSlot = -1;
            }
        } else {
            candidatePanel = candidateSlot = -1;
            if (outsideSince < 0) outsideSince = now;
            if (now - outsideSince >= CLOSE_DELAY_MS) trim(0);
        }
        panels = panels(root, rootGui, center, rotation, pixelScale);
        if (active >= 0 && active < panels.size()) {
            Panel old = panels.get(active);
            panels.set(active, new Panel(old.level(), old.center(), old.gui(), old.items(), old.title(), hit.slot()));
        }
        return panels;
    }

    private void reconcile(ContainerSnapshot root) {
        List<ItemStack> parent = root.items();
        for (int i = 0; i < path.size(); i++) {
            Node node = path.get(i);
            if (node.sourceSlot() >= parent.size() || !ShulkerNestingInspector.isShulkerBox(parent.get(node.sourceSlot()))) {
                trim(i); return;
            }
            ItemStack current = parent.get(node.sourceSlot());
            if (current != node.reference()) {
                if (!ShulkerNestingInspector.validateDepth(current, ShulkerNestingInspector.MAX_NESTING_DEPTH)) {
                    trim(i); return;
                }
                if (!ItemStack.matches(current, node.source())) {
                    Node updated = new Node(i, node.sourceSlot(), current, current.copy(),
                            ShulkerNestingInspector.getContents(current), current.getHoverName());
                    path.set(i, updated);
                    trim(i + 1);
                    parent = updated.items();
                } else {
                    path.set(i, new Node(i, node.sourceSlot(), current, node.source(), node.items(), node.title()));
                    parent = node.items();
                }
            } else parent = node.items();
        }
    }

    private List<Panel> panels(ContainerSnapshot root, ContainerGuiDefinition rootGui, Vec3 center,
            Quaternionfc rotation, double pixelScale) {
        List<Panel> panels = new ArrayList<>();
        panels.add(new Panel(0, center, rootGui, root.items(), root.title(), -1));
        for (int level = 1; level <= path.size(); level++) {
            Node node = path.get(level - 1);
            // Two columns bound the horizontal span even at the fifth child.
            int row = level / 2;
            int column = row % 2 == 0 ? level % 2 : 1 - level % 2;
            Vector3f right = rotation.transform(new Vector3f(1, 0, 0));
            Vector3f down = rotation.transform(new Vector3f(0, -1, 0));
            Vector3f normal = rotation.transform(new Vector3f(0, 0, 1));
            double x = column * (rootGui.width() + GAP_PIXELS) * pixelScale;
            double y = row * (Math.max(rootGui.height(), SHULKER_GUI.height()) + GAP_PIXELS) * pixelScale;
            Vec3 location = center.add(right.x * x + down.x * y + normal.x * level * 0.0001,
                    right.y * x + down.y * y + normal.y * level * 0.0001,
                    right.z * x + down.z * y + normal.z * level * 0.0001);
            panels.add(new Panel(level, location, SHULKER_GUI, node.items(), node.title(), -1));
        }
        return panels;
    }

    private void trim(int size) { while (path.size() > size) path.removeLast(); }
    public boolean focused() { return focused; }
    public boolean retainsTarget() { return focused || !path.isEmpty(); }
    public void reset() { path.clear(); rootIdentity = null; candidatePanel = candidateSlot = -1; outsideSince = -1; focused = false; }
}
