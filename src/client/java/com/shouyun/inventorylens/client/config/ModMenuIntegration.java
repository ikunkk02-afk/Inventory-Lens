package com.shouyun.inventorylens.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Loaded only by Mod Menu's optional entrypoint. */
public final class ModMenuIntegration implements ModMenuApi {
	@Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return InventoryLensConfigScreen::new;
	}
}
