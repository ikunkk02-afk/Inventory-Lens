package com.shouyun.inventorylens.config;

import java.util.HashMap;
import java.util.Map;

/** Loader independent, versioned visual preferences. Distances are GUI/camera local blocks. */
public final class InventoryLensConfig {
	public int configVersion = 3;
	/** auto, zh_cn, or en_us; affects only the Inventory Lens settings screen. */
	public String language = "auto";
	public Placement general = new Placement();
	public Placement entity = new Placement();
	public Placement container = new Placement();
	public Animation animation = new Animation();
	public Visibility visibility = new Visibility();

	public static final class Placement {
		public double scale = 1;
		public double horizontalOffset;
		public double verticalOffset;
		public double depthOffset;
	}

	public static final class Animation {
		public boolean enabled = true;
		public int enterDurationMs = 180;
		public int exitDurationMs = 150;
	}

	/** Missing type keys remain visible, preserving older configurations. */
	public static final class Visibility {
		public boolean entity = true;
		public boolean containers = true;
		public Map<String, Boolean> types = new HashMap<>();

		public boolean typeEnabled(String id) {
			return types == null || !Boolean.FALSE.equals(types.get(group(id)));
		}

		public boolean containerEnabled(String id) {
			return containers && typeEnabled(id);
		}

		public void setTypeEnabled(String id, boolean enabled) {
			if (types == null) types = new HashMap<>();
			if (enabled) types.remove(group(id)); else types.put(group(id), false);
		}

		private static String group(String id) {
			return switch (id) {
				case "double_chest", "trapped_chest", "double_trapped_chest" -> "chest";
				default -> id;
			};
		}
	}

	public void sanitize() {
		if (general == null) general = new Placement();
		if (entity == null) entity = new Placement();
		if (container == null) container = new Placement();
		if (animation == null) animation = new Animation();
		if (visibility == null) visibility = new Visibility();
		if (visibility.types == null) visibility.types = new HashMap<>();
		visibility.types.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
		sanitize(general);
		sanitize(entity);
		sanitize(container);
		animation.enterDurationMs = Math.clamp(animation.enterDurationMs, 50, 600);
		animation.exitDurationMs = Math.clamp(animation.exitDurationMs, 50, 600);
		if (!"auto".equals(language) && !"zh_cn".equals(language) && !"en_us".equals(language)) language = "auto";
		configVersion = 3;
	}

	private static void sanitize(Placement value) {
		value.scale = finite(value.scale, 0.5, 2, 1);
		value.horizontalOffset = finite(value.horizontalOffset, -2, 2, 0);
		value.verticalOffset = finite(value.verticalOffset, -2, 2, 0);
		value.depthOffset = finite(value.depthOffset, -1, 1, 0);
	}

	private static double finite(double value, double low, double high, double fallback) {
		return Double.isFinite(value) ? Math.clamp(value, low, high) : fallback;
	}

	public double scale(boolean equipment) {
		return Math.clamp(general.scale * (equipment ? entity.scale : container.scale), 0.25, 4);
	}

	public double horizontal(boolean equipment) {
		return general.horizontalOffset + (equipment ? entity.horizontalOffset : container.horizontalOffset);
	}

	public double vertical(boolean equipment) {
		return general.verticalOffset + (equipment ? entity.verticalOffset : container.verticalOffset);
	}

	public double depth(boolean equipment) {
		return general.depthOffset + (equipment ? entity.depthOffset : container.depthOffset);
	}
}
