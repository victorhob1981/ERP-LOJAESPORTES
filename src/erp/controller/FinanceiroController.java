package erp.controller;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

public class FinanceiroController implements Initializable {

    @FXML private DatePicker dpDataInicio;
    @FXML private DatePicker dpDataFim;
    @FXML private Button btnGerarRelatorio;
    @FXML private Label lblFaturamento;
    @FXML private Label lblCustoProdutos;
    @FXML private Label lblLucroBruto;
    @FXML private Label lblMargemLucro;
    @FXML private Label lblTicketMedio;
    @FXML private Label lblValorEstoque;
    @FXML private Label lblCapitalPedidos;
    @FXML private BarChart<String, Number> graficoFinanceiro;
    @FXML private CategoryAxis eixoX;
    @FXML private NumberAxis eixoY;

    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpDataInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpDataFim.setValue(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
        btnGerarRelatorio.setOnAction(_ -> atualizarDashboard());
        atualizarDashboard();
    }

    private void atualizarDashboard() {
        LocalDate dataInicio = dpDataInicio.getValue();
        LocalDate dataFim = dpDataFim.getValue();

        if (dataInicio == null || dataFim == null || dataInicio.isAfter(dataFim)) {
            mostrarAlerta("Datas Inválidas", "Por favor, selecione um período de datas válido.", Alert.AlertType.WARNING);
            return;
        }

        atualizarResumoPeriodo(dataInicio, dataFim);
        atualizarSaudeEstoque();
        atualizarGrafico(dataInicio, dataFim);
    }
    
    // --- MÉTODO COM A LÓGICA DE CUSTO CORRIGIDA ---
    private void atualizarResumoPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        // A consulta foi alterada para usar APENAS o custo registrado no momento da venda.
        // Isso evita que o custo atual de um produto seja usado para calcular o lucro de vendas antigas.
        String sql = "SELECT " +
                     "  (SELECT COALESCE(SUM(ValorFinalVenda), 0) FROM Vendas WHERE DataVenda >= ? AND DataVenda < ?) as Faturamento, " +
                     "  (SELECT COALESCE(SUM(IV.CustoMedioUnitarioRegistrado * IV.Quantidade), 0) " +
                     "   FROM ItensVenda IV JOIN Vendas V ON IV.VendaID = V.VendaID " +
                     "   WHERE V.DataVenda >= ? AND V.DataVenda < ?) as CustoProdutosVendidos, " +
                     "  (SELECT COUNT(VendaID) FROM Vendas WHERE DataVenda >= ? AND DataVenda < ?) as NumeroVendasUnicas";

        try (Connection con = UTIL.ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setDate(1, java.sql.Date.valueOf(dataInicio));
            pst.setDate(2, java.sql.Date.valueOf(dataFim.plusDays(1)));
            pst.setDate(3, java.sql.Date.valueOf(dataInicio));
            pst.setDate(4, java.sql.Date.valueOf(dataFim.plusDays(1)));
            pst.setDate(5, java.sql.Date.valueOf(dataInicio));
            pst.setDate(6, java.sql.Date.valueOf(dataFim.plusDays(1)));
            
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                double faturamento = rs.getDouble("Faturamento");
                double custo = rs.getDouble("CustoProdutosVendidos");
                int numVendas = rs.getInt("NumeroVendasUnicas");

                double lucro = faturamento - custo;
                double margem = (faturamento > 0) ? (lucro / faturamento) * 100 : 0;
                double ticketMedio = (numVendas > 0) ? faturamento / numVendas : 0;

                lblFaturamento.setText(currencyFormat.format(faturamento));
                lblCustoProdutos.setText(currencyFormat.format(custo));
                lblLucroBruto.setText(currencyFormat.format(lucro));
                lblMargemLucro.setText(String.format("%.2f%%", margem));
                lblTicketMedio.setText(currencyFormat.format(ticketMedio));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Erro de Banco de Dados", "Não foi possível calcular o resumo do período.", Alert.AlertType.ERROR);
        }
    }

    private void atualizarSaudeEstoque() {
        String sqlEstoque = "SELECT COALESCE(SUM(QuantidadeEstoque * PrecoVendaAtual), 0) AS valor FROM Produtos";
        String sqlPedidos = "SELECT COALESCE(SUM((IP.QuantidadePedida - IP.QuantidadeRecebida) * IP.CustoUnitarioComTaxas), 0) AS valor " +
                            "FROM ItensPedidoFornecedor IP JOIN PedidosFornecedor PF ON IP.PedidoFornecedorID = PF.PedidoFornecedorID " +
                            "WHERE PF.StatusPedido NOT IN ('Recebido Integralmente', 'Cancelado')";

        try (Connection con = UTIL.ConexaoBanco.conectar()) {
            try (PreparedStatement pst = con.prepareStatement(sqlEstoque); ResultSet rs = pst.executeQuery()) {
                if (rs.next()) lblValorEstoque.setText(currencyFormat.format(rs.getDouble("valor")));
            }
            try (PreparedStatement pst = con.prepareStatement(sqlPedidos); ResultSet rs = pst.executeQuery()) {
                if (rs.next()) lblCapitalPedidos.setText(currencyFormat.format(rs.getDouble("valor")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Erro de Banco de Dados", "Não foi possível calcular a saúde do estoque.", Alert.AlertType.ERROR);
        }
    }

    // --- GRÁFICO TAMBÉM CORRIGIDO ---
    private void atualizarGrafico(LocalDate dataInicio, LocalDate dataFim) {
        graficoFinanceiro.getData().clear();

        String sql = "SELECT " +
                     "  YEAR(V.DataVenda) as Ano, MONTH(V.DataVenda) as Mes, " +
                     "  SUM(V.ValorFinalVenda) as Faturamento, " +
                     "  SUM((SELECT COALESCE(SUM(IV.CustoMedioUnitarioRegistrado * IV.Quantidade), 0) FROM ItensVenda IV WHERE IV.VendaID = V.VendaID)) as Custo " +
                     "FROM Vendas V " +
                     "WHERE V.DataVenda >= ? AND V.DataVenda < ? " +
                     "GROUP BY YEAR(V.DataVenda), MONTH(V.DataVenda) " +
                     "ORDER BY Ano, Mes";
        
        XYChart.Series<String, Number> seriesFaturamento = new XYChart.Series<>();
        seriesFaturamento.setName("Faturamento");
        XYChart.Series<String, Number> seriesCusto = new XYChart.Series<>();
        seriesCusto.setName("Custo");
        XYChart.Series<String, Number> seriesLucro = new XYChart.Series<>();
        seriesLucro.setName("Lucro");

        try (Connection con = UTIL.ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setDate(1, java.sql.Date.valueOf(dataInicio));
            pst.setDate(2, java.sql.Date.valueOf(dataFim.plusDays(1)));
            ResultSet rs = pst.executeQuery();

            while(rs.next()){
                String mesAno = String.format("%02d/%d", rs.getInt("Mes"), rs.getInt("Ano"));
                double faturamento = rs.getDouble("Faturamento");
                double custo = rs.getDouble("Custo");
                double lucro = faturamento - custo;

                seriesFaturamento.getData().add(new XYChart.Data<>(mesAno, faturamento));
                seriesCusto.getData().add(new XYChart.Data<>(mesAno, custo));
                seriesLucro.getData().add(new XYChart.Data<>(mesAno, lucro));
            }
            
            graficoFinanceiro.getData().addAll(seriesFaturamento, seriesCusto, seriesLucro);

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Erro de Banco de Dados", "Não foi possível gerar os dados do gráfico.", Alert.AlertType.ERROR);
        }
    }
    
    private void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
