package erp.application;

import UTIL.ConexaoBanco;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImportadorPedidoFornecedorCsv {

    private static final List<String> TIPOS_VALIDOS = List.of("Masculina", "Feminina", "Infantil");

    public static void main(String[] args) {
        try {
            Argumentos argumentos = parseArgumentos(args);
            List<ItemCsv> itens = carregarItens(argumentos.caminhoCsv());
            validarItens(itens);
            Resumo resumo = resumir(itens);

            System.out.println("Itens CSV: " + itens.size());
            System.out.println("Pecas CSV: " + resumo.quantidadeTotal());
            System.out.println("Custo total: " + resumo.custoTotal());

            ResultadoImportacao resultado = importar(argumentos, itens, resumo);
            System.out.println((argumentos.dryRun() ? "Dry-run OK. " : "Importacao OK. ")
                    + "PedidoFornecedorID=" + resultado.pedidoId()
                    + ", produtos criados=" + resultado.produtosCriados()
                    + ", produtos atualizados=" + resultado.produtosAtualizados()
                    + ", itens inseridos=" + resultado.itensInseridos());
        } catch (Exception e) {
            System.err.println("Falha na importacao: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static Argumentos parseArgumentos(String[] args) {
        if (args.length < 3 || args.length > 4) {
            throw new IllegalArgumentException("Uso: java erp.application.ImportadorPedidoFornecedorCsv <csv> <data-pedido YYYY-MM-DD> <fornecedor> [--dry-run]");
        }

        boolean dryRun = false;
        if (args.length == 4) {
            if (!"--dry-run".equalsIgnoreCase(args[3])) {
                throw new IllegalArgumentException("Parametro invalido: " + args[3]);
            }
            dryRun = true;
        }

        Path caminhoCsv = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.exists(caminhoCsv)) {
            throw new IllegalArgumentException("CSV nao encontrado: " + caminhoCsv);
        }

        LocalDate dataPedido = LocalDate.parse(args[1]);
        String fornecedor = args[2].trim();
        if (fornecedor.isEmpty()) {
            throw new IllegalArgumentException("Fornecedor obrigatorio.");
        }

        return new Argumentos(caminhoCsv, dataPedido, fornecedor, dryRun);
    }

    private static List<ItemCsv> carregarItens(Path caminhoCsv) throws IOException {
        List<String> linhas = Files.readAllLines(caminhoCsv, StandardCharsets.UTF_8);
        if (linhas.isEmpty()) {
            throw new IllegalArgumentException("CSV vazio: " + caminhoCsv);
        }

        List<String> cabecalho = parseCsvLine(linhas.get(0));
        Map<String, Integer> indices = new LinkedHashMap<>();
        for (int i = 0; i < cabecalho.size(); i++) {
            indices.put(cabecalho.get(i), i);
        }

        List<String> obrigatorias = List.of(
                "Clube", "Modelo", "Tipo", "Tamanho", "QuantidadePedida",
                "CustoUnitarioFornecedor", "CustoUnitarioComTaxas", "PrecoVendaAtual", "DescricaoCompleta"
        );
        for (String coluna : obrigatorias) {
            if (!indices.containsKey(coluna)) {
                throw new IllegalArgumentException("Coluna obrigatoria ausente: " + coluna);
            }
        }

        List<ItemCsv> itens = new ArrayList<>();
        for (int i = 1; i < linhas.size(); i++) {
            List<String> campos = expandirCampos(parseCsvLine(linhas.get(i)), cabecalho.size());
            if (campos.stream().allMatch(String::isBlank)) {
                continue;
            }

            itens.add(new ItemCsv(
                    i + 1,
                    campo(campos, indices, "Clube"),
                    campo(campos, indices, "Modelo"),
                    campo(campos, indices, "Tipo"),
                    campo(campos, indices, "Tamanho"),
                    parseInt(campo(campos, indices, "QuantidadePedida"), i + 1, "QuantidadePedida"),
                    parseDecimal(campo(campos, indices, "CustoUnitarioFornecedor"), i + 1, "CustoUnitarioFornecedor"),
                    parseDecimal(campo(campos, indices, "CustoUnitarioComTaxas"), i + 1, "CustoUnitarioComTaxas"),
                    parseDecimal(campo(campos, indices, "PrecoVendaAtual"), i + 1, "PrecoVendaAtual"),
                    campo(campos, indices, "DescricaoCompleta")
            ));
        }

        return itens;
    }

    private static void validarItens(List<ItemCsv> itens) {
        if (itens.isEmpty()) {
            throw new IllegalArgumentException("Nenhum item para importar.");
        }

        for (ItemCsv item : itens) {
            if (item.clube().isEmpty() || item.modelo().isEmpty() || item.tipo().isEmpty() || item.tamanho().isEmpty()) {
                throw new IllegalArgumentException("Linha " + item.linhaOrigem() + " possui Clube, Modelo, Tipo ou Tamanho vazio.");
            }
            if (!TIPOS_VALIDOS.contains(item.tipo())) {
                throw new IllegalArgumentException("Linha " + item.linhaOrigem() + " possui Tipo invalido: " + item.tipo());
            }
            if (item.quantidadePedida() <= 0) {
                throw new IllegalArgumentException("Linha " + item.linhaOrigem() + " possui quantidade menor ou igual a zero.");
            }
            if (item.custoUnitarioFornecedor().signum() < 0 || item.custoUnitarioComTaxas().signum() < 0 || item.precoVendaAtual().signum() < 0) {
                throw new IllegalArgumentException("Linha " + item.linhaOrigem() + " possui valor monetario negativo.");
            }
        }
    }

    private static Resumo resumir(List<ItemCsv> itens) {
        int quantidadeTotal = 0;
        BigDecimal custoTotal = BigDecimal.ZERO;
        for (ItemCsv item : itens) {
            quantidadeTotal += item.quantidadePedida();
            custoTotal = custoTotal.add(item.custoUnitarioComTaxas().multiply(BigDecimal.valueOf(item.quantidadePedida())));
        }
        return new Resumo(quantidadeTotal, custoTotal);
    }

    private static ResultadoImportacao importar(Argumentos argumentos, List<ItemCsv> itens, Resumo resumo) throws SQLException {
        try (Connection con = ConexaoBanco.conectar()) {
            con.setAutoCommit(false);
            try {
                long pedidoId = inserirPedido(con, argumentos, resumo);
                int produtosCriados = 0;
                int produtosAtualizados = 0;
                int itensInseridos = 0;

                String sqlItem = "INSERT INTO ItensPedidoFornecedor "
                        + "(PedidoFornecedorID, ProdutoID, QuantidadePedida, CustoUnitarioFornecedor, CustoUnitarioComTaxas) "
                        + "VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement pstItem = con.prepareStatement(sqlItem)) {
                    for (ItemCsv item : itens) {
                        ProdutoResultado produto = findOrCreateProduto(con, item);
                        if (produto.criado()) {
                            produtosCriados++;
                        } else {
                            produtosAtualizados++;
                        }

                        pstItem.setLong(1, pedidoId);
                        pstItem.setInt(2, produto.produtoId());
                        pstItem.setInt(3, item.quantidadePedida());
                        pstItem.setBigDecimal(4, item.custoUnitarioFornecedor());
                        pstItem.setBigDecimal(5, item.custoUnitarioComTaxas());
                        pstItem.addBatch();
                        itensInseridos++;
                    }
                    pstItem.executeBatch();
                }

                if (argumentos.dryRun()) {
                    con.rollback();
                } else {
                    con.commit();
                }

                return new ResultadoImportacao(pedidoId, produtosCriados, produtosAtualizados, itensInseridos);
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private static long inserirPedido(Connection con, Argumentos argumentos, Resumo resumo) throws SQLException {
        String sqlPedido = "INSERT INTO PedidosFornecedor "
                + "(DataPedido, NomeFornecedor, CustoTotalEstimadoItens, TaxaImportacaoTotal, CustoTotalFinalPedido, StatusPedido) "
                + "VALUES (?, ?, ?, 0.00, ?, 'Realizado')";

        try (PreparedStatement pst = con.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
            pst.setDate(1, Date.valueOf(argumentos.dataPedido()));
            pst.setString(2, argumentos.fornecedor());
            pst.setBigDecimal(3, resumo.custoTotal());
            pst.setBigDecimal(4, resumo.custoTotal());
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Pedido criado sem ID retornado.");
    }

    private static ProdutoResultado findOrCreateProduto(Connection con, ItemCsv item) throws SQLException {
        String sqlSelect = "SELECT ProdutoID FROM Produtos WHERE Modelo = ? AND Clube = ? AND Tipo = ? AND Tamanho = ?";
        try (PreparedStatement pst = con.prepareStatement(sqlSelect)) {
            pst.setString(1, item.modelo());
            pst.setString(2, item.clube());
            pst.setString(3, item.tipo());
            pst.setString(4, item.tamanho());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int produtoId = rs.getInt("ProdutoID");
                    atualizarProdutoExistente(con, produtoId, item);
                    return new ProdutoResultado(produtoId, false);
                }
            }
        }

        String sqlInsert = "INSERT INTO Produtos "
                + "(Modelo, Clube, Tipo, Tamanho, DescricaoCompleta, PrecoVendaAtual, QuantidadeEstoque, CustoMedioPonderado) "
                + "VALUES (?, ?, ?, ?, ?, ?, 0, ?)";
        try (PreparedStatement pst = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, item.modelo());
            pst.setString(2, item.clube());
            pst.setString(3, item.tipo());
            pst.setString(4, item.tamanho());
            pst.setString(5, item.descricaoCompleta());
            pst.setBigDecimal(6, item.precoVendaAtual());
            pst.setBigDecimal(7, item.custoUnitarioFornecedor());
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    return new ProdutoResultado(rs.getInt(1), true);
                }
            }
        }

        throw new SQLException("Produto criado sem ID retornado: " + item.modelo() + " " + item.clube() + " " + item.tipo() + " " + item.tamanho());
    }

    private static void atualizarProdutoExistente(Connection con, int produtoId, ItemCsv item) throws SQLException {
        String sqlUpdate = "UPDATE Produtos SET PrecoVendaAtual = ?, DescricaoCompleta = ? WHERE ProdutoID = ?";
        try (PreparedStatement pst = con.prepareStatement(sqlUpdate)) {
            pst.setBigDecimal(1, item.precoVendaAtual());
            pst.setString(2, item.descricaoCompleta());
            pst.setInt(3, produtoId);
            pst.executeUpdate();
        }
    }

    private static String campo(List<String> campos, Map<String, Integer> indices, String nome) {
        return campos.get(indices.get(nome)).replace("\uFEFF", "").trim();
    }

    private static int parseInt(String valor, int linha, String coluna) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Linha " + linha + ", coluna " + coluna + " invalida: " + valor);
        }
    }

    private static BigDecimal parseDecimal(String valor, int linha, String coluna) {
        try {
            return new BigDecimal(valor.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Linha " + linha + ", coluna " + coluna + " invalida: " + valor);
        }
    }

    private static List<String> parseCsvLine(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean emAspas = false;

        for (int i = 0; i < linha.length(); i++) {
            char caractere = linha.charAt(i);

            if (caractere == '"') {
                if (emAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    emAspas = !emAspas;
                }
                continue;
            }

            if (caractere == ';' && !emAspas) {
                campos.add(atual.toString());
                atual.setLength(0);
                continue;
            }

            if (caractere != '\r' && caractere != '\n') {
                atual.append(caractere);
            }
        }

        campos.add(atual.toString());
        return campos;
    }

    private static List<String> expandirCampos(List<String> campos, int minimo) {
        ArrayList<String> resultado = new ArrayList<>(campos);
        while (resultado.size() < minimo) {
            resultado.add("");
        }
        return resultado;
    }

    private record Argumentos(Path caminhoCsv, LocalDate dataPedido, String fornecedor, boolean dryRun) { }
    private record ItemCsv(
            int linhaOrigem,
            String clube,
            String modelo,
            String tipo,
            String tamanho,
            int quantidadePedida,
            BigDecimal custoUnitarioFornecedor,
            BigDecimal custoUnitarioComTaxas,
            BigDecimal precoVendaAtual,
            String descricaoCompleta
    ) { }
    private record Resumo(int quantidadeTotal, BigDecimal custoTotal) { }
    private record ProdutoResultado(int produtoId, boolean criado) { }
    private record ResultadoImportacao(long pedidoId, int produtosCriados, int produtosAtualizados, int itensInseridos) { }
}
