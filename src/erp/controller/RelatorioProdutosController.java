package erp.controller;

import UTIL.ConexaoBanco;
import erp.model.ProdutoEstoque;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class RelatorioProdutosController implements Initializable {

    private static final Comparator<String> tamanhoComparator;
    static {
        Map<String, Integer> ordemTamanhos = new HashMap<>();
        ordemTamanhos.put("P", 1);
        ordemTamanhos.put("M", 2);
        ordemTamanhos.put("G", 3);
        ordemTamanhos.put("GG", 4);
        ordemTamanhos.put("2GG", 5);
        ordemTamanhos.put("3GG", 6);
        ordemTamanhos.put("4GG", 7);
        tamanhoComparator = Comparator.comparing(tamanho -> ordemTamanhos.getOrDefault(tamanho, Integer.MAX_VALUE));
    }

    // --- DECLARAÇÕES FXML ATUALIZADAS ---
    @FXML private VBox cardTotalVendidos, cardTotalEstoque, cardClubeMaisVendido, cardTamanhoMaisVendido, cardProdutosUnicos;
    @FXML private Label lblTituloTotalVendidos, lblTituloTotalEstoque, lblTituloClubeMaisVendido, lblTituloTamanhoMaisVendido, lblTituloProdutosUnicos;
    @FXML private Label lblTotalVendidos, lblTotalEstoque, lblClubeMaisVendido, lblTamanhoMaisVendido, lblProdutosUnicos;
    
    @FXML private DatePicker dpDataInicio;
    @FXML private DatePicker dpDataFim;
    @FXML private Button btnGerarRelatorio;
    @FXML private BarChart<String, Number> graficoVendasClube;
    @FXML private BarChart<String, Number> graficoVendasTamanho;
    @FXML private BarChart<String, Number> graficoEstoqueTamanho; // <-- NOVO GRÁFICO
    @FXML private TableView<ProdutoEstoque> tblEstoqueDetalhado;
    @FXML private TableColumn<ProdutoEstoque, String> colClube;
    @FXML private TableColumn<ProdutoEstoque, String> colModelo;
    @FXML private TableColumn<ProdutoEstoque, String> colTipo;
    @FXML private TableColumn<ProdutoEstoque, Integer> colP, colM, colG, colGG, col2GG, col3GG, col4GG, colTotal;

    private ObservableList<ProdutoEstoque> listaEstoqueDetalhado = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpDataInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpDataFim.setValue(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
        configurarTabelaEstoque();
        btnGerarRelatorio.setOnAction(_ -> atualizarRelatorio());
        
        graficoVendasClube.setAnimated(false);
        graficoVendasClube.setLegendVisible(false);
        graficoVendasTamanho.setAnimated(false);
        graficoVendasTamanho.setLegendVisible(false);
        graficoEstoqueTamanho.setAnimated(false);
        graficoEstoqueTamanho.setLegendVisible(false);
        
        atualizarRelatorio();
    }

    private void atualizarRelatorio() {
        LocalDate dataInicio = dpDataInicio.getValue();
        LocalDate dataFim = dpDataFim.getValue();
        if (dataInicio == null || dataFim == null || dataInicio.isAfter(dataFim)) {
            mostrarAlerta("Datas Inválidas", "Por favor, selecione um período de datas válido.", Alert.AlertType.WARNING);
            return;
        }
        carregarDadosVendas(dataInicio, dataFim);
        carregarDadosEstoque();
        carregarTotalProdutosUnicos();
    }
    
    private void carregarTotalProdutosUnicos() {
        String sql = "SELECT COUNT(*) AS total FROM (" +
                     "  SELECT 1 FROM Produtos " +
                     "  WHERE QuantidadeEstoque > 0 " +
                     "  GROUP BY TRIM(Clube), TRIM(Modelo), TRIM(Tipo)" +
                     ") AS ProdutosUnicos";
        
        try (Connection con = ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            if (rs.next()) {
                lblProdutosUnicos.setText(String.valueOf(rs.getInt("total")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblProdutosUnicos.setText("0");
        }
    }

    private void atualizarCoresDosCards(String clube) {
        aplicarEstiloCard(cardTotalVendidos, "#27ae60");
        aplicarEstiloCard(cardTotalEstoque, "#e67e22");
        aplicarEstiloCard(cardTamanhoMaisVendido, "#8e44ad");
        aplicarEstiloCard(cardProdutosUnicos, "#2980b9");

        String corPrincipalClube;
        switch (clube.toUpperCase()) {
            case "FLAMENGO": corPrincipalClube = "#c0392b"; break;
            case "BOTAFOGO": case "VASCO": corPrincipalClube = "#2c3e50"; break;
            case "FLUMINENSE": corPrincipalClube = "#880E4F"; break;
            default: corPrincipalClube = "#2980b9"; break;
        }
        aplicarEstiloCard(cardClubeMaisVendido, corPrincipalClube);
    }
    
    private void aplicarEstiloCard(VBox card, String corPrincipal) {
        String style = "-fx-padding: 20; -fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);";
        style += "-fx-border-color: " + corPrincipal + "; -fx-border-width: 0 0 0 4;";
        card.setStyle(style);
        
        if (card.getChildren().size() >= 2) {
            Label titulo = (Label) card.getChildren().get(0);
            Label valor = (Label) card.getChildren().get(1);
            
            titulo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
            String tamanhoFonte = (card == cardClubeMaisVendido) ? "24px" : "32px";
            valor.setStyle("-fx-font-size: " + tamanhoFonte + "; -fx-font-weight: bold; -fx-text-fill: " + corPrincipal + ";");
        }
    }

    private void carregarDadosVendas(LocalDate dataInicio, LocalDate dataFim) {
        Map<String, Integer> vendasPorClube = new HashMap<>();
        Map<String, Integer> vendasPorTamanho = new HashMap<>();
        int totalPecasVendidas = 0;

        String sql = "SELECT p.clube, p.tamanho, SUM(iv.Quantidade) as total_vendido " +
                     "FROM ItensVenda iv " +
                     "JOIN Produtos p ON iv.ProdutoID = p.ProdutoID " +
                     "JOIN Vendas v ON iv.VendaID = v.VendaID " +
                     "AND v.DataVenda BETWEEN ? AND ? " +
                     "GROUP BY p.clube, p.tamanho";

        try (Connection con = ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setDate(1, java.sql.Date.valueOf(dataInicio));
            pst.setDate(2, java.sql.Date.valueOf(dataFim));
            ResultSet rs = pst.executeQuery();

            while(rs.next()) {
                String clube = rs.getString("clube");
                String tamanho = rs.getString("tamanho");
                int quantidade = rs.getInt("total_vendido");
                
                totalPecasVendidas += quantidade;
                vendasPorClube.merge(clube, quantidade, Integer::sum);
                vendasPorTamanho.merge(tamanho, quantidade, Integer::sum);
            }

        } catch (SQLException e) { e.printStackTrace(); }

        lblTotalVendidos.setText(String.valueOf(totalPecasVendidas));
        String clubeMaisVendido = vendasPorClube.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("-");
        lblClubeMaisVendido.setText(clubeMaisVendido.toUpperCase());
        String tamanhoMaisVendido = vendasPorTamanho.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("-");
        lblTamanhoMaisVendido.setText(tamanhoMaisVendido);
        
        atualizarCoresDosCards(clubeMaisVendido);

        atualizarGrafico(graficoVendasClube, vendasPorClube, null, "#3498db");
        atualizarGrafico(graficoVendasTamanho, vendasPorTamanho, tamanhoComparator, "#e67e22");
    }
    
    private void carregarDadosEstoque() {
        listaEstoqueDetalhado.clear();
        Map<String, ProdutoEstoque> mapaProdutosAgregados = new HashMap<>();
        Map<String, Integer> estoquePorTamanho = new HashMap<>();
        int totalGeralDeItens = 0;

        String sql = "SELECT Modelo, Clube, Tipo, Tamanho, QuantidadeEstoque " +
                     "FROM Produtos " +
                     "ORDER BY Clube, Modelo, Tipo, Tamanho";

        try (Connection con = ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                int quantidade = rs.getInt("QuantidadeEstoque"); 
                totalGeralDeItens += quantidade;

                String clube = rs.getString("Clube");
                String modelo = rs.getString("Modelo");
                String tipo = rs.getString("Tipo");
                String tamanho = rs.getString("Tamanho");

                String chaveProduto = clube + "|" + modelo + "|" + tipo;

                ProdutoEstoque produto = mapaProdutosAgregados.get(chaveProduto);
                if (produto == null) {
                    produto = new ProdutoEstoque(modelo, clube, tipo);
                    mapaProdutosAgregados.put(chaveProduto, produto);
                }

                produto.setQuantidadeParaTamanho(tamanho, quantidade);

                // acumula estoque por tamanho
                estoquePorTamanho.merge(tamanho, quantidade, Integer::sum);
            }
            
            List<ProdutoEstoque> produtosComEstoque = mapaProdutosAgregados.values()
                .stream()
                .filter(p -> p.getQuantidadeTotal() > 0)
                .collect(Collectors.toList());

            listaEstoqueDetalhado.setAll(produtosComEstoque);
            lblTotalEstoque.setText(String.valueOf(totalGeralDeItens));

            // gráfico novo
            atualizarGrafico(graficoEstoqueTamanho, estoquePorTamanho, tamanhoComparator, "#2ecc71");

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível carregar os dados de estoque.", Alert.AlertType.ERROR);
        }
    }

    private void atualizarGrafico(BarChart<String, Number> chart, Map<String, Integer> dados, Comparator<String> sorter, String corBarras) {
        chart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        
        List<String> keys = new ArrayList<>(dados.keySet());
        if (sorter != null) {
            keys.sort(sorter);
        } else {
            Collections.sort(keys);
        }
        
        for (String key : keys) {
            series.getData().add(new XYChart.Data<>(key, dados.get(key)));
        }
        
        chart.getData().add(series);

        for(Node n: chart.lookupAll(".chart-bar")) {
            n.setStyle("-fx-bar-fill: " + corBarras + ";");
        }
    }

    private void configurarTabelaEstoque() {
        tblEstoqueDetalhado.setItems(listaEstoqueDetalhado);
        colClube.setCellValueFactory(new PropertyValueFactory<>("clube"));
        colModelo.setCellValueFactory(new PropertyValueFactory<>("modelo"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colP.setCellValueFactory(new PropertyValueFactory<>("quantidadeP"));
        colM.setCellValueFactory(new PropertyValueFactory<>("quantidadeM"));
        colG.setCellValueFactory(new PropertyValueFactory<>("quantidadeG"));
        colGG.setCellValueFactory(new PropertyValueFactory<>("quantidadeGG"));
        col2GG.setCellValueFactory(new PropertyValueFactory<>("quantidade2GG"));
        col3GG.setCellValueFactory(new PropertyValueFactory<>("quantidade3GG"));
        col4GG.setCellValueFactory(new PropertyValueFactory<>("quantidade4GG"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("quantidadeTotal"));

        formatarCelulaQuantidade(colP);
        formatarCelulaQuantidade(colM);
        formatarCelulaQuantidade(colG);
        formatarCelulaQuantidade(colGG);
        formatarCelulaQuantidade(col2GG);
        formatarCelulaQuantidade(col3GG);
        formatarCelulaQuantidade(col4GG);
        formatarCelulaQuantidade(colTotal);
    }
    
    private void formatarCelulaQuantidade(TableColumn<ProdutoEstoque, Integer> coluna) {
        coluna.setCellFactory(_ -> new TableCell<>() {
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
        });
    }

    private void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
