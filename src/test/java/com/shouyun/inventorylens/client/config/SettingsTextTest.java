package com.shouyun.inventorylens.client.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingsTextTest {
	@Test void selectedLanguageOverridesGameLanguage() {
		assertEquals("Inventory Lens 设置", SettingsText.get("zh_cn", "inventorylens.settings.title").getString());
		assertEquals("Inventory Lens Settings", SettingsText.get("en_us", "inventorylens.settings.title").getString());
		assertEquals("当前大小：1.50×", SettingsText.get("zh_cn", "inventorylens.settings.preview.scale", "1.50×").getString());
	}
}
