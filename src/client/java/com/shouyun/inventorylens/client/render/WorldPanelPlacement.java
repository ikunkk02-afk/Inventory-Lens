package com.shouyun.inventorylens.client.render;

import net.minecraft.client.Camera;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3f;

/** Bounded terrain probes for a single billboard. Does not search for entities or change depth state. */
public final class WorldPanelPlacement {
	private static final double SURFACE_GAP = 0.08;
	private static final double MIN_VIEW_DEPTH = 0.65;
	private static final double SWITCH_ADVANTAGE = 0.12;
	private static final double KEEP_SIDE_TOLERANCE = 0.04;
	private final Vector3f right = new Vector3f();
	private final Vector3f up = new Vector3f();
	private final Vector3f forward = new Vector3f();
	private Vec3 position = Vec3.ZERO;
	private float scale = 1;
	private int selectedSide;

	public boolean update(BlockGetter blocks, Camera camera, Vec3 preferredAnchor, Vec3 alternateAnchor,
			float halfWidth, float halfHeight, int preferredSide) {
		camera.rotation().transform(right.set(1, 0, 0));
		camera.rotation().transform(up.set(0, 1, 0));
		camera.rotation().transform(forward.set(0, 0, -1));
		Vec3 eye = camera.getPosition();
		double preferred = visibleFraction(blocks, eye, preferredAnchor, halfWidth, halfHeight);
		double fraction = preferred;
		Vec3 anchor = preferredAnchor;
		int side = preferredSide;
		// The unobstructed case costs only 15 short block rays; never scan the surrounding world.
		if (preferred < 1) {
			double alternate = visibleFraction(blocks, eye, alternateAnchor, halfWidth, halfHeight);
			boolean keepAlternate = selectedSide == -preferredSide
					&& alternate >= preferred - KEEP_SIDE_TOLERANCE;
			if (alternate > 0 && (preferred == 0 || alternate == 1
					|| alternate > preferred + SWITCH_ADVANTAGE || keepAlternate)) {
				fraction = alternate;
				anchor = alternateAnchor;
				side = -preferredSide;
			}
		}
		if (fraction <= 0) {
			// Camera is too close to solid geometry for a safe world-space panel.
			return false;
		}
		selectedSide = side;
		position = eye.lerp(anchor, fraction);
		scale = (float) fraction;
		return true;
	}

	private double visibleFraction(BlockGetter blocks, Vec3 eye, Vec3 anchor, float halfWidth, float halfHeight) {
		double viewDepth = (anchor.x - eye.x) * forward.x()
				+ (anchor.y - eye.y) * forward.y() + (anchor.z - eye.z) * forward.z();
		if (viewDepth < MIN_VIEW_DEPTH) {
			return 0;
		}
		double fraction = 1;
		// Sample the center, borders and interior, including padding around the panel's shadow.
		// 3 columns x 5 rows keeps the cost fixed even in dense terrain.
		for (int column = -1; column <= 1; column++) {
			for (int row = -2; row <= 2; row++) {
				double x = column * halfWidth;
				double y = row * halfHeight * 0.5;
				Vec3 sample = anchor.add(right.x() * x + up.x() * y,
						right.y() * x + up.y() * y, right.z() * x + up.z() * y);
				BlockHitResult hit = blocks.clip(new ClipContext(eye, sample,
						ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
				if (hit.getType() == HitResult.Type.BLOCK) {
					if (hit.isInside()) {
						return 0;
					}
					double clearDistance = Math.max(0, eye.distanceTo(hit.getLocation()) - SURFACE_GAP);
					fraction = Math.min(fraction, clearDistance / eye.distanceTo(sample));
					if (fraction * viewDepth < MIN_VIEW_DEPTH) {
						return 0;
					}
				}
			}
		}
		return fraction;
	}

	public Vec3 position() {
		return position;
	}

	/** Shrink by the same ratio as the camera-relative translation: screen size stays unchanged. */
	public float scale() {
		return scale;
	}

	public int selectedSide() {
		return selectedSide;
	}

	public void reset() {
		selectedSide = 0;
		position = Vec3.ZERO;
		scale = 1;
	}
}
