package com.shouyun.inventorylens.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public final class WorldContainerRenderer implements AutoCloseable {
	private static final float WORLD_PIXEL_SIZE = 0.005F;
	private final WorldInventoryGridRenderer grid = new WorldInventoryGridRenderer();
	private final WorldContainerPlacement placement = new WorldContainerPlacement();
	private WorldItemRenderer items;

	public void render(Minecraft minecraft, PoseStack pose, Camera camera, ContainerSnapshot snapshot, BlockHitResult hit) {
		WorldContainerPlacement.Placement panel = placement.update(snapshot.container(), hit, camera.getPosition(), camera.rotation(),
				WorldInventoryGridRenderer.WIDTH * WORLD_PIXEL_SIZE,
				WorldInventoryGridRenderer.height(snapshot.container().type()) * WORLD_PIXEL_SIZE);
		if (panel == null) {
			return;
		}
		if (items == null) {
			items = new WorldItemRenderer();
		}
		// Preserve the same world-pass state contract as WorldEquipmentRenderer without changing it.
		boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
		int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
		int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
		int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
		int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
		ShaderInstance shader = RenderSystem.getShader();
		float[] color = RenderSystem.getShaderColor();
		float red = color[0], green = color[1], blue = color[2], alpha = color[3];
		pose.pushPose();
		try {
			RenderSystem.setShaderColor(1, 1, 1, 1);
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_LEQUAL);
			RenderSystem.depthMask(true);
			Vec3 anchor = panel.anchor();
			WorldUiTransform.applyAt(pose, camera.getPosition(), panel.rotation(), anchor.x, anchor.y, anchor.z);
			float scale = WORLD_PIXEL_SIZE * panel.scale() / WorldUiTransform.PIXEL_SCALE;
			pose.scale(scale, scale, scale);
			pose.translate(-WorldInventoryGridRenderer.WIDTH * 0.5,
					-WorldInventoryGridRenderer.height(snapshot.container().type()) * 0.5, 0);
			grid.render(minecraft, pose, items, snapshot);
		} finally {
			pose.popPose();
			if (minecraft.level.effects().constantAmbientLight()) {
				Lighting.setupNetherLevel();
			} else {
				Lighting.setupLevel();
			}
			RenderSystem.setShaderColor(red, green, blue, alpha);
			RenderSystem.setShader(() -> shader);
			RenderSystem.depthMask(depthMask);
			RenderSystem.depthFunc(depthFunction);
			if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
			if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
			RenderSystem.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
			if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
		}
	}

	public void resetPlacement() {
		placement.reset();
	}

	@Override
	public void close() {
		resetPlacement();
		if (items != null) {
			items.close();
			items = null;
		}
	}
}
