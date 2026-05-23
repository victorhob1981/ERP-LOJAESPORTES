package com.sincronizador.domain.model;

import com.sincronizador.domain.valueobject.Tamanho;
import com.sincronizador.domain.valueobject.Tipo;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Disponibilidade {

    private final SKU sku;
    private final Set<Tamanho> tamanhosDisponiveis;

    private Disponibilidade(SKU sku, Set<Tamanho> tamanhosDisponiveis) {
        this.sku = sku;
        this.tamanhosDisponiveis = tamanhosDisponiveis;
    }

    public SKU getSku() {
        return sku;
    }

    public Set<Tamanho> getTamanhosDisponiveis() {
        return tamanhosDisponiveis;
    }

    public boolean estaDisponivel() {
        return !tamanhosDisponiveis.isEmpty();
    }

    /**
     * Fábrica de Disponibilidade a partir do estoque
     */
    public static Disponibilidade aPartirDoEstoque(Estoque estoque) {
        Tipo tipo = estoque.getSku().getTipo();

        Set<Tamanho> tamanhosValidos = tamanhosValidosPorTipo(tipo);

        Set<Tamanho> disponiveis = estoque.getQuantidades().entrySet()
                .stream()
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .filter(tamanhosValidos::contains)
                .collect(Collectors.toSet());

        return new Disponibilidade(estoque.getSku(), disponiveis);
    }

    /**
     * Define quais tamanhos são permitidos para cada tipo
     */
    private static Set<Tamanho> tamanhosValidosPorTipo(Tipo tipo) {
        if (tipo == Tipo.INFANTIL) {
            return EnumSet.of(
                    Tamanho._16,
                    Tamanho._18,
                    Tamanho._20,
                    Tamanho._22,
                    Tamanho._24,
                    Tamanho._26,
                    Tamanho._28
            );
        }

        // Masculino e Feminino
        return EnumSet.of(
                Tamanho.P,
                Tamanho.M,
                Tamanho.G,
                Tamanho.GG,
                Tamanho._2GG,
                Tamanho._3GG,
                Tamanho._4GG
        );
    }
}
