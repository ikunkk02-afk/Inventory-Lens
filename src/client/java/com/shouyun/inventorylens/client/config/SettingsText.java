package com.shouyun.inventorylens.client.config;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.google.gson.JsonParser;
import com.shouyun.inventorylens.InventoryLens;
import net.minecraft.network.chat.Component;

/** Optional per-screen language, without changing Minecraft's global language. */
final class SettingsText {
	private static final Map<String, String> ENGLISH = read("en_us");
	private static final Map<String, String> CHINESE = read("zh_cn");

	private SettingsText() { }

	private static Map<String, String> read(String language) {
		String path = "assets/inventorylens/lang/" + language + ".json";
		try (var stream = SettingsText.class.getClassLoader().getResourceAsStream(path)) {
			if (stream == null) throw new IllegalStateException("Missing " + path);
			var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			var values = new java.util.HashMap<String, String>();
			json.entrySet().forEach(entry -> values.put(entry.getKey(), entry.getValue().getAsString()));
			return Map.copyOf(values);
		} catch (Exception failure) {
			InventoryLens.LOGGER.warn("Cannot load Inventory Lens settings language {}", language, failure);
			return Map.of();
		}
	}

	static Component get(String language, String key, Object... args) {
		if ("auto".equals(language)) return Component.translatable(key, args);
		String template = ("zh_cn".equals(language) ? CHINESE : ENGLISH).get(key);
		if (template == null) return Component.translatable(key, args);
		return Component.literal(args.length == 0 ? template : String.format(java.util.Locale.ROOT, template, args));
	}
}
