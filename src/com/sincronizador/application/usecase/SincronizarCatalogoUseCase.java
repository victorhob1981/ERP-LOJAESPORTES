package com.sincronizador.application.usecase;

import com.sincronizador.application.dto.ResultadoSincronizacaoDTO;
import com.sincronizador.application.port.CatalogoLocalWriter;
import com.sincronizador.application.port.CatalogoReader;
import com.sincronizador.application.port.CatalogoWriter;
import com.sincronizador.application.port.EstoqueReader;
import com.sincronizador.application.port.ImagemRepository;
import com.sincronizador.domain.model.Disponibilidade;
import com.sincronizador.domain.model.ItemDeCatalogo;
import com.sincronizador.domain.model.ResultadoComparacaoTamanhos;
import com.sincronizador.domain.model.SKU;
import com.sincronizador.domain.service.ComparadorDeTamanhos;
import com.sincronizador.domain.service.GeradorDeLegenda;

import java.io.File;
import java.util.*;

public class SincronizarCatalogoUseCase {

    @FunctionalInterface
    public interface ProgressoCallback {
        /**
         * @param atual   1..total
         * @param total   total de itens processados na execução
         * @param mensagem mensagem humana do passo atual
         */
        void onProgresso(int atual, int total, String mensagem);
    }

    private final EstoqueReader estoqueReader;
    private final CatalogoReader catalogoReader;
    private final CatalogoWriter catalogoWriter;
    private final ImagemRepository imagemRepository;
    private final CatalogoLocalWriter catalogoLocalWriter;

    public SincronizarCatalogoUseCase(
            EstoqueReader estoqueReader,
            CatalogoReader catalogoReader,
            CatalogoWriter catalogoWriter,
            ImagemRepository imagemRepository
    ) {
        this(estoqueReader, catalogoReader, catalogoWriter, imagemRepository, null);
    }

    public SincronizarCatalogoUseCase(
            EstoqueReader estoqueReader,
            CatalogoReader catalogoReader,
            CatalogoWriter catalogoWriter,
            ImagemRepository imagemRepository,
            CatalogoLocalWriter catalogoLocalWriter
    ) {
        this.estoqueReader = Objects.requireNonNull(estoqueReader);
        this.catalogoReader = Objects.requireNonNull(catalogoReader);
        this.catalogoWriter = Objects.requireNonNull(catalogoWriter);
        this.imagemRepository = Objects.requireNonNull(imagemRepository);
        this.catalogoLocalWriter = catalogoLocalWriter;
    }

    public ResultadoSincronizacaoDTO executar() {
        return executar(null);
    }

    public ResultadoSincronizacaoDTO executar(ProgressoCallback progresso) {

        List<Disponibilidade> estoque = estoqueReader.obterDisponibilidades();
        List<ItemDeCatalogo> itensCatalogo = catalogoReader.obterItens();

        Map<SKU, Disponibilidade> erpPorSku = new HashMap<>();
        for (Disponibilidade d : estoque) {
            if (d != null && d.getSku() != null) {
                erpPorSku.put(d.getSku(), d);
            }
        }

        Map<SKU, ItemDeCatalogo> drivePorSku = new HashMap<>();
        for (ItemDeCatalogo i : itensCatalogo) {
            if (i != null && i.getSku() != null) {
                drivePorSku.put(i.getSku(), i);
            }
        }

        int criados = 0;
        int atualizados = 0;
        int removidos = 0;
        int pendentesSemImagemLocal = 0;
        List<String> erros = new ArrayList<>();

        ComparadorDeTamanhos comparador = new ComparadorDeTamanhos();

        // --- prepara total para progresso ---
        int removiveis = 0;
        for (ItemDeCatalogo item : itensCatalogo) {
            if (item == null || item.getSku() == null) continue;
            SKU sku = item.getSku();
            if (ehPlaceholderCatalogo(sku)) continue;
            if (!erpPorSku.containsKey(sku)) removiveis++;
        }

        List<Map.Entry<SKU, Disponibilidade>> erpEntries = new ArrayList<>(erpPorSku.entrySet());
        erpEntries.sort(Comparator.comparing(e -> safeSkuKey(e.getKey())));

        int total = removiveis + erpEntries.size();
        int atual = 0;

        // 1) REMOVER do Drive o que não existe mais no ERP
        for (ItemDeCatalogo item : itensCatalogo) {
            if (item == null || item.getSku() == null) continue;

            SKU sku = item.getSku();

            // segurança p/ não apagar “arquivos velhos/sem identidade”
            if (ehPlaceholderCatalogo(sku)) continue;

            if (!erpPorSku.containsKey(sku)) {
                atual++;
                report(progresso, atual, total, "Removendo do Drive: " + sku);

                try {
                    catalogoWriter.remover(item.getIdExterno());
                    removidos++;
                } catch (Exception e) {
                    erros.add("REMOVER " + sku + " -> " + resumirErro(e));
                }

                try {
                    removerDoCatalogoLocal(sku);
                } catch (Exception e) {
                    erros.add("LOCAL REMOVER " + sku + " -> " + resumirErro(e));
                }
            }
        }

        // 2) CRIAR/ATUALIZAR no Drive baseado no ERP + imagem LOCAL
        for (Map.Entry<SKU, Disponibilidade> entry : erpEntries) {
            SKU sku = entry.getKey();
            Disponibilidade dispErp = entry.getValue();

            atual++;
            report(progresso, atual, total, "Processando: " + sku + " (" + atual + "/" + total + ")");

            Optional<File> imgOpt = imagemRepository.obterImagem(sku);
            if (imgOpt.isEmpty()) {
                pendentesSemImagemLocal++;
                continue;
            }

            File imgLocal = imgOpt.get();
            ItemDeCatalogo itemDrive = drivePorSku.get(sku);

            String legenda = GeradorDeLegenda.gerarLegenda(dispErp);

            // Se não existe no Drive, cria
            if (itemDrive == null) {
                report(progresso, atual, total, "Criando no Drive: " + sku + " (" + atual + "/" + total + ")");
                try {
                    catalogoWriter.criarComImagemLocal(sku, dispErp, imgLocal);
                    criados++;
                } catch (Exception e) {
                    erros.add("CRIAR " + sku + " -> " + resumirErro(e));
                }

                try {
                    publicarNoCatalogoLocal(sku, dispErp, imgLocal);
                } catch (Exception e) {
                    erros.add("LOCAL " + sku + " -> " + resumirErro(e));
                }
                continue;
            }

            boolean mudouAlgo = false;

            // Garante a metadata técnica de tamanhos, mas sem chamar o Drive se o reader já trouxe igual.
            try {
                report(progresso, atual, total, "Verificando metadados: " + sku + " (" + atual + "/" + total + ")");
                boolean metaMudou = catalogoWriter.atualizarTamanhosFabrica(
                        itemDrive.getIdExterno(),
                        dispErp,
                        itemDrive.getTamanhosFabricaMetadata()
                );
                if (metaMudou) mudouAlgo = true;
            } catch (Exception e) {
                erros.add("METADATA " + sku + " -> " + resumirErro(e));
            }

            // Atualiza tamanhos/legenda se precisar
            try {
                ResultadoComparacaoTamanhos comp = comparador.comparar(dispErp, itemDrive);
                if (comp != ResultadoComparacaoTamanhos.IGUAIS) {
                    report(progresso, atual, total, "Atualizando legenda: " + sku + " (" + atual + "/" + total + ")");
                    boolean legendaMudou = catalogoWriter.atualizarLegenda(
                            itemDrive.getIdExterno(),
                            legenda,
                            itemDrive.getNomeExterno()
                    );
                    if (legendaMudou) mudouAlgo = true;
                }
            } catch (Exception e) {
                erros.add("LEGENDA " + sku + " -> " + resumirErro(e));
            }

            // Atualiza imagem SOMENTE se de fato for diferente.
            try {
                report(progresso, atual, total, "Validando imagem: " + sku + " (" + atual + "/" + total + ")");
                boolean imagemMudou = catalogoWriter.trocarImagem(
                        itemDrive.getIdExterno(),
                        imgLocal,
                        itemDrive.getMd5Checksum()
                );
                if (imagemMudou) mudouAlgo = true;
            } catch (Exception e) {
                erros.add("IMAGEM " + sku + " -> " + resumirErro(e));
            }

            try {
                publicarNoCatalogoLocal(sku, dispErp, imgLocal);
            } catch (Exception e) {
                erros.add("LOCAL " + sku + " -> " + resumirErro(e));
            }

            if (mudouAlgo) atualizados++;
        }

        try {
            removerAusentesDoCatalogoLocal(erpPorSku.keySet());
        } catch (Exception e) {
            erros.add("LOCAL LIMPEZA -> " + resumirErro(e));
        }

        report(progresso, total, total, "Concluído (" + total + "/" + total + ")");
        return new ResultadoSincronizacaoDTO(criados, atualizados, removidos, pendentesSemImagemLocal, erros);
    }

    private void report(ProgressoCallback cb, int atual, int total, String msg) {
        if (cb == null) return;
        try {
            cb.onProgresso(atual, Math.max(total, 1), msg == null ? "" : msg);
        } catch (Exception ignored) {
            // progresso nunca pode quebrar a sincronização
        }
    }

    private void publicarNoCatalogoLocal(SKU sku, Disponibilidade disponibilidade, File imagemLocal) {
        if (catalogoLocalWriter == null) return;
        catalogoLocalWriter.publicar(sku, disponibilidade, imagemLocal);
    }

    private void removerDoCatalogoLocal(SKU sku) {
        if (catalogoLocalWriter == null) return;
        catalogoLocalWriter.remover(sku);
    }

    private void removerAusentesDoCatalogoLocal(Set<SKU> skusAtivos) {
        if (catalogoLocalWriter == null) return;
        catalogoLocalWriter.removerAusentes(skusAtivos);
    }

    private boolean ehPlaceholderCatalogo(SKU sku) {
        try {
            String modelo = sku.getModelo();
            return modelo != null && modelo.trim().equalsIgnoreCase("CATALOGO");
        } catch (Exception ignored) {
            return false;
        }
    }

    private String resumirErro(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return e.getClass().getSimpleName();
        msg = msg.replace("\n", " ").replace("\r", " ").trim();
        return msg.length() > 180 ? msg.substring(0, 180) + "..." : msg;
    }

    private String safeSkuKey(SKU sku) {
        if (sku == null) return "";
        try {
            String clube = sku.getClube() == null ? "" : sku.getClube().trim();
            String modelo = sku.getModelo() == null ? "" : sku.getModelo().trim();
            String tipo = (sku.getTipo() == null) ? "" : sku.getTipo().name();
            return (clube + "|" + modelo + "|" + tipo).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return String.valueOf(sku).toUpperCase(Locale.ROOT);
        }
    }
}
