package com.sincronizador.application.port;

import com.sincronizador.domain.model.ItemDeCatalogo;

import java.util.List;

public interface CatalogoReader {

    List<ItemDeCatalogo> obterItens();
}
