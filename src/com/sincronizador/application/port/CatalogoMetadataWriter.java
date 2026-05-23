package com.sincronizador.application.port;

import com.sincronizador.domain.model.SKU;

public interface CatalogoMetadataWriter {
    void vincularSku(String fileId, SKU sku);
}
