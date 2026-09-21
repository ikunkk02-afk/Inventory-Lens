package com.shouyun.inventorylens.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryLensConfigTest {
	@Test void oldPartialConfigurationKeepsNewDefaults() {
		InventoryLensConfig config = new Gson().fromJson("{\"general\":{\"scale\":1.5}}", InventoryLensConfig.class);
		config.sanitize();
		assertEquals(1.5, config.general.scale);
		assertEquals(1, config.entity.scale);
		assertEquals(180, config.animation.enterDurationMs);
		assertEquals("auto", config.language);
		assertTrue(config.visibility.entity);
		assertTrue(config.visibility.containerEnabled("chest"));
	}

	@Test void visibilityKeepsContainerTypesIndependentAndGroupsChestHalves() {
		InventoryLensConfig config = new InventoryLensConfig();
		config.visibility.setTypeEnabled("chest", false);
		assertFalse(config.visibility.containerEnabled("chest"));
		assertFalse(config.visibility.containerEnabled("double_chest"));
		assertFalse(config.visibility.containerEnabled("trapped_chest"));
		assertTrue(config.visibility.containerEnabled("barrel"));
		config.visibility.entity = false;
		assertTrue(config.visibility.containerEnabled("barrel"));
		config.visibility.setTypeEnabled("chest", true);
		assertTrue(config.visibility.containerEnabled("double_trapped_chest"));
	}

	@Test void invalidValuesCannotBreakWorldMatrices() {
		InventoryLensConfig config = new InventoryLensConfig();
		config.general.scale = Double.POSITIVE_INFINITY;
		config.container.scale = 1000;
		config.entity.horizontalOffset = Double.NaN;
		config.animation.exitDurationMs = -10;
		config.sanitize();
		assertEquals(1, config.general.scale);
		assertEquals(2, config.container.scale);
		assertEquals(0, config.entity.horizontalOffset);
		assertEquals(50, config.animation.exitDurationMs);
	}
}
