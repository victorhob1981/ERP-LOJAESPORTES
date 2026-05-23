package com.sincronizador.application.dto;

import java.util.Collections;
import java.util.List;

public class ResultadoSincronizacaoDTO {

    private final int criados;
    private final int atualizados;
    private final int removidos;
    private final int pendentesCriacaoSemImagem;
    private final List<String> erros;

    // Construtor novo (com criados)
    public ResultadoSincronizacaoDTO(
            int criados,
            int atualizados,
            int removidos,
            int pendentesCriacaoSemImagem,
            List<String> erros
    ) {
        this.criados = criados;
        this.atualizados = atualizados;
        this.removidos = removidos;
        this.pendentesCriacaoSemImagem = pendentesCriacaoSemImagem;
        this.erros = (erros == null) ? List.of() : erros;
    }

    // Construtor antigo (mantém compatibilidade)
    public ResultadoSincronizacaoDTO(
            int atualizados,
            int removidos,
            int pendentesCriacaoSemImagem,
            List<String> erros
    ) {
        this(0, atualizados, removidos, pendentesCriacaoSemImagem, erros);
    }

    public int getCriados() {
        return criados;
    }

    public int getAtualizados() {
        return atualizados;
    }

    public int getRemovidos() {
        return removidos;
    }

    // mantém o getter “do jeitinho” que seu controller já chamava
    public int getPendentesCriacaoSemimagem() {
        return pendentesCriacaoSemImagem;
    }

    public List<String> getErros() {
        return Collections.unmodifiableList(erros);
    }

    public boolean temErros() {
        return !erros.isEmpty();
    }
}
