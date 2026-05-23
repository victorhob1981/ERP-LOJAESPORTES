package com.sincronizador.application.port;

import com.sincronizador.domain.model.Disponibilidade;
import com.sincronizador.domain.model.SKU;

import java.io.File;

public interface CatalogoWriter {

    String criarComImagemLocal(SKU sku, Disponibilidade disponibilidade, File imagemLocal);

    boolean atualizarLegenda(String fileId, String novoNome);

    boolean atualizarLegenda(String fileId, String novoNome, String nomeAtual);

    boolean trocarImagem(String fileId, File novaImagemLocal);

    boolean trocarImagem(String fileId, File novaImagemLocal, String md5Remoto);

    void vincularSku(String fileId, SKU sku);

    void remover(String fileId);

    /**
     * Atualiza/garante a metadata técnica (tamanhos de fábrica) no arquivo do Drive.
     * Retorna true se realmente precisou alterar algo, false se já estava igual.
     */
    boolean atualizarTamanhosFabrica(String fileId, Disponibilidade disponibilidade);

    boolean atualizarTamanhosFabrica(String fileId, Disponibilidade disponibilidade, String tamanhosFabricaAtual);
}
