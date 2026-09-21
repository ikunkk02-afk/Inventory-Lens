package com.shouyun.inventorylens.client.animation;

import java.util.LinkedHashMap;
import java.util.Map;
import com.shouyun.inventorylens.config.InventoryLensConfig;

/** Real-time visibility, keyed by stable entity/container identity. */
public final class WorldUiAnimator<K> {
	private static final int MAX_STATES = 4;
	private final LinkedHashMap<K, State> states = new LinkedHashMap<>();
	private long lastNanos = -1;

	public record Visual(float alpha, float scale, float offsetPixels) { }
	private static final class State {
		float progress;
		boolean visible;
	}

	public void show(K key) {
		State state = states.get(key);
		if (state == null) {
			if (states.size() >= MAX_STATES) states.remove(states.keySet().iterator().next());
			state = new State();
			states.put(key, state);
		}
		state.visible = true;
	}

	public void hideOthers(K key) {
		for (Map.Entry<K, State> entry : states.entrySet())
			if (!entry.getKey().equals(key)) entry.getValue().visible = false;
	}

	public void update(long nowNanos, InventoryLensConfig.Animation config) {
		float seconds = lastNanos < 0 ? 0 : Math.max(0, (nowNanos - lastNanos) / 1_000_000_000f);
		lastNanos = nowNanos;
		for (var state : states.values()) {
			if (!config.enabled) state.progress = state.visible ? 1 : 0;
			else state.progress = Math.clamp(state.progress + seconds * 1000f /
					(state.visible ? config.enterDurationMs : -config.exitDurationMs), 0, 1);
		}
		states.entrySet().removeIf(entry -> !entry.getValue().visible && entry.getValue().progress <= 0);
	}

	public Visual visual(K key) {
		State state = states.get(key);
		if (state == null) return new Visual(0, 0.88f, -4);
		float t = state.progress;
		// One curve for both directions keeps the visual value continuous on reversal.
		float eased = 1 - (float)Math.pow(1 - t, 3);
		return new Visual(eased, 0.88f + 0.12f * eased, -4 * (1 - eased));
	}

	public boolean contains(K key) { return states.containsKey(key); }
	public Iterable<K> keys() { return states.keySet(); }
	public void clear() { states.clear(); lastNanos = -1; }
}
