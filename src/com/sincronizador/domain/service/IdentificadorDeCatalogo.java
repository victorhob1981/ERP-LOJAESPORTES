package com.sincronizador.domain.service;

import com.sincronizador.domain.model.ItemDeCatalogo;
import com.sincronizador.domain.model.SKU;

import java.text.Normalizer;
import java.util.Locale;

public class IdentificadorDeCatalogo {

    public boolean representa(ItemDeCatalogo item, SKU sku) {
    return item.getSku().equals(sku);
}


    @SuppressWarnings("unused")
    private String normalizar(String texto) {
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return normalizado
                .replaceAll("[^\\p{ASCII}]", "")
                .toUpperCase(Locale.ROOT);
    }
}
