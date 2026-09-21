package com.shouyun.inventorylens.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shouyun.inventorylens.client.target.EquipmentTargetTracker;
import com.shouyun.inventorylens.client.animation.WorldUiAnimator;
import com.shouyun.inventorylens.client.config.ConfigManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public final class WorldEquipmentRenderer implements AutoCloseable {
	private static final int PANEL_WIDTH = 48;
	private static final int PANEL_HEIGHT = 84;
	private static final int SLOT_SIZE = 18;
	private static final float PANEL_SCALE = 0.8F;
	private static final PanelSlot[] SLOTS = {
			new PanelSlot(EquipmentSlot.HEAD, 5, 5, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET),
			new PanelSlot(EquipmentSlot.CHEST, 5, 24, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE),
			new PanelSlot(EquipmentSlot.LEGS, 5, 43, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS),
			new PanelSlot(EquipmentSlot.FEET, 5, 62, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS),
			new PanelSlot(EquipmentSlot.MAINHAND, 25, 43, ResourceLocation.withDefaultNamespace("item/empty_slot_sword")),
			new PanelSlot(EquipmentSlot.OFFHAND, 25, 62, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD)
	};

	private record PanelSlot(EquipmentSlot equipmentSlot, int x, int y, ResourceLocation emptySprite) {
	}
	private WorldItemRenderer items;
	private final WorldPanelPlacement placement = new WorldPanelPlacement();
	private int placementTargetId = Integer.MIN_VALUE;
	private int panelSide = 1;
	private boolean hasPlacement;

	public void render(Minecraft minecraft, PoseStack pose, Camera camera, float partialTick,
			EquipmentTargetTracker equipment) {
		if (equipment.target() != null) render(minecraft, pose, camera, partialTick, equipment.target(),
				new WorldUiAnimator.Visual(1, 1, 0), false);
	}

	public void render(Minecraft minecraft, PoseStack pose, Camera camera, float partialTick,
			LivingEntity target, WorldUiAnimator.Visual visual, boolean exiting) {
		if (visual.alpha() <= 0) return;
		if (target.getId() != placementTargetId) {
			resetPlacement();
			placementTargetId = target.getId();
		}
		if (!exiting && minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() == target) {
			panelSide = WorldUiTransform.sideOppositeAim(hit.getLocation(), target.position(),
					camera.getLeftVector(), panelSide);
		}
		float worldPixelSize = WorldUiTransform.PIXEL_SCALE * PANEL_SCALE;
		if (!exiting && !placement.update(minecraft.level, camera,
				WorldUiTransform.equipmentAnchor(camera, target, partialTick, panelSide),
				WorldUiTransform.equipmentAnchor(camera, target, partialTick, -panelSide),
				(PANEL_WIDTH * 0.5F + 2) * worldPixelSize,
				(PANEL_HEIGHT * 0.5F + 3) * worldPixelSize, panelSide)) {
			return;
		}
		if (exiting && !hasPlacement) return;
		hasPlacement = true;
		if (items == null) {
			items = new WorldItemRenderer();
		}
		// RenderType teardown restores vanilla defaults; preserve the caller's actual GL state too.
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
			items.alpha(visual.alpha());
			RenderSystem.setShaderColor(1, 1, 1, 1);
			RenderSystem.depthMask(true);
			Vec3 anchor = WorldUiTransform.offset(placement.position(), camera.rotation(),
					ConfigManager.get().horizontal(true), ConfigManager.get().vertical(true)
							+ visual.offsetPixels() * WorldUiTransform.PIXEL_SCALE * PANEL_SCALE,
					ConfigManager.get().depth(true));
			WorldUiTransform.applyAt(pose, camera.getPosition(), camera.rotation(), anchor.x, anchor.y, anchor.z);
			float scale = (float)(PANEL_SCALE * placement.scale() * ConfigManager.get().scale(true) * visual.scale());
			pose.scale(scale, scale, scale);
			pose.translate(-PANEL_WIDTH * 0.5F, -PANEL_HEIGHT * 0.5F, 0);
			drawPanel(pose);
			for (PanelSlot slot : SLOTS) {
				drawSlot(pose, slot.x(), slot.y());
			}
			items.flush();
			for (PanelSlot slot : SLOTS) {
				ItemStack stack = target.getItemBySlot(slot.equipmentSlot());
				if (stack.isEmpty()) {
					items.sprite(pose, minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(slot.emptySprite()),
							slot.x() + 1, slot.y() + 1, 0.05F);
				} else {
					items.render(minecraft, pose, stack, target, slot.x() + 1, slot.y() + 1,
							target.getId() + slot.equipmentSlot().ordinal());
				}
			}
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
		}
	}

	public void resetPlacement() {
		placementTargetId = Integer.MIN_VALUE;
		panelSide = 1;
		placement.reset();
		hasPlacement = false;
	}

	private void drawPanel(PoseStack pose) {
		// Pixel bevels evoke vanilla inventory windows; all quads remain in the world plane.
		items.fill(pose, 1, 2, PANEL_WIDTH + 1, PANEL_HEIGHT + 2, -0.02F, 0x90000000);
		items.fill(pose, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 0, 0xFF24282E);
		items.fill(pose, 1, 1, PANEL_WIDTH - 1, PANEL_HEIGHT - 1, 0, 0xFFC6CCD2);
		items.fill(pose, 1, 1, PANEL_WIDTH - 2, 3, 0, 0xFFF7FAFC);
		items.fill(pose, 1, 3, 3, PANEL_HEIGHT - 2, 0, 0xFFF7FAFC);
		items.fill(pose, 3, PANEL_HEIGHT - 3, PANEL_WIDTH - 1, PANEL_HEIGHT - 1, 0, 0xFF606871);
		items.fill(pose, PANEL_WIDTH - 3, 3, PANEL_WIDTH - 1, PANEL_HEIGHT - 3, 0, 0xFF818A94);
		items.fill(pose, 3, 3, PANEL_WIDTH - 3, 4, 0, 0xFFE6EBF0);
		items.fill(pose, 3, 4, 4, PANEL_HEIGHT - 3, 0, 0xFFE6EBF0);
	}

	private void drawSlot(PoseStack pose, int x, int y) {
		items.fill(pose, x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0.01F, 0xFF373E47);
		items.fill(pose, x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0.01F, 0xFF9098A1);
		items.fill(pose, x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, 0.01F, 0xFFF0F4F8);
		items.fill(pose, x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE - 1, 0.01F, 0xFFF0F4F8);
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
