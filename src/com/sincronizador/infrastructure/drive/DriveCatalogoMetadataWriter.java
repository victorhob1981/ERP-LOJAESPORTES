package com.sincronizador.infrastructure.drive;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.sincronizador.application.port.CatalogoMetadataWriter;
import com.sincronizador.domain.model.SKU;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DriveCatalogoMetadataWriter implements CatalogoMetadataWriter {

    private final Drive drive;

    public DriveCatalogoMetadataWriter(Drive drive) {
        this.drive = Objects.requireNonNull(drive, "drive não pode ser nulo");
    }

    @Override
    public void vincularSku(String fileId, SKU sku) {
        Objects.requireNonNull(fileId, "fileId não pode ser nulo");
        Objects.requireNonNull(sku, "sku não pode ser nulo");

        Map<String, String> appProps = new HashMap<>();
        appProps.put(DriveMetadataKeys.SKU_CLUBE, sku.getClube());
        appProps.put(DriveMetadataKeys.SKU_MODELO, sku.getModelo());
        appProps.put(DriveMetadataKeys.SKU_TIPO, sku.getTipo().name());
        appProps.put(
                DriveMetadataKeys.SKU_KEY,
                (sku.getClube() + "|" + sku.getModelo() + "|" + sku.getTipo().name()).toUpperCase(Locale.ROOT)
        );

        File metadata = new File();
        metadata.setAppProperties(appProps);

        try {
            drive.files()
                    .update(fileId, metadata)
                    .setFields("id, appProperties")
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao vincular metadados no arquivo do Drive: " + fileId, e);
        }
    }
}
