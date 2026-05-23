package com.sincronizador.infrastructure.erp;

import com.sincronizador.application.port.EstoqueReader;
import com.sincronizador.domain.model.Disponibilidade;
import com.sincronizador.domain.model.Estoque;
import com.sincronizador.domain.model.Produto;
import com.sincronizador.domain.model.SKU;
import com.sincronizador.domain.valueobject.Tamanho;
import com.sincronizador.domain.valueobject.Tipo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.lang.reflect.Method;
import java.util.*;

public class ErpEstoqueReader implements EstoqueReader {

    // TODO (final): mover pra config/env, não deixar senha no código
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/gemini_teste";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "Senhalp3";

    // ✅ Plano A: só traz o que está em estoque (> 0)
    private static final String SQL_ESTOQUE = """
                SELECT Clube, Modelo, Tipo, Tamanho, QuantidadeEstoque
                FROM produtos
                WHERE QuantidadeEstoque > 0
            """;

    @Override
    public List<Disponibilidade> obterDisponibilidades() {

        Map<SKU, Map<Tamanho, Integer>> estoqueAgrupado = new HashMap<>();

        try (Connection connection = abrirConexao();
                PreparedStatement stmt = connection.prepareStatement(SQL_ESTOQUE);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                String clube = rs.getString("Clube");
                String modelo = rs.getString("Modelo");

                Tipo tipo = mapearTipo(rs.getString("Tipo"));
                Tamanho tamanho = mapearTamanho(rs.getString("Tamanho"));

                int quantidade = rs.getInt("QuantidadeEstoque");

                // ✅ Redundante (porque já filtrou na query), mas deixa blindado
                if (quantidade <= 0)
                    continue;

                if (clube == null || modelo == null || tipo == null || tamanho == null) {
                    continue;
                }

                Produto produto = new Produto(clube, modelo, tipo);
                SKU sku = new SKU(produto);

                Map<Tamanho, Integer> porTamanho = estoqueAgrupado
                        .computeIfAbsent(sku, k -> new EnumMap<>(Tamanho.class));

                // soma caso exista linha duplicada por tamanho
                porTamanho.merge(tamanho, quantidade, Integer::sum);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler estoque do ERP", e);
        }

        List<Disponibilidade> disponibilidades = new ArrayList<>();

        for (Map.Entry<SKU, Map<Tamanho, Integer>> entry : estoqueAgrupado.entrySet()) {
            Estoque estoque = new Estoque(entry.getKey(), entry.getValue());
            Disponibilidade disponibilidade = Disponibilidade.aPartirDoEstoque(estoque);

            // ✅ Com a query >0 isso já vem sempre com tamanhos,
            // mas deixo a blindagem para nunca devolver SKU “vazio”
            if (disponibilidade != null && disponibilidade.estaDisponivel()) {
                disponibilidades.add(disponibilidade);
            }
        }

        return disponibilidades;
    }

    private Tipo mapearTipo(String tipoBanco) {
        if (tipoBanco == null)
            return null;

        String v = tipoBanco.trim().toLowerCase(Locale.ROOT);

        return switch (v) {
            case "masculino", "masculina", "m" -> Tipo.MASCULINO;
            case "feminino", "feminina", "f" -> Tipo.FEMININO;
            case "infantil", "i" -> Tipo.INFANTIL;
            default -> null;
        };
    }

    private Tamanho mapearTamanho(String tamanhoBanco) {
        if (tamanhoBanco == null)
            return null;

        String valor = tamanhoBanco.trim().toUpperCase(Locale.ROOT);

        try {

            if (!valor.isEmpty() && Character.isDigit(valor.charAt(0))) {
                return Tamanho.valueOf("_" + valor);
            }
            return Tamanho.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Connection abrirConexao() throws Exception {
        Optional<Connection> conexaoErp = tentarAbrirConexaoViaErp();
        if (conexaoErp.isPresent()) return conexaoErp.get();
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    private Optional<Connection> tentarAbrirConexaoViaErp() {
        try {
            Class<?> conexaoBancoClass = Class.forName("UTIL.ConexaoBanco");
            Method conectar = conexaoBancoClass.getMethod("conectar");
            Object conexao = conectar.invoke(null);
            if (conexao instanceof Connection) {
                return Optional.of((Connection) conexao);
            }
        } catch (ClassNotFoundException ignored) {
            // Rodando fora do ERP.
        } catch (Exception e) {
            throw new RuntimeException("Erro ao abrir conexao usando configuracao do ERP", e);
        }
        return Optional.empty();
    }
}
