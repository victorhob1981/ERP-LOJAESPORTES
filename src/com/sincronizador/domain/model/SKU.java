package com.sincronizador.domain.model;

import com.sincronizador.domain.valueobject.Tipo;

import java.util.Objects;

public class SKU {

    private final Produto produto;

    public SKU(Produto produto) {
        this.produto = Objects.requireNonNull(produto, "produto não pode ser nulo");
    }

    public Produto getProduto() {
        return produto;
    }

    public String getClube() {
        return produto.getClube();
    }

    public String getModelo() {
        return produto.getModelo();
    }

    public Tipo getTipo() {
        return produto.getTipo();
    }

    /**
     * Representação legível para UI/logs.
     * Ex: "VASCO AWAY 2025 (MASCULINO)".
     */
    @Override
    public String toString() {
        String clube = safeUpper(getClube());
        String modelo = safeUpper(getModelo());
        String tipo = (getTipo() == null) ? "" : getTipo().name();

        String base = (clube + " " + modelo).trim();
        return tipo.isBlank() ? base : base + " (" + tipo + ")";
    }

    private static String safeUpper(String s) {
        return (s == null) ? "" : s.trim().toUpperCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SKU)) return false;
        SKU sku = (SKU) o;
        return Objects.equals(produto.getClube(), sku.produto.getClube())
                && Objects.equals(produto.getModelo(), sku.produto.getModelo())
                && produto.getTipo() == sku.produto.getTipo();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                produto.getClube(),
                produto.getModelo(),
                produto.getTipo()
        );
    }
}
