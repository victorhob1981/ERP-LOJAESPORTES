package com.sincronizador.infrastructure.drive;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.sincronizador.application.port.CatalogoReader;
import com.sincronizador.domain.model.*;
import com.sincronizador.domain.valueobject.Tamanho;
import com.sincronizador.domain.valueobject.Tipo;

import java.util.*;
import java.util.stream.Collectors;

public class DriveCatalogoReader implements CatalogoReader {

    private final Drive drive;
    private final String folderId;

    public DriveCatalogoReader(Drive drive, String folderId) {
        this.drive = drive;
        this.folderId = folderId;
    }

    @Override
    public List<ItemDeCatalogo> obterItens() {

        List<ItemDeCatalogo> itens = new ArrayList<>();

        try {
            FileList result = drive.files().list()
                    .setQ("'" + folderId + "' in parents and trashed = false")
                    .setFields("files(id,name,appProperties,md5Checksum)")
                    .execute();

            for (File file : result.getFiles()) {

                Optional<SKU> skuOpt = skuViaMetadata(file);
                if (skuOpt.isEmpty()) {
                    // sem metadata = não entra no catálogo (segurança pra não deletar por engano)
                    continue;
                }

                SKU sku = skuOpt.get();

                // ✅ NOVA REGRA:
                // INFANTIL: a verdade técnica (tamanhos de fábrica) vem da metadata.
                // ADULTO: pode vir da metadata (novo padrão) ou fallback via nome (legenda).
                Set<Tamanho> tamanhos = extrairTamanhosDoArquivo(file, sku);

                Map<Tamanho, Integer> quantidades = new EnumMap<>(Tamanho.class);
                for (Tamanho t : tamanhos) {
                    quantidades.put(t, 1);
                }

                Estoque estoque = new Estoque(sku, quantidades);
                Disponibilidade disponibilidade = Disponibilidade.aPartirDoEstoque(estoque);

                itens.add(new ItemDeCatalogo(
                        sku,
                        disponibilidade,
                        file.getId(),
                        file.getName(),
                        file.getMd5Checksum(),
                        obterTamanhosFabricaMetadata(file)
                ));
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler catálogo do Drive: " + e.getMessage(), e);
        }

        return itens;
    }

    private String obterTamanhosFabricaMetadata(File file) {
        Map<String, String> p = file.getAppProperties();
        if (p == null) return null;
        return p.get(DriveMetadataKeys.SKU_TAMANHOS_FABRICA);
    }

    private Set<Tamanho> extrairTamanhosDoArquivo(File file, SKU sku) {
        // 1) Tenta metadata de tamanhos de fábrica (novo padrão)
        Set<Tamanho> viaMetadata = extrairTamanhosFabricaDaMetadata(file);
        if (!viaMetadata.isEmpty()) return viaMetadata;

        // 2) Se for INFANTIL e ainda não migrou, não tenta parse do nome (porque nome = idades)
        //    Vai voltar vazio e ser marcado como “a migrar” até a próxima sync.
        if (sku != null && sku.getTipo() == Tipo.INFANTIL) {
            return Collections.emptySet();
        }

        // 3) Fallback: parse do nome (compatibilidade para ADULTO e itens antigos)
        return extrairTamanhosDaLegenda(file.getName());
    }

    /**
     * Lê "sku_tamanhos_fabrica" (ex.: "18,20,24,28" ou "P,M,GG,2GG").
     * - Numérico (18..28) -> enum _18.._28
     * - Texto (P/M/G...) -> enum correspondente
     */
    private Set<Tamanho> extrairTamanhosFabricaDaMetadata(File file) {
        Map<String, String> p = file.getAppProperties();
        if (p == null) return Collections.emptySet();

        String raw = p.get(DriveMetadataKeys.SKU_TAMANHOS_FABRICA);
        if (raw == null || raw.isBlank()) return Collections.emptySet();

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::mapearTamanhoFabrica)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Tamanho mapearTamanhoFabrica(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toUpperCase(Locale.ROOT);

        // aceita formatos com underscore vindo do enum (casos antigos)
        if (v.startsWith("_")) v = v.substring(1);

        // Adulto (texto)
        if (v.equals("P")) return Tamanho.P;
        if (v.equals("M")) return Tamanho.M;
        if (v.equals("G")) return Tamanho.G;
        if (v.equals("GG")) return Tamanho.GG;
        if (v.equals("2GG")) return Tamanho._2GG;
        if (v.equals("3GG")) return Tamanho._3GG;
        if (v.equals("4GG")) return Tamanho._4GG;

        // Infantil (numérico)
        if (v.equals("16")) return Tamanho._16;
        if (v.equals("18")) return Tamanho._18;
        if (v.equals("20")) return Tamanho._20;
        if (v.equals("22")) return Tamanho._22;
        if (v.equals("24")) return Tamanho._24;
        if (v.equals("26")) return Tamanho._26;
        if (v.equals("28")) return Tamanho._28;

        // fallback: tenta enum direto (P, GG, _24 etc.)
        try {
            return Tamanho.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private Optional<SKU> skuViaMetadata(File file) {
        Map<String, String> p = file.getAppProperties();
        if (p == null) return Optional.empty();

        String clube = p.get(DriveMetadataKeys.SKU_CLUBE);
        if (clube == null || clube.isBlank()) clube = p.get("clube");

        String modelo = p.get(DriveMetadataKeys.SKU_MODELO);
        if (modelo == null || modelo.isBlank()) modelo = p.get("modelo");

        String tipoStr = p.get(DriveMetadataKeys.SKU_TIPO);
        if (tipoStr == null || tipoStr.isBlank()) tipoStr = p.get("tipo");

        if ((clube == null || modelo == null || tipoStr == null) && p.containsKey(DriveMetadataKeys.SKU_KEY)) {
            String skuKey = p.get(DriveMetadataKeys.SKU_KEY);
            String[] partes = skuKey == null ? new String[0] : skuKey.split("\\|");
            if (partes.length == 3) {
                if (clube == null || clube.isBlank()) clube = partes[0];
                if (modelo == null || modelo.isBlank()) modelo = partes[1];
                if (tipoStr == null || tipoStr.isBlank()) tipoStr = partes[2];
            }
        }

        if (clube == null || modelo == null || tipoStr == null) return Optional.empty();

        Tipo tipo;
        try {
            tipo = Tipo.valueOf(tipoStr.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return Optional.empty();
        }

        Produto produto = new Produto(clube, modelo, tipo);
        return Optional.of(new SKU(produto));
    }

    private Set<Tamanho> extrairTamanhosDaLegenda(String legenda) {
        if (legenda == null) return Collections.emptySet();
        if (!legenda.contains("-")) return Collections.emptySet();

        String[] partes = legenda.split("-");
        if (partes.length != 2) return Collections.emptySet();

        String tamanhosStr = partes[1].trim();
        if (tamanhosStr.isBlank()) return Collections.emptySet();

        return Arrays.stream(tamanhosStr.split(","))
                .map(String::trim)
                .map(this::mapearTamanho)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Tamanho mapearTamanho(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toUpperCase(Locale.ROOT);

        if (v.equals("P")) return Tamanho.P;
        if (v.equals("M")) return Tamanho.M;
        if (v.equals("G")) return Tamanho.G;
        if (v.equals("GG")) return Tamanho.GG;
        if (v.equals("2GG")) return Tamanho._2GG;
        if (v.equals("3GG")) return Tamanho._3GG;
        if (v.equals("4GG")) return Tamanho._4GG;

        if (v.equals("16")) return Tamanho._16;
        if (v.equals("18")) return Tamanho._18;
        if (v.equals("20")) return Tamanho._20;
        if (v.equals("22")) return Tamanho._22;
        if (v.equals("24")) return Tamanho._24;
        if (v.equals("26")) return Tamanho._26;
        if (v.equals("28")) return Tamanho._28;

        try {
            return Tamanho.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }
}
