package com.sincronizador.domain.model;

import com.sincronizador.domain.valueobject.Tamanho;

import java.util.Map;

public class Estoque {

    private final SKU sku;
    private final Map<Tamanho, Integer> quantidades;

    public Estoque(SKU sku, Map<Tamanho, Integer> quantidades) {
        this.sku = sku;
        this.quantidades = quantidades;
    }

    public SKU getSku() {
        return sku;
    }

    public boolean temEstoque() {
        return quantidades.values().stream().anyMatch(q -> q > 0);
    }

    public Map<Tamanho, Integer> getQuantidades() {
        return quantidades;
    }
}
