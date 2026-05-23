package com.sincronizador.application.dto;

import com.sincronizador.domain.model.SKU;

public class ProdutoCatalogoStatusDTO {

    private final SKU sku;
    private final String nomeProduto;
    private final EstadoProdutoCatalogo estado;

    // NOVO: tamanhos/idades para exibir no painel (vem do ERP quando existir)
    private final String tamanhosResumo;

    /**
     * Construtor antigo (mantido para NÃO quebrar outros pontos do projeto).
     * Quando usado, tamanhosResumo ficará como "—".
     */
    public ProdutoCatalogoStatusDTO(
            SKU sku,
            String nomeProduto,
            EstadoProdutoCatalogo estado
    ) {
        this(sku, nomeProduto, estado, "—");
    }

    /**
     * Construtor novo com tamanhosResumo.
     */
    public ProdutoCatalogoStatusDTO(
            SKU sku,
            String nomeProduto,
            EstadoProdutoCatalogo estado,
            String tamanhosResumo
    ) {
        this.sku = sku;
        this.nomeProduto = nomeProduto;
        this.estado = estado;
        this.tamanhosResumo = (tamanhosResumo == null || tamanhosResumo.isBlank()) ? "—" : tamanhosResumo;
    }

    public SKU getSku() {
        return sku;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public EstadoProdutoCatalogo getEstado() {
        return estado;
    }

    public String getTamanhosResumo() {
        return tamanhosResumo;
    }
}
