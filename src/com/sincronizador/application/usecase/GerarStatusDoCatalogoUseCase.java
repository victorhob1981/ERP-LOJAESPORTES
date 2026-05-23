package com.sincronizador.application.usecase;

import com.sincronizador.application.dto.EstadoProdutoCatalogo;
import com.sincronizador.application.dto.ProdutoCatalogoStatusDTO;
import com.sincronizador.application.port.CatalogoReader;
import com.sincronizador.application.port.EstoqueReader;
import com.sincronizador.domain.model.Disponibilidade;
import com.sincronizador.domain.model.ItemDeCatalogo;
import com.sincronizador.domain.model.ResultadoComparacaoTamanhos;
import com.sincronizador.domain.model.SKU;
import com.sincronizador.domain.service.ComparadorDeTamanhos;
import com.sincronizador.domain.service.GeradorDeLegenda;
import com.sincronizador.domain.valueobject.Tipo;

import java.util.*;

public class GerarStatusDoCatalogoUseCase {

    private final EstoqueReader estoqueReader;
    private final CatalogoReader catalogoReader;

    public GerarStatusDoCatalogoUseCase(EstoqueReader estoqueReader, CatalogoReader catalogoReader) {
        this.estoqueReader = Objects.requireNonNull(estoqueReader);
        this.catalogoReader = Objects.requireNonNull(catalogoReader);
    }

    public List<ProdutoCatalogoStatusDTO> executar() {

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

        Set<SKU> todos = new HashSet<>();
        todos.addAll(erpPorSku.keySet());
        todos.addAll(drivePorSku.keySet());

        ComparadorDeTamanhos comparador = new ComparadorDeTamanhos();

        List<ProdutoCatalogoStatusDTO> out = new ArrayList<>();

        // ordena para ficar estável/bonito
        List<SKU> ordenado = new ArrayList<>(todos);
        ordenado.sort(Comparator.comparing(this::skuSortKey));

        for (SKU sku : ordenado) {

            Disponibilidade dispErp = erpPorSku.get(sku);
            ItemDeCatalogo itemDrive = drivePorSku.get(sku);

            EstadoProdutoCatalogo estado;

            if (dispErp != null && itemDrive == null) {
                estado = EstadoProdutoCatalogo.SEM_IMAGEM;
            } else if (dispErp == null && itemDrive != null) {
                estado = EstadoProdutoCatalogo.ORFAO;
            } else if (dispErp != null) {

                // ✅ Ajuste de robustez para INFANTIL:
                // Antes da primeira sync, itens antigos no Drive podem não ter sku_tamanhos_fabrica.
                // Nessa fase, o Reader pode retornar disponibilidade "vazia" (não comparável).
                // Como o nome do arquivo é vitrine (idade), NÃO usamos o nome como verdade técnica.
                // Então evitamos falsos DESATUALIZADO até a próxima sync aplicar os metadados.
                if (sku != null && sku.getTipo() == Tipo.INFANTIL && itemDrive != null) {
                    boolean driveSemTamanhosTecnicos =
                            itemDrive.getDisponibilidade() == null
                                    || itemDrive.getDisponibilidade().getTamanhosDisponiveis() == null
                                    || itemDrive.getDisponibilidade().getTamanhosDisponiveis().isEmpty();

                    if (driveSemTamanhosTecnicos) {
                        estado = EstadoProdutoCatalogo.OK;
                    } else {
                        ResultadoComparacaoTamanhos comp = comparador.comparar(dispErp, itemDrive);
                        estado = (comp == ResultadoComparacaoTamanhos.IGUAIS)
                                ? EstadoProdutoCatalogo.OK
                                : EstadoProdutoCatalogo.DESATUALIZADO;
                    }
                } else {
                    ResultadoComparacaoTamanhos comp = comparador.comparar(dispErp, itemDrive);
                    estado = (comp == ResultadoComparacaoTamanhos.IGUAIS)
                            ? EstadoProdutoCatalogo.OK
                            : EstadoProdutoCatalogo.DESATUALIZADO;
                }

            } else {
                // não deveria acontecer, mas por segurança:
                estado = EstadoProdutoCatalogo.ORFAO;
            }

            String nomeProduto = (sku == null) ? "" : sku.toString();

            // ✅ tamanhosResumo vindo do ERP (quando existir)
            String tamanhosResumo = "—";
            if (dispErp != null) {
                // GeradorDeLegenda aplica a regra do INFANTIL (idades) e a ordem adulto.
                String legenda = GeradorDeLegenda.gerarLegenda(dispErp);
                tamanhosResumo = extrairParteDepoisDoHifen(legenda);
                if (tamanhosResumo == null || tamanhosResumo.isBlank()) tamanhosResumo = "—";
            }

            out.add(new ProdutoCatalogoStatusDTO(sku, nomeProduto, estado, tamanhosResumo));
        }

        return out;
    }

    private String extrairParteDepoisDoHifen(String legenda) {
        if (legenda == null) return "—";
        int idx = legenda.lastIndexOf(" - ");
        if (idx < 0) return "—";
        return legenda.substring(idx + 3).trim();
    }

    private String skuSortKey(SKU sku) {
        if (sku == null) return "";
        try {
            String clube = sku.getClube() == null ? "" : sku.getClube().trim();
            String modelo = sku.getModelo() == null ? "" : sku.getModelo().trim();
            String tipo = sku.getTipo() == null ? "" : sku.getTipo().name();
            return (clube + "|" + modelo + "|" + tipo).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return String.valueOf(sku).toUpperCase(Locale.ROOT);
        }
    }
}
