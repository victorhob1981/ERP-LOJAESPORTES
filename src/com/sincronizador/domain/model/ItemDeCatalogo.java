package com.sincronizador.domain.model;

import java.util.Objects;

public class ItemDeCatalogo {

    private final SKU sku;
    private final Disponibilidade disponibilidade;
    private final String idExterno;
    private final String nomeExterno;
    private final String md5Checksum;
    private final String tamanhosFabricaMetadata;

    public ItemDeCatalogo(SKU sku, Disponibilidade disponibilidade, String idExterno) {
        this(sku, disponibilidade, idExterno, null, null, null);
    }

    public ItemDeCatalogo(
            SKU sku,
            Disponibilidade disponibilidade,
            String idExterno,
            String nomeExterno,
            String md5Checksum,
            String tamanhosFabricaMetadata
    ) {
        this.sku = Objects.requireNonNull(sku, "sku não pode ser nulo");
        this.disponibilidade = Objects.requireNonNull(disponibilidade, "disponibilidade não pode ser nula");
        this.idExterno = Objects.requireNonNull(idExterno, "idExterno não pode ser nulo");
        this.nomeExterno = nomeExterno;
        this.md5Checksum = md5Checksum;
        this.tamanhosFabricaMetadata = tamanhosFabricaMetadata;
    }

    public SKU getSku() {
        return sku;
    }

    public Disponibilidade getDisponibilidade() {
        return disponibilidade;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public String getNomeExterno() {
        return nomeExterno;
    }

    public String getMd5Checksum() {
        return md5Checksum;
    }

    public String getTamanhosFabricaMetadata() {
        return tamanhosFabricaMetadata;
    }

    public boolean estaAtivo() {
        return disponibilidade.estaDisponivel();
    }
}
