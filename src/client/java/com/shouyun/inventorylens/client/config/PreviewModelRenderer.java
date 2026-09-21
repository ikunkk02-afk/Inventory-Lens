package com.shouyun.inventorylens.client.config;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import com.mojang.math.Axis;

/** Draws real vanilla model geometry; no world or spawned demo entity is required. */
final class PreviewModelRenderer {
	private static final ResourceLocation ZOMBIE_TEXTURE =
			ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
	private final ChestBlockEntity chest = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
	private ZombieModel<Zombie> zombie;

	void render(GuiGraphics graphics, boolean entity, int x, int feetY, int height) {
		if (entity) renderZombie(graphics, x, feetY, height);
		else renderChest(graphics, x, feetY, height);
	}

	private void renderZombie(GuiGraphics graphics, int x, int feetY, int height) {
		if (zombie == null) zombie = new ZombieModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ZOMBIE));
		graphics.flush();
		PoseStack pose = graphics.pose();
		pose.pushPose();
		try {
			pose.translate(x, feetY, 120);
			float size = height / 2.0f;
			pose.scale(size, size, size);
			pose.mulPose(Axis.YP.rotationDegrees(155));
			pose.translate(0, -1.501, 0);
			Lighting.setupForEntityInInventory();
			zombie.renderToBuffer(pose, graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(ZOMBIE_TEXTURE)),
					LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			graphics.bufferSource().endBatch();
		} finally {
			pose.popPose();
			Lighting.setupFor3DItems();
		}
	}

	private void renderChest(GuiGraphics graphics, int x, int feetY, int height) {
		graphics.flush();
		PoseStack pose = graphics.pose();
		pose.pushPose();
		try {
			pose.translate(x, feetY - height / 2.0, 120);
			pose.scale(height, -height, height);
			pose.mulPose(Axis.YP.rotationDegrees(25));
			pose.mulPose(Axis.XP.rotationDegrees(12));
			pose.translate(-0.5, -0.5, -0.5);
			Lighting.setupForEntityInInventory();
			Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(chest, pose,
					graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			graphics.bufferSource().endBatch();
		} finally {
			pose.popPose();
			Lighting.setupFor3DItems();
		}
	}
}
