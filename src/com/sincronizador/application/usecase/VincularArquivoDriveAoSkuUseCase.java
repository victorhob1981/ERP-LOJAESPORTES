package com.sincronizador.application.usecase;

import com.sincronizador.application.port.CatalogoMetadataWriter;
import com.sincronizador.domain.model.SKU;

import java.util.Objects;

public class VincularArquivoDriveAoSkuUseCase {

    private final CatalogoMetadataWriter writer;

    public VincularArquivoDriveAoSkuUseCase(CatalogoMetadataWriter writer) {
        this.writer = Objects.requireNonNull(writer, "writer não pode ser nulo");
    }

    public void executar(String fileId, SKU sku) {
        writer.vincularSku(fileId, sku);
    }
}
