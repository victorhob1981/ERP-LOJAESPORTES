package com.sincronizador.application.port;

import com.sincronizador.domain.model.SKU;

import java.io.File;
import java.util.Optional;

public interface ImagemRepository {

    boolean possuiImagem(SKU sku);

    Optional<File> obterImagem(SKU sku);

    /**
     * Salva a associação de forma permanente (copiando o arquivo para a pasta do app)
     * e retorna o arquivo final salvo.
     */
    File salvarAssociacao(SKU sku, File imagemOrigem);

    void removerAssociacao(SKU sku);
}
