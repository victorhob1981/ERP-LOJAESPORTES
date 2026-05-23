package com.sincronizador.domain.service;

import com.sincronizador.domain.model.Disponibilidade;
import com.sincronizador.domain.model.SKU;
import com.sincronizador.domain.valueobject.Tamanho;
import com.sincronizador.domain.valueobject.Tipo;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public class GeradorDeLegenda {

    // Conversão tamanho infantil (número) -> idades
    // 16 = 3~4, 18 = 4~5, 20 = 5~6, 22 = 6~7, 24 = 8~9, 26 = 10~11, 28 = 12~13
    private static final Map<Integer, int[]> TAMANHO_PARA_IDADES = new HashMap<>();
    static {
        TAMANHO_PARA_IDADES.put(16, new int[]{3, 4});
        TAMANHO_PARA_IDADES.put(18, new int[]{4, 5});
        TAMANHO_PARA_IDADES.put(20, new int[]{5, 6});
        TAMANHO_PARA_IDADES.put(22, new int[]{6, 7});
        TAMANHO_PARA_IDADES.put(24, new int[]{8, 9});
        TAMANHO_PARA_IDADES.put(26, new int[]{10, 11});
        TAMANHO_PARA_IDADES.put(28, new int[]{12, 13});
    }

    public static String gerarLegenda(Disponibilidade disponibilidade) {
        SKU sku = disponibilidade.getSku();

        String clubeNorm = normalizar(sku.getClube()).toUpperCase(Locale.ROOT);
        String clube3 = clubeNorm.length() <= 3 ? clubeNorm : clubeNorm.substring(0, 3);

        // ✅ INFANTIL: converte tamanhos numéricos -> idades (ex: 24 -> "8, 9")
        if (sku.getTipo() == Tipo.INFANTIL) {
            String idades = gerarListaIdadesInfantil(disponibilidade);

            // fallback: se não conseguiu converter, mostra os tamanhos mesmo (ordenados)
            if (idades.isBlank()) {
                String tamanhosFallback = disponibilidade.getTamanhosDisponiveis().stream()
                        .sorted(Comparator.comparingInt(GeradorDeLegenda::ordemTamanhoAdultoOuNumerico))
                        .map(Tamanho::getValorExibicao)
                        .collect(Collectors.joining(", "));
                return clube3 + " INFANTIL - " + tamanhosFallback;
            }

            return clube3 + " INFANTIL - " + idades;
        }

        // ✅ Adulto (mantém padrão atual): tamanhos ordenados P, M, G, GG, 2GG, 3GG, 4GG
        String tamanhos = disponibilidade.getTamanhosDisponiveis()
                .stream()
                .sorted(Comparator.comparingInt(GeradorDeLegenda::ordemTamanhoAdultoOuNumerico))
                .map(Tamanho::getValorExibicao)
                .collect(Collectors.joining(", "));

        return clube3 + " - " + tamanhos;
    }

    private static String gerarListaIdadesInfantil(Disponibilidade disponibilidade) {
        // Usa TreeSet para ordenar e remover duplicados automaticamente
        Set<Integer> idades = new TreeSet<>();

        for (Tamanho t : disponibilidade.getTamanhosDisponiveis()) {
            String v = t.getValorExibicao();
            Integer tamanhoNum = tentarParseInt(v);
            if (tamanhoNum == null) continue;

            int[] faixa = TAMANHO_PARA_IDADES.get(tamanhoNum);
            if (faixa == null || faixa.length != 2) continue;

            // Ex.: 24 -> adiciona 8 e 9
            idades.add(faixa[0]);
            idades.add(faixa[1]);
        }

        return idades.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    private static Integer tentarParseInt(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;

        // aceita apenas números puros (16, 18, 22, 24...)
        for (int i = 0; i < v.length(); i++) {
            if (!Character.isDigit(v.charAt(i))) return null;
        }

        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return null;
        }
    }

    // Ordenação “inteligente”:
    // - Se for numérico (infantil), ordena pelo número
    // - Se for adulto, ordena pela ordem desejada: P, M, G, GG, 2GG, 3GG, 4GG
    private static int ordemTamanhoAdultoOuNumerico(Tamanho tamanho) {
        String v = tamanho.getValorExibicao();
        Integer num = tentarParseInt(v);
        if (num != null) return 1000 + num; // joga numéricos pro final, mas ordenados

        // adulto
        return switch (v) {
            case "P" -> 1;
            case "M" -> 2;
            case "G" -> 3;
            case "GG" -> 4;
            case "2GG" -> 5;
            case "3GG" -> 6;
            case "4GG" -> 7;
            default -> 999;
        };
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return normalizado.replaceAll("[^\\p{ASCII}]", "");
    }
}
