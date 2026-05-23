package com.sincronizador.domain.model;

public class AcaoDeSincronizacao {

    public enum Tipo {
        CRIAR,
        ATUALIZAR,
        REMOVER,
        NENHUMA
    }

    private final Tipo tipo;
    private final SKU sku;
    private final String motivo;

    public AcaoDeSincronizacao(Tipo tipo, SKU sku, String motivo) {
        this.tipo = tipo;
        this.sku = sku;
        this.motivo = motivo;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public SKU getSku() {
        return sku;
    }

    public String getMotivo() {
        return motivo;
    }
}
