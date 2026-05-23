package com.sincronizador.application.usecase;

import com.sincronizador.application.port.ImagemRepository;
import com.sincronizador.domain.model.SKU;

import java.io.File;
import java.util.Objects;

public class AssociarImagemAoCatalogoUseCase {

    private final ImagemRepository imagemRepository;

    public AssociarImagemAoCatalogoUseCase(ImagemRepository imagemRepository) {
        this.imagemRepository = Objects.requireNonNull(imagemRepository, "imagemRepository não pode ser nulo");
    }

    public void executar(SKU sku, File imagemLocal) {
        Objects.requireNonNull(sku, "sku não pode ser nulo");
        Objects.requireNonNull(imagemLocal, "imagemLocal não pode ser nulo");

        imagemRepository.salvarAssociacao(sku, imagemLocal);
    }
}
