package com.shouyun.inventorylens.client.animation;

import com.shouyun.inventorylens.config.InventoryLensConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldUiAnimatorTest {
	@Test void reversalKeepsTheSameVisualAndUsesRealTime() {
		var animator = new WorldUiAnimator<String>();
		var config = new InventoryLensConfig.Animation();
		animator.show("chest");
		animator.update(0, config);
		animator.update(90_000_000, config);
		float halfway = animator.visual("chest").alpha();
		assertTrue(halfway > 0 && halfway < 1);
		animator.hideOthers(null);
		animator.update(90_000_000, config);
		assertEquals(halfway, animator.visual("chest").alpha());
		animator.update(130_000_000, config);
		float exiting = animator.visual("chest").alpha();
		animator.show("chest");
		animator.update(130_000_000, config);
		assertEquals(exiting, animator.visual("chest").alpha());
		animator.update(170_000_000, config);
		assertTrue(animator.visual("chest").alpha() > exiting);
	}

	@Test void disabledAnimationShowsAndRemovesImmediately() {
		var animator = new WorldUiAnimator<String>();
		var config = new InventoryLensConfig.Animation();
		config.enabled = false;
		animator.show("a");
		animator.update(0, config);
		assertEquals(1, animator.visual("a").alpha());
		animator.hideOthers(null);
		animator.update(1, config);
		assertFalse(animator.contains("a"));
	}

	@Test void switchingTargetsKeepsIndependentExitAndEntry() {
		var animator = new WorldUiAnimator<String>();
		var config = new InventoryLensConfig.Animation();
		animator.show("A");
		animator.update(0, config);
		animator.update(180_000_000, config);
		animator.show("B");
		animator.hideOthers("B");
		animator.update(180_000_000, config);
		assertEquals(1, animator.visual("A").alpha());
		assertEquals(0, animator.visual("B").alpha());
		animator.update(255_000_000, config);
		assertTrue(animator.visual("A").alpha() < 1);
		assertTrue(animator.visual("B").alpha() > 0);
		assertTrue(animator.contains("A") && animator.contains("B"));
	}
}
