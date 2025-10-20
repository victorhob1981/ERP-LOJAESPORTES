package erp.controller;

import erp.model.ItemPedidoDetalheVO;
import erp.model.PedidoVO;
import UTIL.ConexaoBanco;
import erp.model.ProdutoEstoque;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label; // Certifique-se que Label está importado
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;

public class AcompanhamentoController implements Initializable {

    // ... (declarações FXML existentes) ...
    @FXML private TableView<PedidoVO> tblPedidos;
    @FXML private TableColumn<PedidoVO, Integer> colPedidoId;
    @FXML private TableColumn<PedidoVO, LocalDate> colDataPedido;
    @FXML private TableColumn<PedidoVO, String> colFornecedor;
    @FXML private TableColumn<PedidoVO, Double> colCustoTotal;
    @FXML private TableColumn<PedidoVO, String> colStatus;
    @FXML private TableView<ItemPedidoDetalheVO> tblItensPedido;
    @FXML private TableColumn<ItemPedidoDetalheVO, String> colItemProduto;
    @FXML private TableColumn<ItemPedidoDetalheVO, Integer> colItemQtdPedida;
    @FXML private TableColumn<ItemPedidoDetalheVO, Integer> colItemQtdRecebida;
    @FXML private TableColumn<ItemPedidoDetalheVO, Double> colItemCustoUnit;
    @FXML private Button btnRegistrarChegada;
    @FXML private TableView<ProdutoEstoque> tblItensPendentes;
    @FXML private TableColumn<ProdutoEstoque, String> colPendenteClube;
    @FXML private TableColumn<ProdutoEstoque, String> colPendenteModelo;
    @FXML private TableColumn<ProdutoEstoque, String> colPendenteTipo;
    @FXML private TableColumn<ProdutoEstoque, Integer> colPendenteP;
    @FXML private TableColumn<ProdutoEstoque, Integer> colPendenteM;
    @FXML private TableColumn<ProdutoEstoque, Integer> colPendenteG;
    @FXML private TableColumn<ProdutoEstoque, Integer> colPendenteGG;
    @FXML private TableColumn<ProdutoEstoque, Integer> colPendente2GG;
    @FXML private TableColumn<ProdutoEstoque, Integer> colPendente3GG;
    @FXML private TableColumn<ProdutoEstoque, Integer> colPendente4GG;
    @FXML private TableColumn<ProdutoEstoque, Integer> colPendenteTotal;
    
    // --- NOVA DECLARAÇÃO FXML ADICIONADA ---
    @FXML private Label lblTotalPendentes;

    private final ObservableList<PedidoVO> listaPedidos = FXCollections.observableArrayList();
    private final ObservableList<ItemPedidoDetalheVO> listaItensPedido = FXCollections.observableArrayList();
    private final ObservableList<ProdutoEstoque> listaItensPendentes = FXCollections.observableArrayList();
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTabelas();
        configurarTabelaItensPendentes();
        configurarListeners();
        
        bindTableHeightToRowCount(tblItensPedido, listaItensPedido, 150);
        bindTableHeightToRowCount(tblItensPendentes, listaItensPendentes, 200);

        carregarDados();
    }

    private void bindTableHeightToRowCount(TableView<?> tableView, ObservableList<?> items, double minHeight) {
        final double ROW_HEIGHT = 24.5;
        final double HEADER_HEIGHT = 28.0;
        
        tableView.setMinHeight(minHeight);

        DoubleBinding tableHeight = Bindings.createDoubleBinding(() -> {
            int numRows = items.size();
            if (numRows == 0) {
                return minHeight;
            }
            return HEADER_HEIGHT + (numRows * ROW_HEIGHT) + 4;
        }, items);

        tableView.prefHeightProperty().bind(tableHeight);
    }
    
    private void carregarDados(){
        carregarPedidos();
        carregarItensPendentes();
    }

    private void configurarTabelas() {
        tblPedidos.setItems(listaPedidos);
        colPedidoId.setCellValueFactory(new PropertyValueFactory<>("pedidoId"));
        colDataPedido.setCellValueFactory(new PropertyValueFactory<>("dataPedido"));
        colFornecedor.setCellValueFactory(new PropertyValueFactory<>("nomeFornecedor"));
        colCustoTotal.setCellValueFactory(new PropertyValueFactory<>("custoTotal"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        formatarColunaMoeda(colCustoTotal);

        tblItensPedido.setItems(listaItensPedido);
        colItemProduto.setCellValueFactory(new PropertyValueFactory<>("descricaoProduto"));
        colItemQtdPedida.setCellValueFactory(new PropertyValueFactory<>("quantidadePedida"));
        colItemQtdRecebida.setCellValueFactory(new PropertyValueFactory<>("quantidadeRecebida"));
        colItemCustoUnit.setCellValueFactory(new PropertyValueFactory<>("custoUnitario"));
        formatarColunaMoeda(colItemCustoUnit);
        
        tblItensPedido.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

private void carregarPedidos() {
    listaPedidos.clear();
    String sql = "SELECT PF.PedidoFornecedorID, PF.DataPedido, PF.NomeFornecedor, PF.StatusPedido, " +
                 " (SELECT SUM(IP.QuantidadePedida * IP.CustoUnitarioFornecedor) " +
                 "  FROM ItensPedidoFornecedor IP " +
                 "  WHERE IP.PedidoFornecedorID = PF.PedidoFornecedorID) AS CustoCalculado " +
                 "FROM PedidosFornecedor PF " +
                 "ORDER BY " +
                 "  CASE " +
                 "    WHEN PF.StatusPedido = 'Realizado' THEN 1 " +
                 "    WHEN PF.StatusPedido = 'Recebido Parcialmente' THEN 2 " +
                 "    WHEN PF.StatusPedido = 'Recebido Integralmente' THEN 3 " +
                 "    ELSE 4 " +
                 "  END, " +
                 "PF.DataPedido DESC";

    try (Connection con = ConexaoBanco.conectar();
         PreparedStatement pst = con.prepareStatement(sql);
         ResultSet rs = pst.executeQuery()) {
        
        while(rs.next()) {
            listaPedidos.add(new PedidoVO(
                rs.getInt("PedidoFornecedorID"),
                rs.getDate("DataPedido").toLocalDate(),
                rs.getString("NomeFornecedor"),
                rs.getDouble("CustoCalculado"),
                rs.getString("StatusPedido")
            ));
        }
    } catch (SQLException e) {
        e.printStackTrace();
        mostrarAlerta("Erro de Banco de Dados", "Não foi possível carregar os pedidos: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}

    private void carregarItensPendentes() {
        listaItensPendentes.clear();
        Map<String, ProdutoEstoque> mapaProdutosPendentes = new HashMap<>();
        int totalGeralPendente = 0; 
        
        String sql = "SELECT P.Clube, P.Modelo, P.Tipo, P.Tamanho, " +
                     "(IP.QuantidadePedida - IP.QuantidadeRecebida) AS QuantidadePendente " +
                     "FROM ItensPedidoFornecedor IP " +
                     "JOIN Produtos P ON IP.ProdutoID = P.ProdutoID " +
                     "JOIN PedidosFornecedor PF ON IP.PedidoFornecedorID = PF.PedidoFornecedorID " +
                     "WHERE (IP.QuantidadePedida > IP.QuantidadeRecebida) " +
                     "AND PF.StatusPedido NOT IN ('Recebido Integralmente', 'Cancelado')";

        try (Connection con = ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                int quantidadePendente = rs.getInt("QuantidadePendente");
                totalGeralPendente += quantidadePendente; // <-- Soma a quantidade ao total

                String clube = rs.getString("Clube").trim();
                String modelo = rs.getString("Modelo").trim();
                String tipo = rs.getString("Tipo").trim();
                String chaveProduto = clube + "|" + modelo + "|" + tipo;
                
                ProdutoEstoque produto = mapaProdutosPendentes.computeIfAbsent(chaveProduto, k -> new ProdutoEstoque(modelo, clube, tipo));
                
                produto.setQuantidadeParaTamanho(rs.getString("Tamanho"), quantidadePendente);
            }
            listaItensPendentes.setAll(mapaProdutosPendentes.values());
            lblTotalPendentes.setText("Total: " + totalGeralPendente);

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível carregar o resumo de itens pendentes.", Alert.AlertType.ERROR);
        }
    }

    
    private void configurarTabelaItensPendentes() {
        tblItensPendentes.setItems(listaItensPendentes);
        colPendenteClube.setCellValueFactory(new PropertyValueFactory<>("clube"));
        colPendenteModelo.setCellValueFactory(new PropertyValueFactory<>("modelo"));
        colPendenteTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colPendenteP.setCellValueFactory(new PropertyValueFactory<>("quantidadeP"));
        colPendenteM.setCellValueFactory(new PropertyValueFactory<>("quantidadeM"));
        colPendenteG.setCellValueFactory(new PropertyValueFactory<>("quantidadeG"));
        colPendenteGG.setCellValueFactory(new PropertyValueFactory<>("quantidadeGG"));
        colPendente2GG.setCellValueFactory(new PropertyValueFactory<>("quantidade2GG"));
        colPendente3GG.setCellValueFactory(new PropertyValueFactory<>("quantidade3GG"));
        colPendente4GG.setCellValueFactory(new PropertyValueFactory<>("quantidade4GG"));
        colPendenteTotal.setCellValueFactory(new PropertyValueFactory<>("quantidadeTotal"));

        formatarCelulaQuantidade(colPendenteP);
        formatarCelulaQuantidade(colPendenteM);
        formatarCelulaQuantidade(colPendenteG);
        formatarCelulaQuantidade(colPendenteGG);
        formatarCelulaQuantidade(colPendente2GG);
        formatarCelulaQuantidade(colPendente3GG);
        formatarCelulaQuantidade(colPendente4GG);
        formatarCelulaQuantidade(colPendenteTotal);
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

    private void configurarListeners() {
        tblPedidos.getSelectionModel().selectedItemProperty().addListener((_obs, _oldSelection, newSelection) -> {
            if (newSelection != null) {
                carregarItensDoPedido(newSelection.getPedidoId());
            } else {
                listaItensPedido.clear();
                btnRegistrarChegada.setDisable(true);
            }
        });

        tblItensPedido.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> 
            btnRegistrarChegada.setDisable(tblItensPedido.getSelectionModel().getSelectedItems().isEmpty())
        );

        btnRegistrarChegada.setOnAction(event -> {
            ObservableList<ItemPedidoDetalheVO> itensSelecionados = tblItensPedido.getSelectionModel().getSelectedItems();
            
            if (itensSelecionados.isEmpty()) {
                mostrarAlerta("Atenção", "Nenhum item selecionado para registrar a chegada.", Alert.AlertType.WARNING);
                return;
            }

            List<ItemPedidoDetalheVO> itensParaProcessar = itensSelecionados.filtered(item -> item.getQuantidadeRecebida() < item.getQuantidadePedida());

            if (itensParaProcessar.isEmpty()) {
                mostrarAlerta("Atenção", "O(s) item(ns) selecionado(s) já foi(foram) totalmente recebido(s).", Alert.AlertType.WARNING);
                return;
            }

            if (itensParaProcessar.size() == 1) {
                registrarChegadaDialogo(itensParaProcessar.get(0));
            } else {
                Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacao.setTitle("Registrar Chegada Múltipla");
                confirmacao.setHeaderText("Registrar chegada para " + itensParaProcessar.size() + " itens selecionados?");
                confirmacao.setContentText("Esta ação registrará a chegada da quantidade TOTAL pendente para cada um dos itens. Deseja continuar?");
                
                Optional<ButtonType> result = confirmacao.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    processarChegadaMultiplosItens(itensParaProcessar);
                }
            }
        });
        
        btnRegistrarChegada.setDisable(true);
    }
    
    private void carregarItensDoPedido(int pedidoId) {
        listaItensPedido.clear();
        String sql = "SELECT IP.ItemPedidoFornecedorID, IP.ProdutoID, " +
                     "CONCAT(P.clube, ' ', P.modelo, ' ', P.tipo, ' ', P.tamanho) AS DescricaoProduto, " +
                     "IP.QuantidadePedida, IP.QuantidadeRecebida, IP.CustoUnitarioFornecedor " +
                     "FROM ItensPedidoFornecedor IP " +
                     "JOIN Produtos P ON IP.ProdutoID = P.ProdutoID WHERE IP.PedidoFornecedorID = ?";
        try (Connection con = ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, pedidoId);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                listaItensPedido.add(new ItemPedidoDetalheVO(
                    rs.getInt("ItemPedidoFornecedorID"), rs.getInt("ProdutoID"),
                    rs.getString("DescricaoProduto"), rs.getInt("QuantidadePedida"),
                    rs.getInt("QuantidadeRecebida"), rs.getDouble("CustoUnitarioFornecedor")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Erro de Banco de Dados", "Não foi possível carregar os itens do pedido: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            btnRegistrarChegada.setDisable(listaItensPedido.isEmpty());
        }
    }
    
    private void processarChegadaMultiplosItens(List<ItemPedidoDetalheVO> itens) {
        Connection con = null;
        try {
            con = ConexaoBanco.conectar();
            con.setAutoCommit(false);

            for (ItemPedidoDetalheVO item : itens) {
                int qtdFaltante = item.getQuantidadePedida() - item.getQuantidadeRecebida();
                executarLogicaDeChegada(con, item, qtdFaltante);
            }

            con.commit();
            mostrarAlerta("Sucesso", itens.size() + " tipos de itens tiveram sua chegada registrada e o estoque foi atualizado.", Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            mostrarAlerta("Erro de Banco de Dados", "Falha ao registrar chegada de itens. A operação foi desfeita.", Alert.AlertType.ERROR);
        } finally {
            if (con != null) try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
            
            int selectedPedidoIndex = tblPedidos.getSelectionModel().getSelectedIndex();
            carregarDados();
            if(selectedPedidoIndex != -1) {
                tblPedidos.getSelectionModel().select(selectedPedidoIndex);
            }
        }
    }
    
    private void registrarChegadaDialogo(ItemPedidoDetalheVO itemSelecionado) {
        int qtdFaltante = itemSelecionado.getQuantidadePedida() - itemSelecionado.getQuantidadeRecebida();
        TextInputDialog dialog = new TextInputDialog(String.valueOf(qtdFaltante));
        dialog.setTitle("Registrar Chegada de Item");
        dialog.setHeaderText("Produto: " + itemSelecionado.getDescricaoProduto());
        dialog.setContentText("Digite a quantidade que chegou:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(qtdStr -> {
            try {
                int qtdRecebida = Integer.parseInt(qtdStr);
                if (qtdRecebida <= 0 || qtdRecebida > qtdFaltante) {
                    mostrarAlerta("Erro", "A quantidade deve ser um número positivo e não pode ser maior que a quantidade faltante (" + qtdFaltante + ").", Alert.AlertType.ERROR);
                    return;
                }
                
                processarChegadaItemUnico(itemSelecionado, qtdRecebida);

            } catch (NumberFormatException e) {
                mostrarAlerta("Erro", "Por favor, digite um número válido.", Alert.AlertType.ERROR);
            }
        });
    }

    private void processarChegadaItemUnico(ItemPedidoDetalheVO item, int quantidadeChegou) {
        Connection con = null;
        try {
            con = ConexaoBanco.conectar();
            con.setAutoCommit(false);
            executarLogicaDeChegada(con, item, quantidadeChegou);
            con.commit();
            mostrarAlerta("Sucesso", "Chegada registrada e estoque atualizado.", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            mostrarAlerta("Erro de Banco de Dados", "Falha ao registrar chegada do item. A operação foi desfeita.", Alert.AlertType.ERROR);
        } finally {
            if (con != null) try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
            int selectedPedidoIndex = tblPedidos.getSelectionModel().getSelectedIndex();
            carregarDados();
            if(selectedPedidoIndex != -1) {
                tblPedidos.getSelectionModel().select(selectedPedidoIndex);
            }
        }
    }
    
    private void executarLogicaDeChegada(Connection con, ItemPedidoDetalheVO item, int quantidadeChegou) throws SQLException {
        String sqlUpdateItem = "UPDATE ItensPedidoFornecedor SET QuantidadeRecebida = QuantidadeRecebida + ? WHERE ItemPedidoFornecedorID = ?";
        try (PreparedStatement pst = con.prepareStatement(sqlUpdateItem)) {
            pst.setInt(1, quantidadeChegou);
            pst.setInt(2, item.getItemPedidoId());
            pst.executeUpdate();
        }

        String sqlSelectProduto = "SELECT QuantidadeEstoque, CustoMedioPonderado FROM Produtos WHERE ProdutoID = ?";
        double estoqueAntigo = 0, custoMedioAntigo = 0;
        try(PreparedStatement pst = con.prepareStatement(sqlSelectProduto)) {
            pst.setInt(1, item.getProdutoId());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                estoqueAntigo = rs.getInt("QuantidadeEstoque");
                custoMedioAntigo = rs.getDouble("CustoMedioPonderado");
            }
        }
        
        double novoEstoque = estoqueAntigo + quantidadeChegou;
        double custoDoLote = item.getCustoUnitario();
        double novoCustoMedio = (novoEstoque > 0) ? ((estoqueAntigo * custoMedioAntigo) + (quantidadeChegou * custoDoLote)) / novoEstoque : custoDoLote;

        String sqlUpdateProduto = "UPDATE Produtos SET QuantidadeEstoque = ?, CustoMedioPonderado = ?, DataUltimaEntradaEstoque = CURDATE() WHERE ProdutoID = ?";
        try (PreparedStatement pst = con.prepareStatement(sqlUpdateProduto)) {
            pst.setDouble(1, novoEstoque);
            pst.setDouble(2, novoCustoMedio);
            pst.setInt(3, item.getProdutoId());
            pst.executeUpdate();
        }

        int pedidoId = tblPedidos.getSelectionModel().getSelectedItem().getPedidoId();
        String sqlVerificaStatus = "SELECT SUM(QuantidadePedida) as totalPedida, SUM(QuantidadeRecebida) as totalRecebida FROM ItensPedidoFornecedor WHERE PedidoFornecedorID = ?";
        try (PreparedStatement pst = con.prepareStatement(sqlVerificaStatus)) {
            pst.setInt(1, pedidoId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String novoStatus = (rs.getInt("totalRecebida") >= rs.getInt("totalPedida")) ? "Recebido Integralmente" : "Recebido Parcialmente";
                String sqlUpdatePedido = "UPDATE PedidosFornecedor SET StatusPedido = ? WHERE PedidoFornecedorID = ?";
                try (PreparedStatement pstUpdate = con.prepareStatement(sqlUpdatePedido)) {
                    pstUpdate.setString(1, novoStatus);
                    pstUpdate.setInt(2, pedidoId);
                    pstUpdate.executeUpdate();
                }
            }
        }
        System.out.println("Item " + item.getDescricaoProduto() + " processado com sucesso.");
    }
    
    private <T> void formatarColunaMoeda(TableColumn<T, Double> coluna) {
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean empty) {
                super.updateItem(valor, empty);
                if (empty || valor == null) setText(null);
                else {
                    setText(currencyFormat.format(valor));
                    setAlignment(Pos.CENTER_RIGHT);
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