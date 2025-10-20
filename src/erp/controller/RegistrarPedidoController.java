package erp.controller;

import erp.model.ItemPedidoVO;
import UTIL.ConexaoBanco;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class RegistrarPedidoController implements Initializable {

    @FXML private CheckBox chkManterCampos;
    @FXML private TextField txtNomeFornecedor;
    @FXML private DatePicker dpDataPedido;
    @FXML private TextField txtItemClube;
    @FXML private TextField txtItemModelo;
    @FXML private ComboBox<String> cbItemTipo;
    @FXML private ComboBox<String> cbItemTamanho;
    @FXML private TextField txtItemQuantidade;
    @FXML private TextField txtItemCusto;
    @FXML private Button btnAdicionarItem;
    @FXML private TableView<ItemPedidoVO> tblItensPedido;
    @FXML private TableColumn<ItemPedidoVO, String> colModelo;
    @FXML private TableColumn<ItemPedidoVO, String> colClube;
    @FXML private TableColumn<ItemPedidoVO, String> colTipo;
    @FXML private TableColumn<ItemPedidoVO, String> colTamanho;
    @FXML private TableColumn<ItemPedidoVO, Integer> colQuantidade;
    @FXML private TableColumn<ItemPedidoVO, Double> colCusto;
    @FXML private TableColumn<ItemPedidoVO, Double> colSubtotal;
    @FXML private Button btnLimparPedido;
    @FXML private Button btnSalvarPedido;
    
    @FXML private Label lblTotalItensPedido;
    private final ObservableList<String> tamanhosAdulto = FXCollections.observableArrayList("P", "M", "G", "GG", "2GG", "3GG", "4GG");
    private final ObservableList<String> tamanhosInfantil = FXCollections.observableArrayList("16", "18", "20", "22", "24", "26", "28");


    private ObservableList<ItemPedidoVO> listaItensPedido = FXCollections.observableArrayList();
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTabela();
        configurarComboBoxes();
        dpDataPedido.setValue(LocalDate.now());
        atualizarContadorDeItens(); 
        configurarComboBoxesDinamicos();
    }
    
    private void configurarComboBoxes() {
        cbItemTipo.getItems().addAll("Masculina", "Feminina", "Infantil");
    }

    private void configurarTabela() {
        tblItensPedido.setItems(listaItensPedido);
        colModelo.setCellValueFactory(new PropertyValueFactory<>("modelo"));
        colClube.setCellValueFactory(new PropertyValueFactory<>("clube"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTamanho.setCellValueFactory(new PropertyValueFactory<>("tamanho"));
        colQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colCusto.setCellValueFactory(new PropertyValueFactory<>("custoUnitario"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        formatarColunaMoeda(colCusto);
        formatarColunaMoeda(colSubtotal);
    }
    
    // --- NOVO MÉTODO PARA ATUALIZAR O CONTADOR ---
    private void atualizarContadorDeItens() {
        int totalItens = 0;
        for (ItemPedidoVO item : listaItensPedido) {
            totalItens += item.getQuantidade();
        }
        lblTotalItensPedido.setText("Total: " + totalItens);
    }
    
    @FXML
    private void adicionarItemAoPedido() {
        if (txtItemClube.getText().trim().isEmpty() || txtItemModelo.getText().trim().isEmpty() || 
            cbItemTipo.getValue() == null || cbItemTamanho.getValue() == null || 
            txtItemQuantidade.getText().trim().isEmpty() || txtItemCusto.getText().trim().isEmpty()) {
            mostrarAlerta("Erro", "Preencha todos os campos do item.", Alert.AlertType.ERROR);
            return;
        }

        try {
            String clube = txtItemClube.getText().trim();
            String modelo = txtItemModelo.getText().trim();
            String tipo = cbItemTipo.getValue();
            String tamanho = cbItemTamanho.getValue();
            int quantidade = Integer.parseInt(txtItemQuantidade.getText().trim());
            double custo = Double.parseDouble(txtItemCusto.getText().trim().replace(",", "."));
            
            if (quantidade <= 0 || custo < 0) {
                mostrarAlerta("Erro", "Quantidade e custo devem ser valores positivos.", Alert.AlertType.ERROR);
                return;
            }

            ItemPedidoVO novoItem = new ItemPedidoVO(modelo, clube, tipo, tamanho, quantidade, custo);
            listaItensPedido.add(novoItem);
            
            limparFormularioItem();
            atualizarContadorDeItens(); // Atualiza o contador após adicionar
            
        } catch (NumberFormatException e) {
            mostrarAlerta("Erro", "Quantidade e Custo devem ser números válidos.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void salvarPedidoCompleto() {
        // (Este método permanece o mesmo)
        if (txtNomeFornecedor.getText().trim().isEmpty() || dpDataPedido.getValue() == null) {
            mostrarAlerta("Erro", "Preencha o Nome do Fornecedor e a Data do Pedido.", Alert.AlertType.ERROR);
            return;
        }
        if (listaItensPedido.isEmpty()) {
            mostrarAlerta("Erro", "Adicione pelo menos um item ao pedido antes de salvar.", Alert.AlertType.ERROR);
            return;
        }
        
        Connection con = null;
        try {
            con = ConexaoBanco.conectar();
            con.setAutoCommit(false);

            String sqlPedido = "INSERT INTO PedidosFornecedor (DataPedido, NomeFornecedor, StatusPedido) VALUES (?, ?, ?)";
            long pedidoId;
            try(PreparedStatement pstPedido = con.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
                pstPedido.setDate(1, java.sql.Date.valueOf(dpDataPedido.getValue()));
                pstPedido.setString(2, txtNomeFornecedor.getText().trim());
                pstPedido.setString(3, "Realizado");
                pstPedido.executeUpdate();

                ResultSet rs = pstPedido.getGeneratedKeys();
                if(rs.next()) {
                    pedidoId = rs.getLong(1);
                } else {
                    throw new SQLException("Falha ao criar o pedido, nenhum ID obtido.");
                }
            }

            String sqlItem = "INSERT INTO ItensPedidoFornecedor (PedidoFornecedorID, ProdutoID, QuantidadePedida, CustoUnitarioFornecedor, CustoUnitarioComTaxas) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstItem = con.prepareStatement(sqlItem)) {
                for (ItemPedidoVO itemVO : listaItensPedido) {
                    int produtoId = findOrCreateProdutoID(con, itemVO);

                    pstItem.setLong(1, pedidoId);
                    pstItem.setInt(2, produtoId);
                    pstItem.setInt(3, itemVO.getQuantidade());
                    pstItem.setDouble(4, itemVO.getCustoUnitario());
                    pstItem.setDouble(5, itemVO.getCustoUnitario());
                    pstItem.addBatch();
                }
                pstItem.executeBatch(); 
            }

            con.commit(); 
            mostrarAlerta("Sucesso", "Pedido #" + pedidoId + " salvo com sucesso!", Alert.AlertType.INFORMATION);
            limparPedido();

        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            mostrarAlerta("Erro de Banco de Dados", "Não foi possível salvar o pedido. A transação foi desfeita.\nErro: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            if (con != null) try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private int findOrCreateProdutoID(Connection con, ItemPedidoVO item) throws SQLException {
        // (Este método permanece o mesmo)
        String sqlSelect = "SELECT ProdutoID FROM Produtos WHERE Modelo = ? AND Clube = ? AND Tipo = ? AND Tamanho = ?";
        try (PreparedStatement pst = con.prepareStatement(sqlSelect)) {
            pst.setString(1, item.getModelo());
            pst.setString(2, item.getClube());
            pst.setString(3, item.getTipo());
            pst.setString(4, item.getTamanho());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt("ProdutoID"); 
            }
        }
        
        String sqlInsert = "INSERT INTO Produtos (Modelo, Clube, Tipo, Tamanho, PrecoVendaAtual, QuantidadeEstoque, CustoMedioPonderado) VALUES (?, ?, ?, ?, 140.00, 0, ?)";
        try(PreparedStatement pst = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, item.getModelo());
            pst.setString(2, item.getClube());
            pst.setString(3, item.getTipo());
            pst.setString(4, item.getTamanho());
            pst.setDouble(5, item.getCustoUnitario());
            pst.executeUpdate();
            
            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Falha ao criar o produto, nenhum ID obtido.");
            }
        }
    }
    
    @FXML
    private void limparPedido() {
        txtNomeFornecedor.clear();
        dpDataPedido.setValue(LocalDate.now());
        listaItensPedido.clear();
        limparFormularioItem();
        atualizarContadorDeItens(); // Atualiza o contador ao limpar o pedido
    }
    
    private void limparFormularioItem() {
        if (chkManterCampos == null || !chkManterCampos.isSelected()) {
            txtItemClube.clear();
            txtItemModelo.clear();
            cbItemTipo.getSelectionModel().clearSelection();
            cbItemTamanho.getSelectionModel().clearSelection();
            txtItemQuantidade.setText("1");
            txtItemCusto.clear();
            txtItemClube.requestFocus();
        } 
        else {
            cbItemTamanho.getSelectionModel().clearSelection();
            txtItemQuantidade.setText("1");
            cbItemTamanho.requestFocus();
        }
    }
    
    private void formatarColunaMoeda(TableColumn<ItemPedidoVO, Double> coluna) {
        // (Este método permanece o mesmo)
        coluna.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean empty) {
                super.updateItem(valor, empty);
                if (empty || valor == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(valor));
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
        // (Este método permanece o mesmo)
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
private void configurarComboBoxesDinamicos() {
    // Popula o ComboBox de Tipo normalmente
    cbItemTipo.getItems().addAll("Masculina", "Feminina", "Infantil");

    // Adiciona o listener para trocar os tamanhos
    cbItemTipo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
        if ("Infantil".equals(newValue)) {
            cbItemTamanho.setItems(tamanhosInfantil);
        } else {
            cbItemTamanho.setItems(tamanhosAdulto);
        }
        cbItemTamanho.getSelectionModel().selectFirst(); // Seleciona o primeiro item da nova lista
    });

    // Define a lista inicial de tamanhos
    cbItemTipo.getSelectionModel().selectFirst();
    cbItemTamanho.setItems(tamanhosAdulto);
}

}