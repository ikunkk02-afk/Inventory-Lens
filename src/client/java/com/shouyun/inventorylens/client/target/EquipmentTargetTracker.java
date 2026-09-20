package com.shouyun.inventorylens.client.target;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

/** A frame-local view of vanilla's pick result; never queries the world's entity list. */
public final class EquipmentTargetTracker {
	private static final double MAX_DISTANCE_SQUARED = 8.0 * 8.0;
	private static final EquipmentSlot[] SLOTS = {
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
			EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};
	private final ItemStack[] equipment = new ItemStack[SLOTS.length];
	@Nullable
	private LivingEntity target;
	private int count;

	public EquipmentTargetTracker() {
		clear();
	}

	public void update(Minecraft minecraft, float partialTick) {
		clear();
		Entity cameraEntity = minecraft.getCameraEntity();
		if (minecraft.level == null || minecraft.player == null || cameraEntity == null
				|| minecraft.screen != null || minecraft.options.hideGui) {
			return;
		}
		if (!(minecraft.crosshairPickEntity instanceof LivingEntity living)
				|| living instanceof Player || living instanceof ArmorStand
				|| !living.isAlive() || living.isRemoved() || living.level() != minecraft.level
				|| living.isInvisibleTo(minecraft.player)) {
			return;
		}
		// hitResult already respects blocks and vanilla interaction reach. Eight blocks is only a cap.
		if (!(minecraft.hitResult instanceof EntityHitResult hit) || hit.getEntity() != living
				|| cameraEntity.getEyePosition(partialTick).distanceToSqr(hit.getLocation()) > MAX_DISTANCE_SQUARED) {
			return;
		}
		for (int index = 0; index < SLOTS.length; index++) {
			ItemStack stack = living.getItemBySlot(SLOTS[index]);
			equipment[index] = stack;
			if (!stack.isEmpty()) {
				count++;
			}
		}
		if (count > 0) {
			target = living;
		}
	}

	public void clear() {
		target = null;
		count = 0;
		Arrays.fill(equipment, ItemStack.EMPTY);
	}

	@Nullable
	public LivingEntity target() {
		return target;
	}

	public int size() {
		return count;
	}

	public ItemStack stack(EquipmentSlot slot) {
		return switch (slot) {
			case MAINHAND -> equipment[0];
			case OFFHAND -> equipment[1];
			case HEAD -> equipment[2];
			case CHEST -> equipment[3];
			case LEGS -> equipment[4];
			case FEET -> equipment[5];
			default -> ItemStack.EMPTY;
		};
	}
}
