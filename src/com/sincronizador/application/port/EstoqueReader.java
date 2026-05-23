package com.sincronizador.application.port;

import com.sincronizador.domain.model.Disponibilidade;

import java.util.List;

public interface EstoqueReader {

    List<Disponibilidade> obterDisponibilidades();
}
