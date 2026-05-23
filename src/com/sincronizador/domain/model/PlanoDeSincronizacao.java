package com.sincronizador.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlanoDeSincronizacao {

    private final List<AcaoDeSincronizacao> acoes = new ArrayList<>();

    public void adicionar(AcaoDeSincronizacao acao) {
        acoes.add(acao);
    }

    public List<AcaoDeSincronizacao> getAcoes() {
        return Collections.unmodifiableList(acoes);
    }

    public boolean estaVazio() {
        return acoes.isEmpty();
    }
}
