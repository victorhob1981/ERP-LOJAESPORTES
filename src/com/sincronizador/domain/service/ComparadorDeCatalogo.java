package com.sincronizador.domain.service;
import com.sincronizador.domain.service.ComparadorDeTamanhos;
import com.sincronizador.domain.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ComparadorDeCatalogo {

    public static PlanoDeSincronizacao comparar(
            List<Disponibilidade> disponibilidadesERP,
            List<ItemDeCatalogo> itensCatalogo
    ) {
        PlanoDeSincronizacao plano = new PlanoDeSincronizacao();
        IdentificadorDeCatalogo identificador = new IdentificadorDeCatalogo();

        // ERP → Catálogo
        for (Disponibilidade disponibilidade : disponibilidadesERP) {
            SKU sku = disponibilidade.getSku();

            ItemDeCatalogo itemCorrespondente = itensCatalogo.stream()
                    .filter(item -> identificador.representa(item, sku))
                    .findFirst()
                    .orElse(null);

            if (disponibilidade.estaDisponivel()) {
                if (itemCorrespondente == null) {
                    plano.adicionar(new AcaoDeSincronizacao(
                            AcaoDeSincronizacao.Tipo.CRIAR,
                            sku,
                            "Produto disponível no ERP e ausente no catálogo"
                    ));
                } else {
                   ComparadorDeTamanhos comparadorDeTamanhos = new ComparadorDeTamanhos();

ResultadoComparacaoTamanhos resultado =
        comparadorDeTamanhos.comparar(disponibilidade, itemCorrespondente);

if (resultado == ResultadoComparacaoTamanhos.DIFERENTES) {
    plano.adicionar(new AcaoDeSincronizacao(
            AcaoDeSincronizacao.Tipo.ATUALIZAR,
            sku,
            "Tamanhos divergentes entre ERP e catálogo"
    ));
}

                }
            }
        }

        // Catálogo → ERP
        for (ItemDeCatalogo item : itensCatalogo) {

            boolean existeNoERP = disponibilidadesERP.stream()
                    .anyMatch(d ->
                            identificador.representa(item, d.getSku())
                    );

            if (!existeNoERP) {
                plano.adicionar(new AcaoDeSincronizacao(
                        AcaoDeSincronizacao.Tipo.REMOVER,
                        item.getSku(),
                        "Produto não existe mais no ERP"
                ));
            }
        }

        return plano;
    }
}

