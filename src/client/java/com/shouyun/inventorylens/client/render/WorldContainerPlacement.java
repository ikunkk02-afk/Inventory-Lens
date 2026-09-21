package com.shouyun.inventorylens.client.render;

import com.shouyun.inventorylens.container.ContainerType;
import com.shouyun.inventorylens.container.ResolvedContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/** A compact preview attached to the lower part of the observed container surface. */
public final class WorldContainerPlacement {
	public static final double GAP = 0.03;
	public static final double EDGE_MARGIN = 0.06;
	public static final double MAX_BILLBOARD_ANGLE = Math.toRadians(12);
	public static final double TOP_MARGIN = 0.025;
	private static final double ITEM_THICKNESS = 0.005;
	private static final double SIDE_ENTER_COS = Math.cos(Math.toRadians(60));
	private static final double SIDE_LEAVE_COS = Math.cos(Math.toRadians(65));
	private static final double TOP_ENTER_COS = Math.cos(Math.toRadians(75));
	private static final double TOP_LEAVE_COS = Math.cos(Math.toRadians(80));
	@Nullable private ResolvedContainer target;
	@Nullable private Direction observedFace;
	private boolean inViewCone;
	private final Vector3f vector = new Vector3f();

	public record Placement(Vec3 anchor, Quaternionf rotation, Direction face, float scale) { }

	@Nullable
	public Placement update(ResolvedContainer container, BlockHitResult hit, Vec3 camera, Quaternionfc cameraRotation,
			float worldWidth, float worldHeight) {
		if (!container.equals(target)) {
			reset();
			target = container;
		}
		if (!container.members().contains(hit.getBlockPos()) || hit.isInside()) {
			return null;
		}
		AABB box = bounds(container);
		Direction face = hit.getDirection();
		if (observedFace != face) {
			observedFace = face;
			inViewCone = false;
		}
		Vec3 faceNormal = Vec3.atLowerCornerOf(face.getNormal());
		Vec3 surfaceCenter = box.getCenter().add(faceNormal.scale(support(box, faceNormal)));
		double viewCosine = camera.subtract(surfaceCenter).normalize().dot(faceNormal);
		double threshold = face.getAxis().isVertical()
				? (inViewCone ? TOP_LEAVE_COS : TOP_ENTER_COS) : (inViewCone ? SIDE_LEAVE_COS : SIDE_ENTER_COS);
		inViewCone = viewCosine >= threshold;
		if (!inViewCone) {
			return null;
		}
		if (face.getAxis().isVertical()) {
			return horizontalSurface(box, surfaceCenter, face, container.facing(), camera, worldWidth, worldHeight);
		}

		// Surface orientation supplies the perspective seen in the reference video. A bounded
		// billboard adjustment keeps it responsive without turning it into a floating sign.
		Quaternionf surfaceRotation = new Quaternionf().rotationY((float) Math.atan2(faceNormal.x, faceNormal.z));
		Quaternionf cameraOrientation = new Quaternionf(cameraRotation);
		double angle = 2 * Math.acos(Math.min(1, Math.abs(surfaceRotation.dot(cameraOrientation))));
		float follow = angle < 0.00001 ? 1 : (float) Math.min(1, MAX_BILLBOARD_ANGLE / angle);
		Quaternionf rotation = new Quaternionf(surfaceRotation).slerp(cameraOrientation, follow).normalize();
		Vec3 right = axis(rotation, 1, 0, 0);
		Vec3 up = axis(rotation, 0, 1, 0);
		Vec3 normal = axis(rotation, 0, 0, 1);
		Vec3 surfaceUp = axis(surfaceRotation, 0, 1, 0);
		double halfHeight = extent(right, up, normal, surfaceUp, worldWidth, worldHeight);
		double outward = GAP + extent(right, up, normal, faceNormal, worldWidth, worldHeight);
		// The bottom edge stays close to the base of the WHOLE container. Ray hit position does
		// not slide the card around; crossing the double-chest seam does not select another anchor.
		Vec3 anchor = surfaceCenter.add(surfaceUp.scale(-support(box, surfaceUp) + EDGE_MARGIN + halfHeight))
				.add(faceNormal.scale(outward));
		if (camera.subtract(anchor).dot(normal) < 0.20) {
			return null;
		}
		return new Placement(anchor, rotation, face, 1);
	}

	@Nullable
	private Placement horizontalSurface(AABB box, Vec3 center, Direction face, Direction facing, Vec3 camera,
			float width, float height) {
		// Lock the entire pose to the block, including after looking away and reacquiring it.
		// Vertical barrels have no horizontal facing, so use a stable north-facing reading direction.
		Direction readingSide = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
		float yaw = (float) Math.atan2(readingSide.getStepX(), readingSide.getStepZ());
		double flatPitch = face == Direction.UP ? -Math.PI / 2 : Math.PI / 2;
		Quaternionf rotation = new Quaternionf().rotationYXZ(yaw, (float) flatPitch, 0);
		// One rotation-independent fit prevents size pulsing while circling the box. The whole
		// rectangle (including icon thickness) fits inside the lid's short dimension at any yaw.
		double diameter = Math.sqrt(width * width + height * height + 4 * ITEM_THICKNESS * ITEM_THICKNESS);
		float scale = (float) Math.min(1, (Math.min(box.getXsize(), box.getZsize()) - 2 * TOP_MARGIN) / diameter);
		Vec3 right = axis(rotation, 1, 0, 0);
		Vec3 up = axis(rotation, 0, 1, 0);
		Vec3 normal = axis(rotation, 0, 0, 1);
		Vec3 faceNormal = Vec3.atLowerCornerOf(face.getNormal());
		double outward = GAP + extent(right, up, normal, faceNormal, width, height) * scale;
		// No tangent offset: both chest halves and every viewing direction share this exact X/Z center.
		Vec3 anchor = center.add(faceNormal.scale(outward));
		if (camera.subtract(anchor).dot(normal) < 0.20) {
			return null;
		}
		return new Placement(anchor, rotation, face, scale);
	}

	private Vec3 axis(Quaternionfc rotation, float x, float y, float z) {
		rotation.transform(vector.set(x, y, z));
		return new Vec3(vector.x, vector.y, vector.z);
	}

	private static double extent(Vec3 right, Vec3 up, Vec3 normal, Vec3 direction, float width, float height) {
		return Math.abs(right.dot(direction)) * width * 0.5 + Math.abs(up.dot(direction)) * height * 0.5
				+ Math.abs(normal.dot(direction)) * ITEM_THICKNESS;
	}

	private static double support(AABB box, Vec3 direction) {
		return (Math.abs(direction.x) * box.getXsize() + Math.abs(direction.y) * box.getYsize()
				+ Math.abs(direction.z) * box.getZsize()) * 0.5;
	}

	public static AABB bounds(ResolvedContainer container) {
		AABB box = new AABB(container.members().getFirst());
		if (container.members().size() == 2) {
			box = box.minmax(new AABB(container.members().get(1)));
		}
		if (container.type() == ContainerType.BARREL) {
			return box;
		}
		return new AABB(box.minX + 1.0 / 16, box.minY, box.minZ + 1.0 / 16,
				box.maxX - 1.0 / 16, box.minY + 14.0 / 16, box.maxZ - 1.0 / 16);
	}

	public void reset() {
		target = null;
		observedFace = null;
		inViewCone = false;
	}
}
