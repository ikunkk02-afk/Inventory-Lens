package com.shouyun.inventorylens.client.render;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/** Native item icons on an existing world-space pixel plane, including vanilla decorations. */
public final class WorldItemRenderer implements AutoCloseable {
	private static final float ITEM_Z = 0.1F;
	private static final float DECORATION_Z = 0.3F;
	// Nested bevel rectangles must retain submission order rather than being distance-sorted.
	private static final RenderType PANEL_QUADS = new RenderType("inventorylens_panel",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
			RenderType.debugStructureQuads()::setupRenderState,
			RenderType.debugStructureQuads()::clearRenderState) { };
	private final ByteBufferBuilder shared = new ByteBufferBuilder(4096);
	private final LinkedHashMap<RenderType, ByteBufferBuilder> foilBuffers = new LinkedHashMap<>();
	private final Map<RenderType, RenderType> mainTargetLayers = Map.of(
			Sheets.translucentItemSheet(), new MainTargetLayer(Sheets.translucentItemSheet()),
			RenderType.glintTranslucent(), new MainTargetLayer(RenderType.glintTranslucent()),
			RenderType.entityGlint(), new MainTargetLayer(RenderType.entityGlint()));
	private final MultiBufferSource.BufferSource buffers;
	private final MultiBufferSource itemBuffers;

	public WorldItemRenderer() {
		// Foil consumers are requested before the base model. Separate persistent buffers keep them
		// alive while base vertices are emitted and make endBatch draw base depth BEFORE EQUAL glint.
		for (RenderType type : new RenderType[] {
				RenderType.armorEntityGlint(), RenderType.glint(), RenderType.glintTranslucent(),
				RenderType.entityGlint(), RenderType.entityGlintDirect()
		}) {
			foilBuffers.put(mainTargetLayers.getOrDefault(type, type), new ByteBufferBuilder(1536));
		}
		buffers = MultiBufferSource.immediateWithBuffers(foilBuffers, shared);
		itemBuffers = type -> buffers.getBuffer(mainTargetLayers.getOrDefault(type, type));
	}

	public void render(Minecraft minecraft, PoseStack pose, ItemStack stack, LivingEntity owner,
			int x, int y, int seed) {
		render(minecraft, pose, stack, owner.level(), owner, x, y, seed);
	}

	/** Containers have a world but no owning entity; preserve the equipment overload unchanged. */
	public void render(Minecraft minecraft, PoseStack pose, ItemStack stack, Level level, @Nullable LivingEntity owner,
			int x, int y, int seed) {
		ItemRenderer renderer = minecraft.getItemRenderer();
		BakedModel model = renderer.getModel(stack, level, owner, seed);
		if (model.usesBlockLight()) {
			Lighting.setupFor3DItems();
		} else {
			Lighting.setupForFlatItems();
		}
		pose.pushPose();
		try {
			pose.translate(x + 8, y + 8, ITEM_Z);
			// Keep the GUI silhouette but compress model thickness: vanilla's GUI Z=150/200 offsets
			// would move the icon several blocks toward the camera and break wall occlusion here.
			pose.scale(16, -16, 0.1F);
			// GUI lighting uses GUI-space normals, independent of camera yaw and world pixel scale.
			pose.last().normal().scaling(1, -1, 1);
			renderer.render(stack, ItemDisplayContext.GUI, false, pose, itemBuffers,
					LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
			flush();
		} finally {
			pose.popPose();
		}
		if (stack.isBarVisible()) {
			fill(pose, x + 2, y + 13, x + 15, y + 15, DECORATION_Z, 0xFF000000);
			fill(pose, x + 2, y + 13, x + 2 + stack.getBarWidth(), y + 14,
					DECORATION_Z + 0.01F, 0xFF000000 | stack.getBarColor());
			flush();
		}
		if (stack.getCount() > 1) {
			String count = Integer.toString(stack.getCount());
			pose.pushPose();
			try {
				pose.translate(0, 0, DECORATION_Z + 0.02F);
				minecraft.font.drawInBatch(count, x + 17 - minecraft.font.width(count), y + 9,
						0xFFFFFFFF, true, pose.last().pose(), buffers, Font.DisplayMode.NORMAL,
						0, LightTexture.FULL_BRIGHT);
				flush();
			} finally {
				pose.popPose();
			}
		}
	}

	/** This native layer blends, tests LEQUAL, disables culling, and writes color only. */
	public void fill(PoseStack pose, float left, float top, float right, float bottom, float z, int color) {
		Matrix4f matrix = pose.last().pose();
		VertexConsumer vertices = buffers.getBuffer(PANEL_QUADS);
		vertices.addVertex(matrix, left, top, z).setColor(color);
		vertices.addVertex(matrix, left, bottom, z).setColor(color);
		vertices.addVertex(matrix, right, bottom, z).setColor(color);
		vertices.addVertex(matrix, right, top, z).setColor(color);
	}

	/** Vanilla inventory placeholder sprite, with world depth and no screen-space GUI calls. */
	public void sprite(PoseStack pose, TextureAtlasSprite sprite, int x, int y, float z) {
		Matrix4f matrix = pose.last().pose();
		VertexConsumer vertices = buffers.getBuffer(RenderType.text(sprite.atlasLocation()));
		vertices.addVertex(matrix, x, y, z).setColor(0xFFFFFFFF)
				.setUv(sprite.getU0(), sprite.getV0()).setLight(LightTexture.FULL_BRIGHT);
		vertices.addVertex(matrix, x, y + 16, z).setColor(0xFFFFFFFF)
				.setUv(sprite.getU0(), sprite.getV1()).setLight(LightTexture.FULL_BRIGHT);
		vertices.addVertex(matrix, x + 16, y + 16, z).setColor(0xFFFFFFFF)
				.setUv(sprite.getU1(), sprite.getV1()).setLight(LightTexture.FULL_BRIGHT);
		vertices.addVertex(matrix, x + 16, y, z).setColor(0xFFFFFFFF)
				.setUv(sprite.getU1(), sprite.getV0()).setLight(LightTexture.FULL_BRIGHT);
	}

	/** Native GUI texture regions on the existing world plane; NORMAL text layers retain depth. */
	public void blit(PoseStack pose, ResourceLocation texture, int x, int y, int width, int height,
			int u, int v, int textureWidth, int textureHeight, float z) {
		Matrix4f matrix = pose.last().pose();
		VertexConsumer vertices = buffers.getBuffer(RenderType.text(texture));
		float u0 = (float) u / textureWidth;
		float v0 = (float) v / textureHeight;
		float u1 = (float) (u + width) / textureWidth;
		float v1 = (float) (v + height) / textureHeight;
		vertices.addVertex(matrix, x, y, z).setColor(0xFFFFFFFF)
				.setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
		vertices.addVertex(matrix, x, y + height, z).setColor(0xFFFFFFFF)
				.setUv(u0, v1).setLight(LightTexture.FULL_BRIGHT);
		vertices.addVertex(matrix, x + width, y + height, z).setColor(0xFFFFFFFF)
				.setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
		vertices.addVertex(matrix, x + width, y, z).setColor(0xFFFFFFFF)
				.setUv(u1, v0).setLight(LightTexture.FULL_BRIGHT);
	}

    public void guiSprite(Minecraft minecraft, PoseStack pose,
            com.shouyun.inventorylens.client.gui.ContainerGuiDefinition.SpriteElement e, float z) {
        if (e.width() <= 0 || e.height() <= 0) return;
        TextureAtlasSprite sprite = minecraft.getGuiSprites().getSprite(e.sprite());
        for (var q : com.shouyun.inventorylens.client.gui.GuiSpriteLayout.quads(e, minecraft.getGuiSprites().getSpriteScaling(sprite))) {
            spriteRegion(pose, sprite, q.x(), q.y(), q.width(), q.height(), q.u0(), q.v0(), q.u1(), q.v1(), z);
        }
    }
    private void spriteRegion(PoseStack pose, TextureAtlasSprite sprite, int x, int y, int width, int height,
            float u0, float v0, float u1, float v1, float z) {
        Matrix4f matrix = pose.last().pose();
        VertexConsumer vertices = buffers.getBuffer(RenderType.text(sprite.atlasLocation()));
        float left = sprite.getU(u0), right = sprite.getU(u1), top = sprite.getV(v0), bottom = sprite.getV(v1);
        vertices.addVertex(matrix, x, y, z).setColor(-1).setUv(left, top).setLight(LightTexture.FULL_BRIGHT);
        vertices.addVertex(matrix, x, y + height, z).setColor(-1).setUv(left, bottom).setLight(LightTexture.FULL_BRIGHT);
        vertices.addVertex(matrix, x + width, y + height, z).setColor(-1).setUv(right, bottom).setLight(LightTexture.FULL_BRIGHT);
        vertices.addVertex(matrix, x + width, y, z).setColor(-1).setUv(right, top).setLight(LightTexture.FULL_BRIGHT);
    }

	public void label(Minecraft minecraft, PoseStack pose, Component text, int x, int y, int color) {
		pose.pushPose();
		try {
			pose.translate(0, 0, 0.05F);
			minecraft.font.drawInBatch(text, x, y, color, false, pose.last().pose(), buffers,
					Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
			flush();
		} finally {
			pose.popPose();
		}
	}

	public void flush() {
		// Never use GuiGraphics.flush(): its screen-space state management is inappropriate here.
		buffers.endBatch();
	}

	@Override
	public void close() {
		shared.close();
		foilBuffers.values().forEach(ByteBufferBuilder::close);
	}

	/** LAST runs after Fabulous compositing; keep native shaders but draw into the world target. */
	private static final class MainTargetLayer extends RenderType {
		private MainTargetLayer(RenderType original) {
			super("inventorylens_main_" + original, original.format(), original.mode(), original.bufferSize(),
					original.affectsCrumbling(), original.sortOnUpload(), () -> {
						original.setupRenderState();
						Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
					}, original::clearRenderState);
		}
	}
}
