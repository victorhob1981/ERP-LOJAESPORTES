package erp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import erp.model.ProdutoEstoque;
import erp.model.ProdutoEstoqueInfantil; 
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

public class EstoqueController {
    private MainLayoutController mainLayoutController;

    // --- Componentes da Tabela Adulto ---
    @FXML private ComboBox<String> cbClube;
    @FXML private TableView<ProdutoEstoque> tblEstoque;
    @FXML private TableColumn<ProdutoEstoque, String> colModelo;
    @FXML private TableColumn<ProdutoEstoque, String> colClubeNome; 
    @FXML private TableColumn<ProdutoEstoque, String> colTipo;
    @FXML private TableColumn<ProdutoEstoque, Integer> colQuantidade;
    @FXML private TableColumn<ProdutoEstoque, Integer> colP;
    @FXML private TableColumn<ProdutoEstoque, Integer> colM;
    @FXML private TableColumn<ProdutoEstoque, Integer> colG;
    @FXML private TableColumn<ProdutoEstoque, Integer> colGG;
    @FXML private TableColumn<ProdutoEstoque, Integer> col2GG;
    @FXML private TableColumn<ProdutoEstoque, Integer> col3GG;
    @FXML private TableColumn<ProdutoEstoque, Integer> col4GG;
    @FXML private Button btnIrParaCadastro;
    @FXML private Label lblTotalCamisas;

    // --- Componentes da Tabela Infantil (NOVOS) ---
    @FXML private TableView<ProdutoEstoqueInfantil> tblEstoqueInfantil;
    @FXML private TableColumn<ProdutoEstoqueInfantil, String> colModeloInfantil;
    @FXML private TableColumn<ProdutoEstoqueInfantil, String> colClubeNomeInfantil;
    @FXML private TableColumn<ProdutoEstoqueInfantil, Integer> colQuantidadeInfantil;
    @FXML private TableColumn<ProdutoEstoqueInfantil, Integer> col16;
    @FXML private TableColumn<ProdutoEstoqueInfantil, Integer> col18;
    @FXML private TableColumn<ProdutoEstoqueInfantil, Integer> col20;
    @FXML private TableColumn<ProdutoEstoqueInfantil, Integer> col22;
    @FXML private TableColumn<ProdutoEstoqueInfantil, Integer> col24;
    @FXML private TableColumn<ProdutoEstoqueInfantil, Integer> col26;
    @FXML private TableColumn<ProdutoEstoqueInfantil, Integer> col28;


private ObservableList<ProdutoEstoque> listaCompletaProdutosEstoque = FXCollections.observableArrayList();
private ObservableList<ProdutoEstoqueInfantil> listaCompletaProdutosInfantis = FXCollections.observableArrayList();
private FilteredList<ProdutoEstoque> listaFiltradaAdulto;
private FilteredList<ProdutoEstoqueInfantil> listaFiltradaInfantil;


    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    // Substitua o método initialize() inteiro (linhas 60-70) pelo código abaixo
@FXML
public void initialize() {
    // 1. Configurar as listas filtradas
    listaFiltradaAdulto = new FilteredList<>(listaCompletaProdutosEstoque, p -> true);
    listaFiltradaInfantil = new FilteredList<>(listaCompletaProdutosInfantis, p -> true);

    // 2. Envolver com SortedList para manter a ordenação da tabela
    SortedList<ProdutoEstoque> sortedAdulto = new SortedList<>(listaFiltradaAdulto);
    sortedAdulto.comparatorProperty().bind(tblEstoque.comparatorProperty());

    SortedList<ProdutoEstoqueInfantil> sortedInfantil = new SortedList<>(listaFiltradaInfantil);
    sortedInfantil.comparatorProperty().bind(tblEstoqueInfantil.comparatorProperty());

    // 3. Vincular as listas ordenadas às tabelas
    tblEstoque.setItems(sortedAdulto);
    tblEstoqueInfantil.setItems(sortedInfantil);

    // 4. Configurar as colunas e a altura dinâmica
    configurarTabelaAdulto();
    configurarTabelaInfantil();
    bindTableHeightToRowCount(tblEstoque, sortedAdulto, 100);
    bindTableHeightToRowCount(tblEstoqueInfantil, sortedInfantil, 100);

    // 5. Carregar dados e configurar ações
    carregarDadosIniciais();
    cbClube.setOnAction(_ -> filtrarEstoquePorClube());
}
    
    // --- Métodos de formatação (sem alteração) ---
    private <T> TableCell<T, Integer> formatarCelulaQuantidade() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private void configurarTabelaAdulto() {
        colModelo.setCellValueFactory(cellData -> cellData.getValue().modeloProperty());
        colClubeNome.setCellValueFactory(cellData -> cellData.getValue().clubeProperty());
        colTipo.setCellValueFactory(cellData -> cellData.getValue().tipoProperty()); 
        colQuantidade.setCellValueFactory(cellData -> cellData.getValue().quantidadeTotalProperty().asObject());
        colP.setCellValueFactory(cellData -> cellData.getValue().quantidadePProperty().asObject());
        colM.setCellValueFactory(cellData -> cellData.getValue().quantidadeMProperty().asObject());
        colG.setCellValueFactory(cellData -> cellData.getValue().quantidadeGProperty().asObject());
        colGG.setCellValueFactory(cellData -> cellData.getValue().quantidadeGGProperty().asObject());
        col2GG.setCellValueFactory(cellData -> cellData.getValue().quantidade2GGProperty().asObject());
        col3GG.setCellValueFactory(cellData -> cellData.getValue().quantidade3GGProperty().asObject());
        col4GG.setCellValueFactory(cellData -> cellData.getValue().quantidade4GGProperty().asObject());

        colQuantidade.setCellFactory(_ -> formatarCelulaQuantidade());
        colP.setCellFactory(_ -> formatarCelulaQuantidade());
        colM.setCellFactory(_ -> formatarCelulaQuantidade());
        colG.setCellFactory(_ -> formatarCelulaQuantidade());
        colGG.setCellFactory(_ -> formatarCelulaQuantidade());
        col2GG.setCellFactory(_ -> formatarCelulaQuantidade());
        col3GG.setCellFactory(_ -> formatarCelulaQuantidade());
        col4GG.setCellFactory(_ -> formatarCelulaQuantidade());
    }

    // --- NOVO MÉTODO PARA CONFIGURAR A TABELA INFANTIL ---
    private void configurarTabelaInfantil() {
        colModeloInfantil.setCellValueFactory(cellData -> cellData.getValue().modeloProperty());
        colClubeNomeInfantil.setCellValueFactory(cellData -> cellData.getValue().clubeProperty());
        colQuantidadeInfantil.setCellValueFactory(cellData -> cellData.getValue().quantidadeTotalProperty().asObject());
        col16.setCellValueFactory(cellData -> cellData.getValue().quantidade16Property().asObject());
        col18.setCellValueFactory(cellData -> cellData.getValue().quantidade18Property().asObject());
        col20.setCellValueFactory(cellData -> cellData.getValue().quantidade20Property().asObject());
        col22.setCellValueFactory(cellData -> cellData.getValue().quantidade22Property().asObject());
        col24.setCellValueFactory(cellData -> cellData.getValue().quantidade24Property().asObject());
        col26.setCellValueFactory(cellData -> cellData.getValue().quantidade26Property().asObject());
        col28.setCellValueFactory(cellData -> cellData.getValue().quantidade28Property().asObject());

        colQuantidadeInfantil.setCellFactory(_ -> formatarCelulaQuantidade());
        col16.setCellFactory(_ -> formatarCelulaQuantidade());
        col18.setCellFactory(_ -> formatarCelulaQuantidade());
        col20.setCellFactory(_ -> formatarCelulaQuantidade());
        col22.setCellFactory(_ -> formatarCelulaQuantidade());
        col24.setCellFactory(_ -> formatarCelulaQuantidade());
        col26.setCellFactory(_ -> formatarCelulaQuantidade());
        col28.setCellFactory(_ -> formatarCelulaQuantidade());
    }


    private void carregarDadosIniciais() {
        carregarProdutosDoBanco();
        carregarClubesParaFiltro(); 
        if (cbClube.getItems().isEmpty() || !"Todos".equals(cbClube.getItems().get(0))) {
            cbClube.getItems().add(0, "Todos");
        }
        cbClube.getSelectionModel().select("Todos");
    }

    private void carregarClubesParaFiltro() {
        ObservableList<String> clubes = FXCollections.observableArrayList();
        clubes.add("Todos");
        TreeSet<String> nomesClubesUnicos = new TreeSet<>();
        listaCompletaProdutosEstoque.forEach(p -> nomesClubesUnicos.add(p.getClube()));
        listaCompletaProdutosInfantis.forEach(p -> nomesClubesUnicos.add(p.getClube()));
        clubes.addAll(nomesClubesUnicos);
        cbClube.setItems(clubes);
    }
    
    // --- MÉTODO PRINCIPAL MODIFICADO PARA SEPARAR OS PRODUTOS ---
    private void carregarProdutosDoBanco() {
        listaCompletaProdutosEstoque.clear();
        listaCompletaProdutosInfantis.clear();
        Map<String, ProdutoEstoque> mapaAdulto = new HashMap<>();
        Map<String, ProdutoEstoqueInfantil> mapaInfantil = new HashMap<>();
        
        int totalGeralDeItens = 0;

        String sql = "SELECT Modelo, Clube, Tipo, Tamanho, QuantidadeEstoque " +
                     "FROM Produtos WHERE QuantidadeEstoque > 0 " +
                     "ORDER BY Clube, Modelo, Tipo, Tamanho";

        try (Connection con = UTIL.ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                String modelo = rs.getString("Modelo");
                String clube = rs.getString("Clube");
                String tipo = rs.getString("Tipo"); 
                String tamanho = rs.getString("Tamanho");
                int quantidade = rs.getInt("QuantidadeEstoque"); 
                
                totalGeralDeItens += quantidade;
                String chaveProduto = clube + "|" + modelo + "|" + tipo;

                if ("Infantil".equalsIgnoreCase(tipo)) {
                    ProdutoEstoqueInfantil produto = mapaInfantil.computeIfAbsent(chaveProduto, k -> new ProdutoEstoqueInfantil(modelo, clube, tipo));
                    produto.setQuantidadeParaTamanho(tamanho, quantidade);
                } else {
                    ProdutoEstoque produto = mapaAdulto.computeIfAbsent(chaveProduto, k -> new ProdutoEstoque(modelo, clube, tipo));
                    produto.setQuantidadeParaTamanho(tamanho, quantidade);
                }
            }

            listaCompletaProdutosEstoque.addAll(mapaAdulto.values());
            listaCompletaProdutosInfantis.addAll(mapaInfantil.values());
            
            lblTotalCamisas.setText("Total: " + totalGeralDeItens);

        } catch (SQLException e) {
            System.err.println("Erro ao carregar produtos do banco: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Erro de Banco", "Não foi possível carregar os dados do estoque.");
        }
    }
// Substitua o método filtrarEstoquePorClube() inteiro pelo código abaixo
private void filtrarEstoquePorClube() {
    String clubeSelecionado = cbClube.getValue();

    // Atualiza o predicado do filtro para a lista de adultos
    listaFiltradaAdulto.setPredicate(produto -> {
        if (clubeSelecionado == null || clubeSelecionado.equals("Todos")) {
            return true;
        }
        return produto.getClube().equals(clubeSelecionado);
    });

    // Atualiza o predicado do filtro para a lista infantil
    listaFiltradaInfantil.setPredicate(produto -> {
        if (clubeSelecionado == null || clubeSelecionado.equals("Todos")) {
            return true;
        }
        return produto.getClube().equals(clubeSelecionado);
    });
}
    
    @FXML
    public void irParaCadastroEstoque(ActionEvent event) {
        if (mainLayoutController != null) {
            mainLayoutController.irParaRegistrarPedido(event);
        }
    }

    private void bindTableHeightToRowCount(TableView<?> tableView, ObservableList<?> items, double minHeight) {
    final double ROW_HEIGHT = 24.5;
    final double HEADER_HEIGHT = 28.0;

    tableView.setMinHeight(minHeight);

    DoubleBinding tableHeight = Bindings.createDoubleBinding(() -> {
        int numRows = items.size();
        if (numRows == 0) {
            // Adiciona um placeholder quando a tabela está vazia
            tableView.setPlaceholder(new Label("Não há conteúdo na tabela"));
            return minHeight;
        }
        return HEADER_HEIGHT + (numRows * ROW_HEIGHT) + 4; // +4 para padding/borda
    }, items);

    tableView.prefHeightProperty().bind(tableHeight);
}

    private void mostrarAlerta(String titulo, String mensagem) {
        Alert.AlertType tipoAlerta = titulo.toLowerCase().contains("erro") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION;
        Alert alert = new Alert(tipoAlerta);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}