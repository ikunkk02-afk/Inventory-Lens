package com.shouyun.inventorylens.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.client.animation.WorldUiAnimator;
import com.shouyun.inventorylens.client.config.ConfigManager;
import com.shouyun.inventorylens.client.preview.NestedPreviewManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.Util;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import com.mojang.blaze3d.platform.GlStateManager;

public final class WorldContainerRenderer implements AutoCloseable {
	private static final float WORLD_PIXEL_SIZE = 0.005F;
	private final WorldContainerGuiRenderer grid = new WorldContainerGuiRenderer();
	private final WorldContainerPlacement placement = new WorldContainerPlacement();
	private final NestedPreviewManager previews = new NestedPreviewManager();
	private WorldItemRenderer items;
	private WorldContainerPlacement.Placement lastPanel;

	public void render(Minecraft minecraft, PoseStack pose, Camera camera, ContainerSnapshot snapshot, BlockHitResult hit) {
		render(minecraft, pose, camera, snapshot, hit, new WorldUiAnimator.Visual(1, 1, 0), false);
	}

	public void render(Minecraft minecraft, PoseStack pose, Camera camera, ContainerSnapshot snapshot, BlockHitResult hit,
			WorldUiAnimator.Visual visual, boolean exiting) {
		var gui = com.shouyun.inventorylens.client.gui.ContainerGuiAdapterRegistry.definition(snapshot);
        if (gui == null) return;
		WorldContainerPlacement.Placement panel = exiting ? lastPanel : placement.update(snapshot.container(), hit, camera.getPosition(), camera.rotation(),
				gui.width() * WORLD_PIXEL_SIZE,
				gui.height() * WORLD_PIXEL_SIZE);
		if (panel == null) {
			if (!exiting) previews.reset();
			return;
		}
		if (!exiting) lastPanel = panel;
		if (visual.alpha() <= 0) return;
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
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int[] shaderTextures = new int[12];
        for (int i = 0; i < shaderTextures.length; i++) shaderTextures[i] = RenderSystem.getShaderTexture(i);
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] scissorBox = new int[4];
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
		pose.pushPose();
		try {
			items.alpha(visual.alpha());
			RenderSystem.setShaderColor(1, 1, 1, 1);
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_LEQUAL);
			RenderSystem.depthMask(true);
			Vec3 look = new Vec3(camera.getLookVector().x(), camera.getLookVector().y(), camera.getLookVector().z());
			double pixelScale = WORLD_PIXEL_SIZE * panel.scale() * ConfigManager.get().scale(false) * visual.scale();
			var panels = previews.update(snapshot, gui, panel.anchor(), panel.rotation(), pixelScale,
					camera.getPosition(), look, Util.getMillis());
			for (var child : panels) {
				pose.pushPose();
				try {
					Vec3 anchor = WorldUiTransform.offset(child.center(), camera.rotation(),
							ConfigManager.get().horizontal(false), ConfigManager.get().vertical(false)
									+ visual.offsetPixels() * WORLD_PIXEL_SIZE, ConfigManager.get().depth(false));
					WorldUiTransform.applyAt(pose, camera.getPosition(), panel.rotation(), anchor.x, anchor.y, anchor.z);
					float scale = (float)(WORLD_PIXEL_SIZE * panel.scale() * ConfigManager.get().scale(false)
							* visual.scale() / WorldUiTransform.PIXEL_SCALE);
					pose.scale(scale, scale, scale);
					pose.translate(-child.gui().width() * 0.5, -child.gui().height() * 0.5, 0);
					grid.render(minecraft, pose, items, child.items(), child.title(),
							snapshot.container().identity().position().hashCode() + child.level() * 31,
							child.gui(), child.hoveredSlot());
				} finally { pose.popPose(); }
			}
        } finally {
            try {
                items.flush();
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
            for (int i = 0; i < shaderTextures.length; i++) RenderSystem.setShaderTexture(i, shaderTextures[i]);
            RenderSystem.activeTexture(activeTexture);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            if (!scissor) RenderSystem.disableScissor();
            }
		}
	}

	public void resetPlacement() {
		placement.reset();
		previews.reset();
		lastPanel = null;
	}

	public boolean previewFocused() { return previews.retainsTarget(); }

	@Override
	public void close() {
		resetPlacement();
		if (items != null) {
			items.close();
			items = null;
		}
	}
}
