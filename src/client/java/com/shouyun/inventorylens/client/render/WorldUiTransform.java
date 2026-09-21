package com.shouyun.inventorylens.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/** World transforms shared by billboard panels. Does not install a GUI projection. */
public final class WorldUiTransform {
	public static final float PIXEL_SCALE = 0.025F;
	private static final double MIN_SIDE_OFFSET = 1.2;
	private static final double SIDE_CLEARANCE = 0.8;
	private static final double CAMERA_OFFSET = 0.35;
	private static final double SIDE_SWITCH_DEAD_ZONE = 0.04;

	private WorldUiTransform() {
	}

	/** Preferred world anchor before terrain avoidance; independent of rendering state. */
	public static Vec3 equipmentAnchor(Camera camera, Entity entity, float partialTick, int panelSide) {
		Vector3f left = camera.getLeftVector();
		Vector3f forward = camera.getLookVector();
		double horizontalLength = Math.sqrt(left.x() * left.x() + left.z() * left.z());
		double side = Math.max(MIN_SIDE_OFFSET, entity.getBbWidth() * 0.5 + SIDE_CLEARANCE) * panelSide;
		// Camera left is a unit horizontal vector even when looking straight up/down.
		double rightX = -left.x() / horizontalLength;
		double rightZ = -left.z() / horizontalLength;
		// Match LevelRenderer.renderEntity: xOld/yOld/zOld, rather than tick-only positions.
		// Pull toward the camera along its view axis so held equipment has room behind the panel.
		return new Vec3(
				Mth.lerp(partialTick, entity.xOld, entity.getX()) + rightX * side - forward.x() * CAMERA_OFFSET,
				Mth.lerp(partialTick, entity.yOld, entity.getY()) + entity.getBbHeight() * 0.55 - forward.y() * CAMERA_OFFSET,
				Mth.lerp(partialTick, entity.zOld, entity.getZ()) + rightZ * side - forward.z() * CAMERA_OFFSET);
	}

	/** +1 means visual right, -1 means visual left. Vertical aim never changes placement. */
	public static int sideOppositeAim(Vec3 hitPosition, Vec3 entityPosition, Vector3f cameraLeft, int previousSide) {
		// Compare against the current entity position, matching the bounding box used by vanilla picking.
		double aimRight = -(hitPosition.x - entityPosition.x) * cameraLeft.x()
				- (hitPosition.z - entityPosition.z) * cameraLeft.z();
		if (aimRight > SIDE_SWITCH_DEAD_ZONE) {
			return -1;
		}
		if (aimRight < -SIDE_SWITCH_DEAD_ZONE) {
			return 1;
		}
		return previousSide;
	}

	/** Generic anchor transform; future panels do not need an Entity to use this pixel plane. */
	public static void applyAt(PoseStack pose, Vec3 cameraPosition, Quaternionf cameraRotation,
			double worldX, double worldY, double worldZ) {
		pose.translate(worldX - cameraPosition.x, worldY - cameraPosition.y, worldZ - cameraPosition.z);
		pose.mulPose(cameraRotation);
		// Local X is screen-right, Y is down, and positive Z points toward the camera.
		pose.scale(PIXEL_SCALE, -PIXEL_SCALE, PIXEL_SCALE);
	}

	/** Screen-right, screen-up, and toward-camera offsets, before panel scaling. */
	public static Vec3 offset(Vec3 anchor, Quaternionfc cameraRotation, double right, double up, double towardCamera) {
		Vector3f vector = new Vector3f((float) right, (float) up, (float) towardCamera);
		cameraRotation.transform(vector);
		return anchor.add(vector.x, vector.y, vector.z);
	}
}
