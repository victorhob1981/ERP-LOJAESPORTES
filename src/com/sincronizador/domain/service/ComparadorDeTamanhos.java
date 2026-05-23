package com.sincronizador.domain.service;

import com.sincronizador.domain.model.Disponibilidade;
import com.sincronizador.domain.model.ItemDeCatalogo;
import com.sincronizador.domain.model.ResultadoComparacaoTamanhos;
import com.sincronizador.domain.valueobject.Tamanho;

import java.util.Collections;
import java.util.Set;

public class ComparadorDeTamanhos {

    public ResultadoComparacaoTamanhos comparar(
            Disponibilidade disponibilidadeERP,
            ItemDeCatalogo itemCatalogo
    ) {

        Set<Tamanho> tamanhosERP = (disponibilidadeERP == null || disponibilidadeERP.getTamanhosDisponiveis() == null)
                ? Collections.emptySet()
                : disponibilidadeERP.getTamanhosDisponiveis();

        Set<Tamanho> tamanhosCatalogo =
                (itemCatalogo == null || itemCatalogo.getDisponibilidade() == null || itemCatalogo.getDisponibilidade().getTamanhosDisponiveis() == null)
                        ? Collections.emptySet()
                        : itemCatalogo.getDisponibilidade().getTamanhosDisponiveis();

        if (tamanhosCatalogo.isEmpty()) {
            return ResultadoComparacaoTamanhos.CATALOGO_SEM_TAMANHOS;
        }

        if (tamanhosERP.equals(tamanhosCatalogo)) {
            return ResultadoComparacaoTamanhos.IGUAIS;
        }

        return ResultadoComparacaoTamanhos.DIFERENTES;
    }
}
