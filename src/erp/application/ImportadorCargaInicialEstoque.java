package erp.application;

import UTIL.ConexaoBanco;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ImportadorCargaInicialEstoque {

    private static final Charset FALLBACK_CHARSET = Charset.forName("windows-1252");
    private static final List<String> TAMANHOS_ADULTO = List.of("P", "M", "G", "GG", "2GG", "3GG", "4GG");
    private static final List<String> TAMANHOS_INFANTIL = List.of("16", "18", "20", "22", "24", "26", "28");
    private static final Map<String, String> COLUNA_PARA_TAMANHO = Map.of(
            "P.", "P",
            "M.", "M",
            "G.", "G",
            "GG.", "GG",
            "2GG.", "2GG",
            "3GG.", "3GG",
            "4GG.", "4GG"
    );
    private static final Set<String> TIPOS_VALIDOS = Set.of("Masculina", "Feminina", "Infantil");

    public static void main(String[] args) {
        try {
            Argumentos argumentos = parseArgumentos(args);
            SnapshotImportacao snapshot = carregarSnapshot(argumentos.caminhoCsv(), argumentos.caminhoOverridesInfantis());

            imprimirResumoLeitura(snapshot);

            if (!snapshot.problemas().isEmpty()) {
                imprimirProblemas(snapshot.problemas());
                System.exit(1);
            }

            if (argumentos.dryRun()) {
                System.out.println("Dry-run finalizado sem gravar dados no banco.");
                return;
            }

            ResultadoPersistencia resultado = persistirSnapshot(snapshot);
            imprimirResumoPersistencia(resultado);
        } catch (ImportacaoException e) {
            System.err.println("Falha na importacao: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Falha inesperada na importacao: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static Argumentos parseArgumentos(String[] args) throws ImportacaoException {
        if (args.length == 0) {
            throw new ImportacaoException("Uso: java erp.application.ImportadorCargaInicialEstoque <caminho-do-csv> [--dry-run] [--infantil-overrides <caminho-do-csv>]");
        }

        Path caminhoCsv = null;
        Path caminhoOverridesInfantis = null;
        boolean dryRun = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--dry-run".equalsIgnoreCase(arg)) {
                dryRun = true;
            } else if ("--infantil-overrides".equalsIgnoreCase(arg)) {
                if (i + 1 >= args.length) {
                    throw new ImportacaoException("O parametro --infantil-overrides exige um caminho de arquivo.");
                }
                caminhoOverridesInfantis = Paths.get(args[++i]).toAbsolutePath().normalize();
            } else if (arg.startsWith("--")) {
                throw new ImportacaoException("Parametro nao reconhecido: " + arg);
            } else if (caminhoCsv == null) {
                caminhoCsv = Paths.get(arg).toAbsolutePath().normalize();
            } else {
                throw new ImportacaoException("Informe apenas um caminho de CSV por execucao.");
            }
        }

        if (caminhoCsv == null) {
            throw new ImportacaoException("O caminho do CSV e obrigatorio.");
        }
        if (!Files.exists(caminhoCsv)) {
            throw new ImportacaoException("Arquivo CSV nao encontrado: " + caminhoCsv);
        }
        if (caminhoOverridesInfantis != null && !Files.exists(caminhoOverridesInfantis)) {
            throw new ImportacaoException("Arquivo de overrides infantis nao encontrado: " + caminhoOverridesInfantis);
        }

        return new Argumentos(caminhoCsv, dryRun, caminhoOverridesInfantis);
    }

    private static SnapshotImportacao carregarSnapshot(Path caminhoCsv, Path caminhoOverridesInfantis) throws IOException, ImportacaoException {
        ConteudoCsv conteudo = lerLinhasDetectandoCodificacao(caminhoCsv);
        List<String> linhas = conteudo.linhas();
        OverridesInfantis overridesInfantis = caminhoOverridesInfantis == null
                ? OverridesInfantis.vazio()
                : carregarOverridesInfantis(caminhoOverridesInfantis);

        if (linhas.isEmpty()) {
            throw new ImportacaoException("O arquivo CSV esta vazio: " + caminhoCsv);
        }

        List<String> cabecalho = parseCsvLine(linhas.get(0));
        int indiceClube = localizarColunaObrigatoria(cabecalho, "Clube");
        int indiceModelo = localizarColunaObrigatoria(cabecalho, "Modelo");
        int indiceTipo = localizarColunaObrigatoria(cabecalho, "Tipo");
        int indiceSeparador = localizarColunaObrigatoria(cabecalho, "0");

        validarBlocoAtual(cabecalho, indiceSeparador);

        Map<String, Integer> indicesColunasAtuais = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : COLUNA_PARA_TAMANHO.entrySet()) {
            indicesColunasAtuais.put(entry.getValue(), localizarColunaObrigatoria(cabecalho, entry.getKey()));
        }

        LinkedHashMap<ProdutoKey, ProdutoImportado> produtos = new LinkedHashMap<>();
        List<ProblemaImportacao> problemas = new ArrayList<>();
        int linhasDados = linhas.size() - 1;
        int linhasUteis = 0;
        int linhasSemEstoqueAtual = 0;

        for (int i = 1; i < linhas.size(); i++) {
            int numeroLinha = i + 1;
            List<String> campos = expandirCampos(parseCsvLine(linhas.get(i)), cabecalho.size());

            String clube = normalizarTexto(campos.get(indiceClube));
            String modelo = normalizarTexto(campos.get(indiceModelo));
            String tipoBruto = normalizarTexto(campos.get(indiceTipo));
            String tipo = normalizarTipo(tipoBruto);

            LinkedHashMap<String, Integer> quantidadesAtuais = new LinkedHashMap<>();
            int totalAtualLinha = 0;
            boolean linhaComQuantidadeInvalida = false;

            for (Map.Entry<String, Integer> entry : indicesColunasAtuais.entrySet()) {
                String tamanho = entry.getKey();
                int indice = entry.getValue();
                String valorBruto = indice < campos.size() ? campos.get(indice) : "";

                try {
                    int quantidade = parseQuantidade(valorBruto);
                    quantidadesAtuais.put(tamanho, quantidade);
                    totalAtualLinha += quantidade;
                } catch (ImportacaoException e) {
                    problemas.add(new ProblemaImportacao(numeroLinha,
                            "Valor invalido na coluna de estoque atual para tamanho " + tamanho + ": '" + valorBruto + "'."));
                    linhaComQuantidadeInvalida = true;
                }
            }

            boolean linhaIgnoravel = clube.isEmpty() && modelo.isEmpty() && tipoBruto.isEmpty() && totalAtualLinha == 0;
            if (linhaIgnoravel) {
                continue;
            }

            linhasUteis++;

            if (linhaComQuantidadeInvalida) {
                continue;
            }

            if (clube.isEmpty() || modelo.isEmpty() || tipoBruto.isEmpty()) {
                problemas.add(new ProblemaImportacao(numeroLinha,
                        "Linha sem Clube, Modelo ou Tipo preenchido no bloco considerado."));
                continue;
            }

            if (tipo == null || !TIPOS_VALIDOS.contains(tipo)) {
                problemas.add(new ProblemaImportacao(numeroLinha,
                        "Tipo fora do dominio atual do ERP: '" + tipoBruto + "'."));
                continue;
            }

            ProdutoKey chave = new ProdutoKey(clube, modelo, tipo);
            Map<String, Integer> quantidadesConsideradas = new LinkedHashMap<>();

            if ("Infantil".equals(tipo) && totalAtualLinha > 0) {
                Map<String, Integer> override = overridesInfantis.quantidadesPara(chave);
                if (override == null || override.isEmpty()) {
                    problemas.add(new ProblemaImportacao(numeroLinha,
                            "Linha infantil com saldo no bloco adulto (P..4GG) sem override correspondente."));
                    continue;
                }

                int totalOverride = override.values().stream()
                        .mapToInt(Integer::intValue)
                        .sum();

                if (totalOverride != totalAtualLinha) {
                    problemas.add(new ProblemaImportacao(numeroLinha,
                            "Override infantil com total divergente para " + clube + " | " + modelo
                                    + ". Planilha=" + totalAtualLinha + ", override=" + totalOverride + "."));
                    continue;
                }

                quantidadesConsideradas.putAll(override);
                overridesInfantis.marcarUtilizado(chave);
            } else if (!"Infantil".equals(tipo)) {
                quantidadesConsideradas.putAll(quantidadesAtuais);
            }

            ProdutoImportado produto = produtos.computeIfAbsent(chave, ProdutoImportado::new);
            produto.adicionarLinhaOrigem(numeroLinha);
            produto.acumularQuantidades(quantidadesConsideradas);

            if (totalAtualLinha == 0) {
                linhasSemEstoqueAtual++;
            }
        }

        for (String overrideNaoUtilizado : overridesInfantis.descreverNaoUtilizados()) {
            problemas.add(new ProblemaImportacao(0, overrideNaoUtilizado));
        }

        int gruposDuplicados = (int) produtos.values().stream()
                .filter(produto -> produto.linhasOrigem().size() > 1)
                .count();

        return new SnapshotImportacao(
                caminhoCsv,
                caminhoOverridesInfantis,
                conteudo.charset(),
                linhasDados,
                linhasUteis,
                linhasSemEstoqueAtual,
                gruposDuplicados,
                produtos,
                problemas
        );
    }

    private static ConteudoCsv lerLinhasDetectandoCodificacao(Path caminhoCsv) throws IOException {
        byte[] bytes = Files.readAllBytes(caminhoCsv);
        String textoUtf8 = new String(bytes, StandardCharsets.UTF_8);

        if (textoUtf8.contains("\uFFFD")) {
            return new ConteudoCsv(Files.readAllLines(caminhoCsv, FALLBACK_CHARSET), FALLBACK_CHARSET);
        }

        return new ConteudoCsv(Files.readAllLines(caminhoCsv, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private static OverridesInfantis carregarOverridesInfantis(Path caminhoOverridesInfantis) throws IOException, ImportacaoException {
        ConteudoCsv conteudo = lerLinhasDetectandoCodificacao(caminhoOverridesInfantis);
        List<String> linhas = conteudo.linhas();

        if (linhas.isEmpty()) {
            throw new ImportacaoException("O arquivo de overrides infantis esta vazio: " + caminhoOverridesInfantis);
        }

        List<String> cabecalho = parseCsvLine(linhas.get(0));
        int indiceClube = localizarColunaObrigatoria(cabecalho, "Clube");
        int indiceModelo = localizarColunaObrigatoria(cabecalho, "Modelo");
        int indiceTipo = localizarColunaObrigatoria(cabecalho, "Tipo");
        int indiceTamanho = localizarColunaObrigatoria(cabecalho, "Tamanho");
        int indiceQuantidade = localizarColunaObrigatoria(cabecalho, "Quantidade");

        LinkedHashMap<ProdutoKey, LinkedHashMap<String, Integer>> quantidadesPorProduto = new LinkedHashMap<>();
        LinkedHashMap<ProdutoKey, LinkedHashSet<Integer>> linhasPorProduto = new LinkedHashMap<>();

        for (int i = 1; i < linhas.size(); i++) {
            int numeroLinha = i + 1;
            List<String> campos = expandirCampos(parseCsvLine(linhas.get(i)), cabecalho.size());

            String clube = normalizarTexto(campos.get(indiceClube));
            String modelo = normalizarTexto(campos.get(indiceModelo));
            String tipoBruto = normalizarTexto(campos.get(indiceTipo));
            String tamanho = normalizarTexto(campos.get(indiceTamanho));
            String quantidadeBruta = normalizarTexto(campos.get(indiceQuantidade));

            boolean linhaIgnoravel = clube.isEmpty()
                    && modelo.isEmpty()
                    && tipoBruto.isEmpty()
                    && tamanho.isEmpty()
                    && quantidadeBruta.isEmpty();

            if (linhaIgnoravel) {
                continue;
            }

            if (clube.isEmpty() || modelo.isEmpty() || tipoBruto.isEmpty() || tamanho.isEmpty() || quantidadeBruta.isEmpty()) {
                throw new ImportacaoException("Linha " + numeroLinha + " do arquivo de overrides infantis esta incompleta.");
            }

            if (!"Infantil".equalsIgnoreCase(tipoBruto)) {
                throw new ImportacaoException("Linha " + numeroLinha + " do arquivo de overrides infantis possui tipo invalido: " + tipoBruto);
            }

            if (!TAMANHOS_INFANTIL.contains(tamanho)) {
                throw new ImportacaoException("Linha " + numeroLinha + " do arquivo de overrides infantis possui tamanho fora da grade infantil: " + tamanho);
            }

            int quantidade = parseQuantidade(quantidadeBruta);
            if (quantidade <= 0) {
                throw new ImportacaoException("Linha " + numeroLinha + " do arquivo de overrides infantis precisa ter quantidade positiva.");
            }

            ProdutoKey chave = new ProdutoKey(clube, modelo, "Infantil");
            LinkedHashMap<String, Integer> quantidades = quantidadesPorProduto.computeIfAbsent(chave, _ -> new LinkedHashMap<>());
            quantidades.merge(tamanho, quantidade, Integer::sum);
            linhasPorProduto.computeIfAbsent(chave, _ -> new LinkedHashSet<>()).add(numeroLinha);
        }

        return new OverridesInfantis(caminhoOverridesInfantis, quantidadesPorProduto, linhasPorProduto);
    }

    private static int localizarColunaObrigatoria(List<String> cabecalho, String nomeColuna) throws ImportacaoException {
        int indice = cabecalho.indexOf(nomeColuna);
        if (indice < 0) {
            throw new ImportacaoException("Coluna obrigatoria nao encontrada no CSV: " + nomeColuna);
        }
        return indice;
    }

    private static void validarBlocoAtual(List<String> cabecalho, int indiceSeparador) throws ImportacaoException {
        List<String> esperado = List.of("P.", "M.", "G.", "GG.", "2GG.", "3GG.", "4GG.");

        if (indiceSeparador + esperado.size() >= cabecalho.size()) {
            throw new ImportacaoException("O bloco de estoque atual apos a coluna '0' nao possui todas as colunas esperadas.");
        }

        for (int i = 0; i < esperado.size(); i++) {
            String colunaEsperada = esperado.get(i);
            String colunaEncontrada = cabecalho.get(indiceSeparador + 1 + i);
            if (!colunaEsperada.equals(colunaEncontrada)) {
                throw new ImportacaoException("Estrutura inesperada apos a coluna '0'. Esperado '" + colunaEsperada
                        + "' mas encontrado '" + colunaEncontrada + "'.");
            }
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

    private static String normalizarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replace("\uFEFF", "")
                .replace('\u00A0', ' ')
                .trim();
    }

    private static String normalizarTipo(String tipoBruto) {
        if ("Masculina".equalsIgnoreCase(tipoBruto)) {
            return "Masculina";
        }
        if ("Feminina".equalsIgnoreCase(tipoBruto)) {
            return "Feminina";
        }
        if ("Infantil".equalsIgnoreCase(tipoBruto)) {
            return "Infantil";
        }
        return null;
    }

    private static int parseQuantidade(String valorBruto) throws ImportacaoException {
        String valor = normalizarTexto(valorBruto);
        if (valor.isEmpty()) {
            return 0;
        }
        if (!valor.matches("\\d+")) {
            throw new ImportacaoException("Quantidade nao numerica: " + valor);
        }
        return Integer.parseInt(valor);
    }

    private static List<String> tamanhosSuportados(String tipo) {
        if ("Infantil".equals(tipo)) {
            return TAMANHOS_INFANTIL;
        }
        return TAMANHOS_ADULTO;
    }

    private static ResultadoPersistencia persistirSnapshot(SnapshotImportacao snapshot) throws SQLException, ImportacaoException {
        try (Connection con = ConexaoBanco.conectar()) {
            con.setAutoCommit(false);

            try {
                int registrosExistentes = contarRegistrosProdutos(con);
                if (registrosExistentes > 0) {
                    throw new ImportacaoException("A carga inicial exige tabela Produtos vazia. Registros atuais encontrados: " + registrosExistentes);
                }

                int registrosInseridos = inserirProdutos(con, snapshot);
                ValidacaoBanco validacao = validarBanco(con, snapshot);

                if (!validacao.sucesso()) {
                    throw new ImportacaoException("Validacao apos insert falhou: " + validacao.mensagem());
                }

                con.commit();
                return new ResultadoPersistencia(registrosInseridos, validacao.totalRegistrosBanco(), validacao.totalPecasBanco());
            } catch (Exception e) {
                con.rollback();
                if (e instanceof ImportacaoException importacaoException) {
                    throw importacaoException;
                }
                if (e instanceof SQLException sqlException) {
                    throw sqlException;
                }
                throw new ImportacaoException(e.getMessage(), e);
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private static int contarRegistrosProdutos(Connection con) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM Produtos";
        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            rs.next();
            return rs.getInt("total");
        }
    }

    private static int inserirProdutos(Connection con, SnapshotImportacao snapshot) throws SQLException {
        String sql = "INSERT INTO Produtos "
                + "(Modelo, Clube, Tipo, Tamanho, DescricaoCompleta, PrecoVendaAtual, QuantidadeEstoque, CustoMedioPonderado, DataUltimaEntradaEstoque) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Timestamp dataCarga = Timestamp.valueOf(LocalDateTime.now());
        int registrosInseridos = 0;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            for (ProdutoImportado produto : snapshot.produtosComEstoque()) {
                for (String tamanho : produto.tamanhosComSaldoOrdenados()) {
                    int quantidade = produto.quantidade(tamanho);
                    pst.setString(1, produto.chave().modelo());
                    pst.setString(2, produto.chave().clube());
                    pst.setString(3, produto.chave().tipo());
                    pst.setString(4, tamanho);
                    pst.setString(5, descricaoCompleta(produto.chave(), tamanho));
                    pst.setDouble(6, 0.0d);
                    pst.setInt(7, quantidade);
                    pst.setDouble(8, 0.0d);
                    pst.setTimestamp(9, dataCarga);
                    pst.addBatch();

                    registrosInseridos++;
                }
            }

            int[] resultados = pst.executeBatch();
            for (int resultado : resultados) {
                if (resultado == Statement.EXECUTE_FAILED) {
                    throw new SQLException("Falha em um dos inserts do batch.");
                }
            }
        }

        return registrosInseridos;
    }

    private static String descricaoCompleta(ProdutoKey chave, String tamanho) {
        return String.format(Locale.ROOT, "%s %s %s %s",
                chave.modelo(), chave.clube(), chave.tipo(), tamanho);
    }

    private static ValidacaoBanco validarBanco(Connection con, SnapshotImportacao snapshot) throws SQLException {
        LinkedHashMap<String, Integer> esperado = new LinkedHashMap<>();
        for (ProdutoImportado produto : snapshot.produtosComEstoque()) {
            for (String tamanho : produto.tamanhosComSaldoOrdenados()) {
                int quantidade = produto.quantidade(tamanho);
                esperado.put(chaveRegistro(produto.chave(), tamanho), quantidade);
            }
        }

        LinkedHashMap<String, Integer> encontrado = new LinkedHashMap<>();
        int totalPecasBanco = 0;

        String sql = "SELECT Clube, Modelo, Tipo, Tamanho, QuantidadeEstoque "
                + "FROM Produtos ORDER BY Clube, Modelo, Tipo, Tamanho";

        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                String clube = rs.getString("Clube");
                String modelo = rs.getString("Modelo");
                String tipo = rs.getString("Tipo");
                String tamanho = rs.getString("Tamanho");
                int quantidade = rs.getInt("QuantidadeEstoque");

                encontrado.put(chaveRegistro(new ProdutoKey(clube, modelo, tipo), tamanho), quantidade);
                totalPecasBanco += quantidade;
            }
        }

        if (esperado.size() != encontrado.size()) {
            return new ValidacaoBanco(false,
                    "Quantidade de registros divergente. Esperado " + esperado.size() + " e encontrado " + encontrado.size() + ".",
                    encontrado.size(),
                    totalPecasBanco);
        }

        for (Map.Entry<String, Integer> entry : esperado.entrySet()) {
            Integer quantidadeBanco = encontrado.get(entry.getKey());
            if (!Objects.equals(entry.getValue(), quantidadeBanco)) {
                return new ValidacaoBanco(false,
                        "Divergencia no registro " + entry.getKey() + ". Esperado " + entry.getValue()
                                + " e encontrado " + quantidadeBanco + ".",
                        encontrado.size(),
                        totalPecasBanco);
            }
        }

        return new ValidacaoBanco(true, "OK", encontrado.size(), totalPecasBanco);
    }

    private static String chaveRegistro(ProdutoKey chave, String tamanho) {
        return String.join("|", chave.clube(), chave.modelo(), chave.tipo(), tamanho);
    }

    private static void imprimirResumoLeitura(SnapshotImportacao snapshot) {
        System.out.println("Arquivo: " + snapshot.caminhoCsv());
        if (snapshot.caminhoOverridesInfantis() != null) {
            System.out.println("Arquivo de overrides infantis: " + snapshot.caminhoOverridesInfantis());
        }
        System.out.println("Codificacao detectada: " + snapshot.charset().displayName());
        System.out.println("Linhas lidas do CSV (sem cabecalho): " + snapshot.linhasDados());
        System.out.println("Linhas uteis consideradas: " + snapshot.linhasUteis());
        System.out.println("Produtos agregados: " + snapshot.produtos().size());
        System.out.println("Produtos com estoque atual positivo: " + snapshot.produtosComEstoque().size());
        System.out.println("Linhas sem estoque atual: " + snapshot.linhasSemEstoqueAtual());
        System.out.println("Grupos duplicados agregados: " + snapshot.gruposDuplicados());
        System.out.println("Registros de estoque a gerar: " + snapshot.totalRegistrosEstoque());
        System.out.println("Total de pecas consideradas: " + snapshot.totalPecas());
        System.out.println();

        System.out.println("Quantidades finais por produto:");
        for (ProdutoImportado produto : snapshot.produtos().values()) {
            System.out.println(" - [linhas " + produto.linhasOrigemTexto() + "] "
                    + produto.chave().clube() + " | "
                    + produto.chave().modelo() + " | "
                    + produto.chave().tipo() + " => "
                    + produto.resumoQuantidades());
        }

        System.out.println();
        System.out.println("Linhas problematicas encontradas: " + snapshot.problemas().size());
    }

    private static void imprimirProblemas(List<ProblemaImportacao> problemas) {
        if (problemas.isEmpty()) {
            return;
        }

        System.out.println("Problemas de importacao:");
        for (ProblemaImportacao problema : problemas) {
            System.out.println(" - linha " + problema.numeroLinha() + ": " + problema.descricao());
        }
    }

    private static void imprimirResumoPersistencia(ResultadoPersistencia resultado) {
        System.out.println();
        System.out.println("Carga inicial concluida com sucesso.");
        System.out.println("Registros inseridos no batch: " + resultado.registrosInseridos());
        System.out.println("Registros confirmados no banco: " + resultado.registrosConfirmadosBanco());
        System.out.println("Total de pecas confirmado no banco: " + resultado.totalPecasBanco());
    }

    private record Argumentos(Path caminhoCsv, boolean dryRun, Path caminhoOverridesInfantis) { }

    private record ConteudoCsv(List<String> linhas, Charset charset) { }

    private record ProblemaImportacao(int numeroLinha, String descricao) { }

    private record SnapshotImportacao(
            Path caminhoCsv,
            Path caminhoOverridesInfantis,
            Charset charset,
            int linhasDados,
            int linhasUteis,
            int linhasSemEstoqueAtual,
            int gruposDuplicados,
            LinkedHashMap<ProdutoKey, ProdutoImportado> produtos,
            List<ProblemaImportacao> problemas
    ) {
        List<ProdutoImportado> produtosComEstoque() {
            return produtos.values().stream()
                    .filter(produto -> produto.totalPecas() > 0)
                    .collect(Collectors.toList());
        }

        int totalRegistrosEstoque() {
            return produtosComEstoque().stream()
                    .mapToInt(ProdutoImportado::totalRegistrosEstoque)
                    .sum();
        }

        int totalPecas() {
            return produtosComEstoque().stream()
                    .mapToInt(ProdutoImportado::totalPecas)
                    .sum();
        }
    }

    private static final class ProdutoImportado {
        private final ProdutoKey chave;
        private final LinkedHashMap<String, Integer> quantidades = new LinkedHashMap<>();
        private final LinkedHashSet<Integer> linhasOrigem = new LinkedHashSet<>();

        ProdutoImportado(ProdutoKey chave) {
            this.chave = chave;
        }

        void adicionarLinhaOrigem(int numeroLinha) {
            linhasOrigem.add(numeroLinha);
        }

        void acumularQuantidades(Map<String, Integer> quantidadesLinha) {
            for (Map.Entry<String, Integer> entry : quantidadesLinha.entrySet()) {
                if (entry.getValue() > 0) {
                    quantidades.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }

        ProdutoKey chave() {
            return chave;
        }

        int quantidade(String tamanho) {
            return quantidades.getOrDefault(tamanho, 0);
        }

        int totalPecas() {
            return quantidades.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        int totalRegistrosEstoque() {
            return quantidades.size();
        }

        Set<Integer> linhasOrigem() {
            return linhasOrigem;
        }

        String linhasOrigemTexto() {
            return linhasOrigem.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }

        String resumoQuantidades() {
            List<String> partes = new ArrayList<>();
            for (String tamanho : tamanhosComSaldoOrdenados()) {
                int quantidade = quantidade(tamanho);
                partes.add(tamanho + "=" + quantidade);
            }

            if (partes.isEmpty()) {
                return "sem estoque atual";
            }

            return String.join(", ", partes) + " | total=" + totalPecas();
        }

        List<String> tamanhosComSaldoOrdenados() {
            return tamanhosSuportados(chave.tipo()).stream()
                    .filter(tamanho -> quantidades.getOrDefault(tamanho, 0) > 0)
                    .collect(Collectors.toList());
        }
    }

    private record ProdutoKey(String clube, String modelo, String tipo) { }

    private static final class OverridesInfantis {
        private final Path caminhoArquivo;
        private final LinkedHashMap<ProdutoKey, LinkedHashMap<String, Integer>> quantidadesPorProduto;
        private final LinkedHashMap<ProdutoKey, LinkedHashSet<Integer>> linhasPorProduto;
        private final LinkedHashSet<ProdutoKey> chavesUtilizadas = new LinkedHashSet<>();

        private OverridesInfantis(
                Path caminhoArquivo,
                LinkedHashMap<ProdutoKey, LinkedHashMap<String, Integer>> quantidadesPorProduto,
                LinkedHashMap<ProdutoKey, LinkedHashSet<Integer>> linhasPorProduto
        ) {
            this.caminhoArquivo = caminhoArquivo;
            this.quantidadesPorProduto = quantidadesPorProduto;
            this.linhasPorProduto = linhasPorProduto;
        }

        static OverridesInfantis vazio() {
            return new OverridesInfantis(null, new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        Map<String, Integer> quantidadesPara(ProdutoKey chave) {
            return quantidadesPorProduto.get(chave);
        }

        void marcarUtilizado(ProdutoKey chave) {
            chavesUtilizadas.add(chave);
        }

        List<String> descreverNaoUtilizados() {
            List<String> mensagens = new ArrayList<>();
            for (Map.Entry<ProdutoKey, LinkedHashSet<Integer>> entry : linhasPorProduto.entrySet()) {
                if (!chavesUtilizadas.contains(entry.getKey())) {
                    ProdutoKey chave = entry.getKey();
                    String linhas = entry.getValue().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    mensagens.add("Override infantil sem linha ambigua correspondente no CSV: "
                            + chave.clube() + " | " + chave.modelo() + " | " + chave.tipo()
                            + " (linhas " + linhas + " em " + caminhoArquivo + ").");
                }
            }
            return mensagens;
        }
    }

    private record ValidacaoBanco(boolean sucesso, String mensagem, int totalRegistrosBanco, int totalPecasBanco) { }

    private record ResultadoPersistencia(int registrosInseridos, int registrosConfirmadosBanco, int totalPecasBanco) { }

    private static final class ImportacaoException extends Exception {
        ImportacaoException(String message) {
            super(message);
        }

        ImportacaoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
