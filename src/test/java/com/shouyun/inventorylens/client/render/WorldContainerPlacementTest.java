package com.shouyun.inventorylens.client.render;

import java.util.List;

import com.shouyun.inventorylens.TestWorld;
import com.shouyun.inventorylens.container.ContainerIdentity;
import com.shouyun.inventorylens.container.ContainerType;
import com.shouyun.inventorylens.container.ResolvedContainer;
import com.shouyun.inventorylens.container.VanillaContainerResolver;
import com.shouyun.inventorylens.client.render.WorldContainerPlacement.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldContainerPlacementTest {
	private static final float WIDTH = 176 * 0.005F;

	@BeforeAll
	static void initialize() {
		TestWorld.bootstrap();
	}

	@Test
	void previewSitsInLowerSurfaceBandFromEverySide() {
		for (ContainerType type : ContainerType.values()) {
			var target = target(type);
			AABB box = WorldContainerPlacement.bounds(target);
			for (Direction face : Direction.Plane.HORIZONTAL) {
				Vec3 surface = surface(target, face);
				Vec3 camera = surface.add(normal(face).scale(3)).add(0, 1.2, 0);
				Placement panel = required(target, face, camera);
				double lowest = Double.POSITIVE_INFINITY;
				double highest = Double.NEGATIVE_INFINITY;
				for (Vec3 corner : corners(target, panel)) {
					lowest = Math.min(lowest, corner.y);
					highest = Math.max(highest, corner.y);
				}
				assertEquals(box.minY + WorldContainerPlacement.EDGE_MARGIN, lowest, 0.00001);
				assertTrue(highest < box.maxY, "The card must remain below the lid");
				assertTrue(panel.anchor().y < box.getCenter().y);
				assertTrue(panel.anchor().subtract(surface).dot(normal(face)) < 0.16,
						"The preview must stay close to the surface");
			}
		}
	}

	@Test
	void changingAimPointDoesNotSwitchSidesOrLiftThePanel() {
		var target = target(ContainerType.CHEST);
		Vec3 center = surface(target, Direction.SOUTH);
		Vec3 camera = center.add(0, 1.2, 3);
		Quaternionf rotation = look(camera, center);
		var placement = new WorldContainerPlacement();
		Placement first = placement.update(target, hit(target, Direction.SOUTH, center), camera, rotation, WIDTH, height(target));
		assertNotNull(first);
		for (double x : new double[] {-0.35, 0, 0.35}) {
			for (double y : new double[] {-0.3, 0, 0.3}) {
				assertEquals(first, placement.update(target, hit(target, Direction.SOUTH, center.add(x, y, 0)),
						camera, rotation, WIDTH, height(target)));
			}
		}
		placement.reset();
		assertEquals(first, placement.update(target, hit(target, Direction.SOUTH, center), camera, rotation, WIDTH, height(target)));
	}

	@Test
	void billboardFollowsCameraButStaysWithinTwelveDegreesOfTheSurface() {
		var target = target(ContainerType.CHEST);
		Vec3 center = surface(target, Direction.SOUTH);
		Placement a = required(target, Direction.SOUTH, center.add(0, 0, 3));
		Placement b = required(target, Direction.SOUTH, center.add(0.4, 0, 3));
		Placement c = required(target, Direction.SOUTH, center.add(2, 1.2, 3));
		assertTrue(Math.abs(a.rotation().dot(b.rotation())) < 0.999F);
		for (Placement panel : List.of(a, b, c)) {
			Vector3f normal = panel.rotation().transform(new Vector3f(0, 0, 1));
			assertTrue(normal.z >= Math.cos(WorldContainerPlacement.MAX_BILLBOARD_ANGLE) - 0.00001);
		}
		Vec3 camera = center.add(2, 1.2, 3);
		assertTrue(Math.abs(c.rotation().dot(look(camera, center))) < 0.99,
				"Oblique views retain surface perspective instead of becoming a screen-parallel rectangle");
	}

	@Test
	void viewingConeHasHysteresisAndNeverShowsTheWrongSide() {
		var target = target(ContainerType.CHEST);
		Vec3 center = surface(target, Direction.SOUTH);
		var hit = hit(target, Direction.SOUTH, center);
		var placement = new WorldContainerPlacement();
		boolean lastVisible = false;
		for (int angle : new int[] {0, 63, 67, 63, 58}) {
			Vec3 camera = center.add(3 * Math.sin(Math.toRadians(angle)), 0, 3 * Math.cos(Math.toRadians(angle)));
			Placement panel = placement.update(target, hit, camera, look(camera, center), WIDTH, height(target));
			if (angle == 67 || (angle == 63 && !lastVisible)) {
				assertNull(panel);
			} else {
				assertNotNull(panel);
			}
			lastVisible = panel != null;
		}
		Vec3 behind = center.add(0, 1, -3);
		assertNull(placement.update(target, hit, behind, look(behind, center), WIDTH, height(target)));
		assertNotNull(required(target, Direction.NORTH, surface(target, Direction.NORTH).add(0, 1, -3)));
	}

	@Test
	void allVisibleCornersRemainOutsideTheHitSurfaceIncludingTopAndBottom() {
		int visible = 0;
		for (ContainerType type : ContainerType.values()) {
			var target = target(type);
			for (Direction face : Direction.values()) {
				Vec3 center = surface(target, face);
				Vec3 n = normal(face);
				Vec3 tangent = face.getAxis().isVertical() ? new Vec3(1, 0, 0) : new Vec3(n.z, 0, -n.x);
				for (int angle : new int[] {-70, -50, -20, 0, 20, 50, 70}) {
					Vec3 camera = center.add(n.scale(3 * Math.cos(Math.toRadians(angle))))
							.add(tangent.scale(3 * Math.sin(Math.toRadians(angle))));
					Placement panel = new WorldContainerPlacement().update(target, hit(target, face, center),
							camera, look(camera, center), WIDTH, height(target));
					if (panel == null) continue;
					visible++;
					for (Vec3 corner : corners(target, panel)) {
						assertTrue(corner.subtract(center).dot(n) >= WorldContainerPlacement.GAP - 0.00001);
						if (face.getAxis().isHorizontal()) {
							assertTrue(corner.y >= WorldContainerPlacement.bounds(target).minY + WorldContainerPlacement.EDGE_MARGIN - 0.00001);
						}
					}
				}
			}
		}
		assertTrue(visible >= 90, "Normal and oblique views must actually display a panel");
	}

	@Test
	void doubleChestUsesOneCenterAcrossItsSeam() {
		var world = new TestWorld();
		var right = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT);
		BlockPos first = BlockPos.ZERO;
		BlockPos second = first.relative(ChestBlock.getConnectedDirection(right));
		world.states.put(first, right);
		world.states.put(second, right.setValue(ChestBlock.TYPE, ChestType.LEFT));
		var a = VanillaContainerResolver.resolve(Level.OVERWORLD, world, world::loaded, first);
		var b = VanillaContainerResolver.resolve(Level.OVERWORLD, world, world::loaded, second);
		Vec3 location = surface(a, Direction.SOUTH);
		Vec3 camera = location.add(0, 1.2, 3);
		var placement = new WorldContainerPlacement();
		Placement p = placement.update(a, new BlockHitResult(location, Direction.SOUTH, first, false), camera, look(camera, location), WIDTH, height(a));
		Placement q = placement.update(b, new BlockHitResult(location, Direction.SOUTH, second, false), camera, look(camera, location), WIDTH, height(b));
		assertNotNull(p);
		assertEquals(p, q);
		assertEquals(a.center().x, p.anchor().x, 0.00001);
	}

	@Test
	void turningHeadOverTheLidDoesNotMoveOrSpinThePreview() {
		for (ContainerType type : ContainerType.values()) {
			var target = target(type);
			Vec3 top = surface(target, Direction.UP);
			Vec3 camera = top.add(0.6, 2, 1.5);
			var placement = new WorldContainerPlacement();
			Placement first = placement.update(target, hit(target, Direction.UP, top), camera, look(camera, top), WIDTH, height(target));
			assertNotNull(first);
			for (double x : new double[] {-0.35, 0, 0.35}) {
				for (double z : new double[] {-0.35, 0, 0.35}) {
					Vec3 aim = top.add(x, 0, z);
					Placement moved = placement.update(target, hit(target, Direction.UP, aim), camera, look(camera, aim), WIDTH, height(target));
					assertEquals(first, moved, "Looking around the lid must not drag the panel along with the crosshair");
				}
			}
		}
	}

	@Test
	void circlingTheLidAndReacquiringItKeepsTheEntirePoseFixed() {
		var northSouthDouble = new ResolvedContainer(new ContainerIdentity(Level.OVERWORLD, BlockPos.ZERO),
				ContainerType.DOUBLE_CHEST, List.of(BlockPos.ZERO, BlockPos.ZERO.south()), Direction.EAST);
		for (var target : List.of(target(ContainerType.CHEST), target(ContainerType.BARREL),
				target(ContainerType.DOUBLE_CHEST), northSouthDouble)) {
			AABB box = WorldContainerPlacement.bounds(target);
			Vec3 top = surface(target, Direction.UP);
			var placement = new WorldContainerPlacement();
			Placement fixedPose = null;
			for (int yaw = 0; yaw < 360; yaw += 10) {
				for (double elevation : new double[] {0.8, 2, 4}) {
					Vec3 camera = top.add(2.5 * Math.sin(Math.toRadians(yaw)), elevation, 2.5 * Math.cos(Math.toRadians(yaw)));
					Placement panel = placement.update(target, hit(target, Direction.UP, top), camera, look(camera, top), WIDTH, height(target));
					assertNotNull(panel);
					assertEquals(top.x, panel.anchor().x, 0.00001);
					assertEquals(top.z, panel.anchor().z, 0.00001);
					if (fixedPose == null) fixedPose = panel;
					assertEquals(fixedPose, panel, "Position, rotation and scale must remain fixed while walking around");
					assertEquals(1, panel.rotation().transform(new Vector3f(0, 0, 1)).y, 0.00001);
					placement.reset();
					assertEquals(fixedPose, placement.update(target, hit(target, Direction.UP, top), camera,
							look(camera, top), WIDTH, height(target)), "Reacquiring the same box must restore the same fixed pose");
					for (Vec3 corner : corners(target, panel)) {
						assertTrue(corner.x >= box.minX + WorldContainerPlacement.TOP_MARGIN - 0.00001);
						assertTrue(corner.x <= box.maxX - WorldContainerPlacement.TOP_MARGIN + 0.00001);
						assertTrue(corner.z >= box.minZ + WorldContainerPlacement.TOP_MARGIN - 0.00001);
						assertTrue(corner.z <= box.maxZ - WorldContainerPlacement.TOP_MARGIN + 0.00001);
						assertTrue(corner.y >= box.maxY + WorldContainerPlacement.GAP - 0.00001);
					}
				}
			}
		}
	}

	@Test
	void nearOverheadKeepsReadingDirectionAndBothDoubleChestHalvesShareTopPose() {
		var target = target(ContainerType.DOUBLE_CHEST);
		Vec3 top = surface(target, Direction.UP);
		var placement = new WorldContainerPlacement();
		Vec3 approach = top.add(0, 2, 1);
		assertNotNull(placement.update(target, hit(target, Direction.UP, top), approach, look(approach, top), WIDTH, height(target)));
		Placement previous = null;
		for (Vec3 offset : List.of(new Vec3(0.1, 2, 0), new Vec3(-0.1, 2, 0), new Vec3(0, 2, -0.1))) {
			Vec3 camera = top.add(offset);
			Placement panel = placement.update(target, hit(target, Direction.UP, top), camera, look(camera, top), WIDTH, height(target));
			assertNotNull(panel);
			if (previous != null) assertEquals(previous, panel, "Tiny movements near overhead must not flip the reading direction");
			previous = panel;
			BlockHitResult otherHalf = new BlockHitResult(top, Direction.UP, target.members().get(1), false);
			assertEquals(panel, placement.update(target, otherHalf, camera, look(camera, top), WIDTH, height(target)));
		}
	}

	@Test
	void topFaceSupportsOverheadAndChangingTargetClearsConeState() {
		var target = target(ContainerType.CHEST);
		Vec3 top = surface(target, Direction.UP);
		assertNotNull(required(target, Direction.UP, top.add(0, 3, 0)));
		var placement = new WorldContainerPlacement();
		Vec3 center = surface(target, Direction.SOUTH);
		Vec3 camera = center.add(0, 0, 3);
		assertNotNull(placement.update(target, hit(target, Direction.SOUTH, center), camera, look(camera, center), WIDTH, height(target)));
		var other = target(ContainerType.BARREL);
		center = surface(other, Direction.SOUTH);
		camera = center.add(3 * Math.sin(Math.toRadians(63)), 0, 3 * Math.cos(Math.toRadians(63)));
		assertNull(placement.update(other, hit(other, Direction.SOUTH, center), camera, look(camera, center), WIDTH, height(other)));
	}

	private static List<Vec3> corners(ResolvedContainer target, Placement panel) {
		var result = new java.util.ArrayList<Vec3>();
		for (int x : new int[] {-1, 1}) for (int y : new int[] {-1, 1}) for (int z : new int[] {-1, 1}) {
			Vector3f local = panel.rotation().transform(new Vector3f(x * WIDTH / 2, y * height(target) / 2, z * 0.005F).mul(panel.scale()));
			result.add(panel.anchor().add(local.x, local.y, local.z));
		}
		return result;
	}

	private static ResolvedContainer target(ContainerType type) {
		return new ResolvedContainer(new ContainerIdentity(Level.OVERWORLD, BlockPos.ZERO), type,
				type == ContainerType.DOUBLE_CHEST ? List.of(BlockPos.ZERO, BlockPos.ZERO.east()) : List.of(BlockPos.ZERO), Direction.NORTH);
	}

	private static float height(ResolvedContainer target) {
		return (24 + 18 * target.type().rows()) * 0.005F;
	}

	private static Vec3 normal(Direction face) {
		return Vec3.atLowerCornerOf(face.getNormal());
	}

	private static Vec3 surface(ResolvedContainer target, Direction face) {
		AABB box = WorldContainerPlacement.bounds(target);
		Vec3 n = normal(face);
		double depth = (Math.abs(n.x) * box.getXsize() + Math.abs(n.y) * box.getYsize() + Math.abs(n.z) * box.getZsize()) / 2;
		return box.getCenter().add(n.scale(depth));
	}

	private static BlockHitResult hit(ResolvedContainer target, Direction face, Vec3 point) {
		return new BlockHitResult(point, face, target.members().getFirst(), false);
	}

	private static Quaternionf look(Vec3 camera, Vec3 point) {
		Vec3 delta = camera.subtract(point);
		return new Quaternionf().rotationYXZ((float) Math.atan2(delta.x, delta.z),
				(float) -Math.atan2(delta.y, Math.hypot(delta.x, delta.z)), 0);
	}

	private static Placement required(ResolvedContainer target, Direction face, Vec3 camera) {
		Vec3 center = surface(target, face);
		Placement panel = new WorldContainerPlacement().update(target, hit(target, face, center), camera, look(camera, center), WIDTH, height(target));
		assertNotNull(panel, target.type() + " on " + face);
		return panel;
	}
}
