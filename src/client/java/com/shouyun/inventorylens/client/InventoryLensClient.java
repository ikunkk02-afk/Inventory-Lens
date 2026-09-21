package com.shouyun.inventorylens.client;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import com.shouyun.inventorylens.client.animation.WorldUiAnimator;
import com.shouyun.inventorylens.client.config.ConfigManager;
import com.shouyun.inventorylens.client.config.InventoryLensConfigScreen;
import com.shouyun.inventorylens.client.container.ContainerSnapshotCache;
import com.shouyun.inventorylens.client.container.ContainerTargetTracker;
import com.shouyun.inventorylens.client.render.WorldContainerRenderer;
import com.shouyun.inventorylens.client.render.WorldEquipmentRenderer;
import com.shouyun.inventorylens.client.target.EquipmentTargetTracker;
import com.shouyun.inventorylens.container.ContainerIdentity;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.network.ContainerSnapshotPayload;
import com.shouyun.inventorylens.network.ContainerSnapshotRequestPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

public class InventoryLensClient implements ClientModInitializer {
    private static final class ContainerScene implements AutoCloseable {
        final WorldContainerRenderer renderer = new WorldContainerRenderer();
        ContainerSnapshot snapshot;
        BlockHitResult hit;
        ContainerScene(ContainerSnapshot snapshot, BlockHitResult hit) { this.snapshot = snapshot; this.hit = hit; }
        @Override public void close() { renderer.close(); }
    }
    private static final class EquipmentScene implements AutoCloseable {
        final WorldEquipmentRenderer renderer = new WorldEquipmentRenderer();
        LivingEntity target;
        EquipmentScene(LivingEntity target) { this.target = target; }
        @Override public void close() { renderer.close(); }
    }

    private final EquipmentTargetTracker equipmentTracker = new EquipmentTargetTracker();
    private final ContainerSnapshotCache containerCache = new ContainerSnapshotCache();
    private final ContainerTargetTracker containerTracker = new ContainerTargetTracker(containerCache);
    private final WorldUiAnimator<UUID> equipmentAnimation = new WorldUiAnimator<>();
    private final WorldUiAnimator<ContainerIdentity> containerAnimation = new WorldUiAnimator<>();
    private final Map<UUID, EquipmentScene> equipmentScenes = new LinkedHashMap<>();
    private final Map<ContainerIdentity, ContainerScene> containerScenes = new LinkedHashMap<>();
    private ContainerScene focusedContainer;

    @Override public void onInitializeClient() {
        ConfigManager.load();
        KeyMapping settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.inventorylens.settings", GLFW.GLFW_KEY_UNKNOWN, "category.inventorylens"));
        ClientPlayNetworking.registerGlobalReceiver(ContainerSnapshotPayload.TYPE, (payload, context) -> {
            containerTracker.update(context.client(), 1,
                    ConfigManager.get().visibility.containers && ClientPlayNetworking.canSend(ContainerSnapshotRequestPayload.TYPE),
                    focusedContainer != null && focusedContainer.renderer.previewFocused());
            containerCache.receive(payload, Util.getMillis());
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settingsKey.consumeClick()) client.setScreen(new InventoryLensConfigScreen(client.screen));
            containerTracker.update(client, 1,
                    ConfigManager.get().visibility.containers && ClientPlayNetworking.canSend(ContainerSnapshotRequestPayload.TYPE),
                    focusedContainer != null && focusedContainer.renderer.previewFocused());
            if (containerTracker.hitPosition() != null && containerTracker.target() != null
                    && ConfigManager.get().visibility.containerEnabled(containerTracker.target().type().id().getPath())) {
                ContainerSnapshotRequestPayload request = containerCache.request(containerTracker.hitPosition(),
                        Util.getMillis(), focusedContainer != null && focusedContainer.renderer.previewFocused());
                if (request != null) ClientPlayNetworking.send(request);
            }
        });
        WorldRenderEvents.LAST.register(context -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (context.matrixStack() == null) return;
            long now = System.nanoTime();
            if (ConfigManager.get().visibility.entity) equipmentTracker.update(minecraft, context.camera().getPartialTickTime());
            else equipmentTracker.clear();
            UUID equipmentKey = equipmentTracker.target() == null ? null : equipmentTracker.target().getUUID();
            if (equipmentKey != null) {
                equipmentAnimation.show(equipmentKey);
                equipmentScenes.computeIfAbsent(equipmentKey, key -> new EquipmentScene(equipmentTracker.target()))
                        .target = equipmentTracker.target();
            }
            equipmentAnimation.hideOthers(equipmentKey);
            equipmentAnimation.update(now, ConfigManager.get().animation);
            for (Iterator<Map.Entry<UUID, EquipmentScene>> it = equipmentScenes.entrySet().iterator(); it.hasNext();) {
                var entry = it.next();
                if (!equipmentAnimation.contains(entry.getKey())) { entry.getValue().close(); it.remove(); continue; }
                float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(
                        !context.world().tickRateManager().isEntityFrozen(entry.getValue().target));
                entry.getValue().renderer.render(minecraft, context.matrixStack(), context.camera(), partialTick,
                        entry.getValue().target, equipmentAnimation.visual(entry.getKey()), !entry.getKey().equals(equipmentKey));
            }

            containerTracker.update(minecraft, context.camera().getPartialTickTime(),
                    ConfigManager.get().visibility.containers && ClientPlayNetworking.canSend(ContainerSnapshotRequestPayload.TYPE),
                    focusedContainer != null && focusedContainer.renderer.previewFocused());
            ContainerSnapshot snapshot = containerCache.snapshot(Util.getMillis());
            ContainerIdentity containerKey = snapshot != null && containerTracker.hitResult() != null
                    && ConfigManager.get().visibility.containerEnabled(snapshot.container().type().id().getPath())
                    ? snapshot.container().identity() : null;
            if (containerKey != null) {
                containerAnimation.show(containerKey);
                ContainerScene scene = containerScenes.computeIfAbsent(containerKey,
                        key -> new ContainerScene(snapshot, containerTracker.hitResult()));
                scene.snapshot = snapshot;
                scene.hit = containerTracker.hitResult();
                focusedContainer = scene;
            } else focusedContainer = null;
            containerAnimation.hideOthers(containerKey);
            containerAnimation.update(now, ConfigManager.get().animation);
            for (Iterator<Map.Entry<ContainerIdentity, ContainerScene>> it = containerScenes.entrySet().iterator(); it.hasNext();) {
                var entry = it.next();
                if (!containerAnimation.contains(entry.getKey())) { entry.getValue().close(); it.remove(); continue; }
                ContainerScene scene = entry.getValue();
                scene.renderer.render(minecraft, context.matrixStack(), context.camera(), scene.snapshot, scene.hit,
                        containerAnimation.visual(entry.getKey()), !entry.getKey().equals(containerKey));
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> clear());
    }

    private void clear() {
        equipmentTracker.clear();
        containerTracker.clear();
        equipmentAnimation.clear();
        containerAnimation.clear();
        equipmentScenes.values().forEach(EquipmentScene::close);
        containerScenes.values().forEach(ContainerScene::close);
        equipmentScenes.clear();
        containerScenes.clear();
        focusedContainer = null;
    }
}
