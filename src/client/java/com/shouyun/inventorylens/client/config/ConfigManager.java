package com.shouyun.inventorylens.client.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shouyun.inventorylens.InventoryLens;
import com.shouyun.inventorylens.config.InventoryLensConfig;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("inventorylens.json");
	private static InventoryLensConfig current = new InventoryLensConfig();

	private ConfigManager() { }
	public static InventoryLensConfig get() { return current; }
	public static void set(InventoryLensConfig config) { config.sanitize(); current = config; }
	public static InventoryLensConfig copy() { return GSON.fromJson(GSON.toJson(current), InventoryLensConfig.class); }

	public static void load() {
		if (Files.exists(FILE)) {
			try {
				InventoryLensConfig parsed = GSON.fromJson(Files.readString(FILE), InventoryLensConfig.class);
				if (parsed == null) throw new IOException("Empty config");
				set(parsed);
				return;
			} catch (IOException | RuntimeException failure) {
				InventoryLens.LOGGER.warn("Cannot read {}. Using defaults; preserving the broken file.", FILE, failure);
				try { Files.move(FILE, FILE.resolveSibling("inventorylens.json.broken"), StandardCopyOption.REPLACE_EXISTING); }
				catch (IOException backupFailure) { InventoryLens.LOGGER.warn("Cannot back up broken config", backupFailure); }
			}
		}
		current = new InventoryLensConfig();
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(current), StandardCharsets.UTF_8);
		} catch (IOException failure) {
			InventoryLens.LOGGER.warn("Cannot save {}", FILE, failure);
		}
	}
}
