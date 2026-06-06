package com.sincronizador.config;

import java.nio.file.Path;

public final class CatalogoDataConfig {

    public static final String PROPERTY_DATA_DIR = "sincronizador.catalogo.data.dir";

    private CatalogoDataConfig() {}

    public static Path resolverCatalogoDataDir() {
        String configured = System.getProperty(PROPERTY_DATA_DIR);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim());
        }

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "ERP 2.0", "data", "catalogo");
        }

        return Path.of(System.getProperty("user.home"), "AppData", "Local", "ERP 2.0", "data", "catalogo");
    }

    public static Path resolverCatalogoLocalIndexFile() {
        return resolverCatalogoDataDir().resolve("catalogo-local.properties");
    }
}
