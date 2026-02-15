package erp.controller;

import UTIL.ConexaoBanco;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class TelaInicialController implements Initializable {

    @FXML private Label lblFaturamentoMes;
    @FXML private Label lblLucroMes;
    @FXML private Label lblEncomendasAbertas;
    @FXML private Label lblPedidosAbertos;
    @FXML private ListView<String> lvEncomendasPendentes;
    @FXML private ListView<String> lvPedidosEmTransito;

    private MainLayoutController mainLayoutController;
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(this::carregarDadosDashboard);
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    public void carregarDadosDashboard() {
        carregarKPIs();
        carregarListaEncomendas();
        carregarListaPedidos();
    }
    
    // --- MÉTODO COM A LÓGICA CORRIGIDA ---
    private void carregarKPIs() {
        // Consulta para os KPIs financeiros (Faturamento e Custo/Lucro do mês atual)
        // A consulta foi dividida em duas subconsultas para garantir a precisão dos valores.
        String sqlFinanceiro = "SELECT " +
            "(SELECT COALESCE(SUM(ValorFinalVenda), 0) FROM Vendas WHERE StatusPagamento = 'Pago' AND MONTH(DataVenda) = MONTH(CURDATE()) AND YEAR(DataVenda) = YEAR(CURDATE())) as FaturamentoMes, " +
            "(SELECT COALESCE(SUM(IF(IV.CustoMedioUnitarioRegistrado > 0, IV.CustoMedioUnitarioRegistrado, P.CustoMedioPonderado) * IV.Quantidade), 0) " +
            " FROM ItensVenda IV JOIN Vendas V ON IV.VendaID = V.VendaID JOIN Produtos P ON IV.ProdutoID = P.ProdutoID " +
            " WHERE V.StatusPagamento = 'Pago' AND MONTH(V.DataVenda) = MONTH(CURDATE()) AND YEAR(V.DataVenda) = YEAR(CURDATE())) as CustoMes";

        // Consulta para os KPIs operacionais (Contagens de pedidos e encomendas)
        String sqlOperacional = "SELECT " +
            "(SELECT COUNT(*) FROM EncomendasCliente WHERE StatusEncomenda NOT IN ('EntregueAoCliente', 'Cancelada')) as EncomendasAbertas, " +
            "(SELECT COUNT(*) FROM PedidosFornecedor WHERE StatusPedido NOT IN ('Recebido Integralmente', 'Cancelado')) as PedidosAbertos";

        try (Connection con = ConexaoBanco.conectar()) {
            // Executa a primeira consulta (Financeiro)
            try (PreparedStatement pst = con.prepareStatement(sqlFinanceiro);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    double faturamento = rs.getDouble("FaturamentoMes");
                    double custo = rs.getDouble("CustoMes");
                    double lucro = faturamento - custo;
                    
                    lblFaturamentoMes.setText(currencyFormat.format(faturamento));
                    lblLucroMes.setText(currencyFormat.format(lucro));
                }
            }
            
            // Executa a segunda consulta (Operacional)
            try (PreparedStatement pst = con.prepareStatement(sqlOperacional);
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    lblEncomendasAbertas.setText(String.valueOf(rs.getInt("EncomendasAbertas")));
                    lblPedidosAbertos.setText(String.valueOf(rs.getInt("PedidosAbertos")));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            // Opcional: Adicionar um alerta para o usuário
        }
    }

    private void carregarListaEncomendas() {
        ObservableList<String> encomendas = FXCollections.observableArrayList();
        String sql = "SELECT C.NomeCliente, E.Clube, E.Modelo, E.Tamanho FROM EncomendasCliente E LEFT JOIN Clientes C ON E.ClienteID = C.ClienteID WHERE E.StatusEncomenda NOT IN ('EntregueAoCliente', 'Cancelada') ORDER BY E.DataEncomenda ASC LIMIT 5";
        
        try (Connection con = ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while(rs.next()) {
                String descricaoCompleta = rs.getString("Clube") + " " + rs.getString("Modelo") + " " + rs.getString("Tamanho");
                String nomeCliente = rs.getString("NomeCliente") != null ? rs.getString("NomeCliente") : "N/A";
                encomendas.add(descricaoCompleta + " (Cliente: " + nomeCliente + ")");
            }
            lvEncomendasPendentes.setItems(encomendas);
            if (encomendas.isEmpty()) {
                lvEncomendasPendentes.setPlaceholder(new Label("Nenhuma encomenda pendente."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void carregarListaPedidos() {
        ObservableList<String> pedidos = FXCollections.observableArrayList();
        String sql = "SELECT PedidoFornecedorID, NomeFornecedor, StatusPedido FROM PedidosFornecedor WHERE StatusPedido IN ('Realizado', 'Recebido Parcialmente') ORDER BY DataPedido ASC LIMIT 5";
        
        try (Connection con = ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while(rs.next()) {
                pedidos.add("Pedido #" + rs.getInt("PedidoFornecedorID") + " (" + rs.getString("NomeFornecedor") + ") - " + rs.getString("StatusPedido"));
            }
            lvPedidosEmTransito.setItems(pedidos);
            if (pedidos.isEmpty()) {
                lvPedidosEmTransito.setPlaceholder(new Label("Nenhum pedido em trânsito."));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML private void navNovaVenda(ActionEvent event) {
        if (mainLayoutController != null) mainLayoutController.irParaRegistrarVenda(event);
    }
    @FXML private void navHistoricoVendas(ActionEvent event) {
        if (mainLayoutController != null) mainLayoutController.irParaHistoricoVendas(event);
    }
    @FXML private void navEstoque(ActionEvent event) {
        if (mainLayoutController != null) mainLayoutController.irParaGerenciarEstoque(event);
    }
    @FXML private void navFinanceiro(ActionEvent event) {
        if (mainLayoutController != null) mainLayoutController.irParaFinanceiro(event);
    }
    @FXML private void navNovaEncomenda(ActionEvent event) {
        if (mainLayoutController != null) mainLayoutController.irParaEncomendas(event);
    }
    @FXML private void navAcompanharPedidos(ActionEvent event) {
        if (mainLayoutController != null) mainLayoutController.irParaAcompanhamento(event);
    }
    @FXML private void navNovoPedido(ActionEvent event) {
        if (mainLayoutController != null) mainLayoutController.irParaRegistrarPedido(event);
    }
}
