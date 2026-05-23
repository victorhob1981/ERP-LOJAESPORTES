package com.sincronizador.application.port;

import com.sincronizador.domain.model.Disponibilidade;
import com.sincronizador.domain.model.SKU;

import java.io.File;
import java.util.Set;

public interface CatalogoLocalWriter {

    boolean publicar(SKU sku, Disponibilidade disponibilidade, File imagemLocal);

    boolean remover(SKU sku);

    int removerAusentes(Set<SKU> skusAtivos);
}
