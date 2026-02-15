package erp.controller;

import erp.model.ProdutoAgregadoVO;
import erp.model.ProdutoAgregadoVO.DetalheTamanho;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;

public class TelaVendasController implements Initializable {
    
    // Comparador para ordenar os tamanhos de roupa corretamente.
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

    // Componentes FXML da interface
    @FXML private ComboBox<ProdutoAgregadoVO> cbProduto;
    @FXML private ComboBox<String> cbTipo; // Novo ComboBox para o tipo
    @FXML private FlowPane fpTamanhosDisponiveis;
    @FXML private TextField txtQuantidadeVendida;
    @FXML private TextField txtValorVenda;
    @FXML private TextField txtNomeCliente;
    @FXML private DatePicker dpDataVenda;
    @FXML private TextField txtDesconto;
    @FXML private CheckBox chkPago;
    @FXML private DatePicker dpDataPrometida;
    @FXML private ComboBox<String> cbMetodoPagamento;
    @FXML private Label lblSubtotalCalculado;
    @FXML private Label lblDescontoAplicado;
    @FXML private Label lblTotalAPagar;
    @FXML private Button btnSalvarVenda;
    @FXML private Button btnLimparCampos;

    // Variáveis de controle
    private ObservableList<ProdutoAgregadoVO> listaProdutosSugeridos = FXCollections.observableArrayList();
    private ToggleGroup grupoTamanhos = new ToggleGroup();
    private ProdutoAgregadoVO produtoSelecionado;
    private DetalheTamanho tamanhoSelecionadoDetalhe;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarComboBoxProduto();
        configurarListeners();
        limparFormularioVenda();
        
        cbMetodoPagamento.getItems().addAll("Pix", "Cartão de Crédito", "Dinheiro");
    }

    private void configurarComboBoxProduto() {
        cbProduto.setItems(listaProdutosSugeridos);
        cbProduto.setEditable(true);

        cbProduto.setConverter(new StringConverter<ProdutoAgregadoVO>() {
            @Override
            public String toString(ProdutoAgregadoVO produto) {
                return produto == null ? "" : produto.getDescricaoModelo();
            }

            @Override
            public ProdutoAgregadoVO fromString(String string) {
                return listaProdutosSugeridos.stream()
                        .filter(p -> p.getDescricaoModelo().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        cbProduto.getEditor().textProperty().addListener((_, _, newValue) -> {
            if (newValue == null || newValue.trim().length() < 3) {
                listaProdutosSugeridos.clear();
                cbProduto.hide();
            } else {
                 if (cbProduto.isFocused() && (produtoSelecionado == null || !newValue.equals(produtoSelecionado.getDescricaoModelo()))) {
                    buscarProdutosSugeridos(newValue);
                }
            }
        });

        cbProduto.valueProperty().addListener((_, _, novoProduto) -> {
            this.produtoSelecionado = novoProduto;
            atualizarOpcoesDeTipo();
        });
    }
    
    private void buscarProdutosSugeridos(String textoBusca) {
        Map<String, ProdutoAgregadoVO> mapaProdutos = new HashMap<>();
        String sql = "SELECT ProdutoID, clube, modelo, tipo, tamanho, PrecoVendaAtual, CustoMedioPonderado, QuantidadeEstoque " +
                     "FROM Produtos " +
                     "WHERE (CONCAT(clube, ' ', modelo) LIKE ?) AND QuantidadeEstoque > 0 " +
                     "ORDER BY clube, modelo, tipo, tamanho " +
                     "LIMIT 50";

        try (Connection con = UTIL.ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {
            
            pst.setString(1, "%" + textoBusca + "%");
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                String descricaoModelo = rs.getString("clube") + " " + rs.getString("modelo");
                ProdutoAgregadoVO prodAgregado = mapaProdutos.computeIfAbsent(descricaoModelo, ProdutoAgregadoVO::new);
                
                prodAgregado.adicionarVariante(
                    rs.getString("tipo"),
                    rs.getString("tamanho"), 
                    rs.getInt("ProdutoID"), 
                    rs.getDouble("PrecoVendaAtual"), 
                    rs.getInt("QuantidadeEstoque"),
                    rs.getDouble("CustoMedioPonderado")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        listaProdutosSugeridos.setAll(mapaProdutos.values());
        if (!listaProdutosSugeridos.isEmpty()) {
            cbProduto.show();
        }
    }

    private void atualizarOpcoesDeTipo() {
        cbTipo.getItems().clear();
        fpTamanhosDisponiveis.getChildren().clear();
        txtValorVenda.clear();
        tamanhoSelecionadoDetalhe = null;

        if (produtoSelecionado != null) {
            List<String> tipos = new ArrayList<>(produtoSelecionado.getTipos());
            cbTipo.setItems(FXCollections.observableArrayList(tipos));
            // Se houver apenas um tipo, seleciona-o automaticamente
            if (tipos.size() == 1) {
                cbTipo.getSelectionModel().selectFirst();
            }
        }
    }

    private void atualizarOpcoesDeTamanho() {
        fpTamanhosDisponiveis.getChildren().clear();
        txtValorVenda.clear();
        tamanhoSelecionadoDetalhe = null;

        String tipoSelecionado = cbTipo.getValue();
        if (produtoSelecionado != null && tipoSelecionado != null) {
            Map<String, DetalheTamanho> tamanhosDoTipo = produtoSelecionado.getTamanhosPorTipo(tipoSelecionado);
            List<String> tamanhosOrdenaveis = new ArrayList<>(tamanhosDoTipo.keySet());
            tamanhosOrdenaveis.sort(tamanhoComparator);

            for (String tamanho : tamanhosOrdenaveis) {
                DetalheTamanho detalhe = tamanhosDoTipo.get(tamanho);
                
                ToggleButton btnTamanho = new ToggleButton(tamanho);
                btnTamanho.setToggleGroup(grupoTamanhos);
                btnTamanho.setUserData(detalhe);
                
                btnTamanho.setOnAction(event -> {
                    if (btnTamanho.isSelected()) {
                        tamanhoSelecionadoDetalhe = (DetalheTamanho) btnTamanho.getUserData();
                        txtValorVenda.setText(String.format("%.2f", tamanhoSelecionadoDetalhe.getPrecoVenda()).replace(",", "."));
                        Platform.runLater(() -> txtQuantidadeVendida.requestFocus());
                    } else {
                        txtValorVenda.clear();
                        tamanhoSelecionadoDetalhe = null;
                    }
                    atualizarResumoVenda();
                });
                fpTamanhosDisponiveis.getChildren().add(btnTamanho);
            }
        }
    }
    
    private void configurarListeners() {
        btnSalvarVenda.setOnAction(event -> salvarVenda());
        
        // Listener para o ComboBox de Tipo, que atualiza os tamanhos disponíveis
        cbTipo.getSelectionModel().selectedItemProperty().addListener((_, _, novoTipo) -> {
            if (novoTipo != null) {
                atualizarOpcoesDeTamanho();
            }
        });

        txtQuantidadeVendida.textProperty().addListener((_, _, _) -> atualizarResumoVenda());
        txtValorVenda.textProperty().addListener((_, _, _) -> atualizarResumoVenda());
        txtDesconto.textProperty().addListener((_, _, _) -> atualizarResumoVenda());
        chkPago.setOnAction(_ -> {
            dpDataPrometida.setDisable(chkPago.isSelected());
            if (chkPago.isSelected()) {
                dpDataPrometida.setValue(null);
            }
        });
    }

    private void atualizarResumoVenda() {
        try {
            int quantidade = txtQuantidadeVendida.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtQuantidadeVendida.getText().trim());
            double precoUnitario = txtValorVenda.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtValorVenda.getText().trim().replace(",", "."));
            double desconto = txtDesconto.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtDesconto.getText().trim().replace(",", "."));
            double subtotal = quantidade * precoUnitario;
            double totalAPagar = subtotal - desconto;
            lblSubtotalCalculado.setText(String.format("R$ %.2f", subtotal));
            lblDescontoAplicado.setText(String.format("R$ %.2f", desconto));
            lblTotalAPagar.setText(String.format("R$ %.2f", totalAPagar));
        } catch (NumberFormatException e) {
            lblSubtotalCalculado.setText("R$ 0,00");
            lblDescontoAplicado.setText("R$ 0,00");
            lblTotalAPagar.setText("R$ 0,00");
        }
    }
    
    @FXML
    private void salvarVenda() {
        if (produtoSelecionado == null || cbTipo.getValue() == null || tamanhoSelecionadoDetalhe == null || dpDataVenda.getValue() == null || txtQuantidadeVendida.getText().trim().isEmpty() || txtValorVenda.getText().trim().isEmpty() || cbMetodoPagamento.getValue() == null) {
            mostrarAlerta("Erro de Validação", "Produto, Tipo, Tamanho, Data, Quantidade, Preço Unitário e Método de Pagamento são obrigatórios.", Alert.AlertType.ERROR);
            return;
        }

        double precoUnitarioVenda;
        int quantidadeVendida;
        double valorDesconto = 0.0;

        try {
            precoUnitarioVenda = Double.parseDouble(txtValorVenda.getText().trim().replace(",", "."));
            quantidadeVendida = Integer.parseInt(txtQuantidadeVendida.getText().trim());
            if (!txtDesconto.getText().trim().isEmpty()) valorDesconto = Double.parseDouble(txtDesconto.getText().trim().replace(",", "."));
            if (quantidadeVendida <= 0) {
                 mostrarAlerta("Erro de Validação", "A quantidade deve ser maior que zero.", Alert.AlertType.ERROR);
                return;
            }
            if (valorDesconto < 0) {
                mostrarAlerta("Erro de Validação", "O desconto não pode ser negativo.", Alert.AlertType.ERROR);
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Erro de Formato", "Valores numéricos inválidos.", Alert.AlertType.ERROR);
            return;
        }

        if (tamanhoSelecionadoDetalhe.getEstoque() < quantidadeVendida) {
            mostrarAlerta("Erro de Estoque", "Estoque insuficiente. Disponível: " + tamanhoSelecionadoDetalhe.getEstoque(), Alert.AlertType.ERROR);
            return;

        }

  
    

        double valorTotalItens = precoUnitarioVenda * quantidadeVendida;
        if (valorDesconto > valorTotalItens) {
            mostrarAlerta("Erro de Validação", "O desconto não pode ser maior que o valor total dos itens.", Alert.AlertType.ERROR);
            return;
        }
        double valorFinalVenda = valorTotalItens - valorDesconto;
        String nomeCliente = txtNomeCliente.getText().trim();
        LocalDate dataVendaLocal = dpDataVenda.getValue();
        boolean pago = chkPago.isSelected();
        LocalDate dataPrometidaLocal = dpDataPrometida.getValue();
        String metodoPagamento = cbMetodoPagamento.getValue();

        Connection con = null;
        try {
            con = UTIL.ConexaoBanco.conectar();
            con.setAutoCommit(false);
            
            Integer clienteId = getClienteID(con, nomeCliente);
            int produtoId = tamanhoSelecionadoDetalhe.getProdutoId();
            double custoMedio = tamanhoSelecionadoDetalhe.getCustoMedio();
            
            String sqlVenda = "INSERT INTO Vendas (ClienteID, DataVenda, ValorTotalItens, ValorDesconto, ValorFinalVenda, StatusPagamento, DataPrometidaPagamento, MetodoPagamento) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            long vendaIdGerado;
            try (PreparedStatement pstVenda = con.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                if (clienteId != null) pstVenda.setInt(1, clienteId); else pstVenda.setNull(1, java.sql.Types.INTEGER);
                pstVenda.setDate(2, java.sql.Date.valueOf(dataVendaLocal));
                pstVenda.setDouble(3, valorTotalItens);
                pstVenda.setDouble(4, valorDesconto);
                pstVenda.setDouble(5, valorFinalVenda);
                pstVenda.setString(6, pago ? "Pago" : "Pendente");
                if (!pago && dataPrometidaLocal != null) pstVenda.setDate(7, java.sql.Date.valueOf(dataPrometidaLocal)); else pstVenda.setNull(7, java.sql.Types.DATE);
                pstVenda.setString(8, metodoPagamento);
                pstVenda.executeUpdate();
                ResultSet rsKeys = pstVenda.getGeneratedKeys();
                if (rsKeys.next()) vendaIdGerado = rsKeys.getLong(1); else throw new SQLException("Falha ao obter ID da venda.");
            }

            String sqlPreco = "UPDATE Produtos SET PrecoVendaAtual = ? WHERE ProdutoID = ? AND PrecoVendaAtual < ?";
        try (PreparedStatement pstPreco = con.prepareStatement(sqlPreco)) {
        pstPreco.setDouble(1, precoUnitarioVenda);
        pstPreco.setInt(2, produtoId);
        pstPreco.setDouble(3, precoUnitarioVenda);
        pstPreco.executeUpdate();
    }   

            String sqlItemVenda = "INSERT INTO ItensVenda (VendaID, ProdutoID, Quantidade, PrecoVendaUnitarioRegistrado, CustoMedioUnitarioRegistrado) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstItem = con.prepareStatement(sqlItemVenda)) {
                pstItem.setLong(1, vendaIdGerado);
                pstItem.setInt(2, produtoId);
                pstItem.setInt(3, quantidadeVendida);
                pstItem.setDouble(4, precoUnitarioVenda);
                pstItem.setDouble(5, custoMedio);
                pstItem.executeUpdate();
            }

            String sqlEstoque = "UPDATE Produtos SET QuantidadeEstoque = QuantidadeEstoque - ? WHERE ProdutoID = ?";
            try (PreparedStatement pstEstoque = con.prepareStatement(sqlEstoque)) {
                pstEstoque.setInt(1, quantidadeVendida);
                pstEstoque.setInt(2, produtoId);
                pstEstoque.executeUpdate();
            }

            con.commit();
            mostrarAlerta("Sucesso", "Venda registrada com sucesso! ID da Venda: " + vendaIdGerado, Alert.AlertType.INFORMATION);
            limparFormularioVenda();

        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            mostrarAlerta("Erro de Banco de Dados", "Não foi possível salvar a venda.\nErro: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private Integer getClienteID(Connection con, String nomeCliente) throws SQLException {
        if (nomeCliente == null || nomeCliente.trim().isEmpty()) {
            String sqlConsumidor = "SELECT ClienteID FROM Clientes WHERE NomeCliente IN ('Cliente Consumidor', 'Consumidor Final') LIMIT 1";
            try(PreparedStatement pst = con.prepareStatement(sqlConsumidor); ResultSet rs = pst.executeQuery()){
                if(rs.next()) return rs.getInt("ClienteID");
            }
            String sqlNovo = "INSERT INTO Clientes (NomeCliente) VALUES ('Cliente Consumidor')";
            try (PreparedStatement pstNovo = con.prepareStatement(sqlNovo, Statement.RETURN_GENERATED_KEYS)) {
                pstNovo.executeUpdate();
                ResultSet chaves = pstNovo.getGeneratedKeys();
                if (chaves.next()) return chaves.getInt(1);
            }
            return null;
        }
        
        String sqlBusca = "SELECT ClienteID FROM Clientes WHERE NomeCliente = ?";
        try (PreparedStatement pst = con.prepareStatement(sqlBusca)) {
            pst.setString(1, nomeCliente.trim());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt("ClienteID");
            else {
                String sqlNovo = "INSERT INTO Clientes (NomeCliente) VALUES (?)";
                try (PreparedStatement pstNovo = con.prepareStatement(sqlNovo, Statement.RETURN_GENERATED_KEYS)) {
                    pstNovo.setString(1, nomeCliente.trim());
                    pstNovo.executeUpdate();
                    ResultSet chaves = pstNovo.getGeneratedKeys();
                    if (chaves.next()) return chaves.getInt(1);
                    else throw new SQLException("Falha ao cadastrar cliente, nenhum ID obtido.");
                }
            }
        }
    }
    
    @FXML
    private void limparFormularioVenda() {
        cbProduto.setValue(null);
        cbProduto.getEditor().clear();
        cbTipo.getItems().clear();
        fpTamanhosDisponiveis.getChildren().clear();
        txtQuantidadeVendida.setText("1");

        txtValorVenda.clear();
        dpDataVenda.setValue(LocalDate.now());
        chkPago.setSelected(true);
        dpDataPrometida.setValue(null);
        dpDataPrometida.setDisable(true);
        if (cbMetodoPagamento.getItems() != null && !cbMetodoPagamento.getItems().isEmpty()) cbMetodoPagamento.getSelectionModel().selectFirst();
        txtDesconto.clear();
        txtNomeCliente.clear();
        atualizarResumoVenda();
        cbProduto.requestFocus();
    }
    
    private void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipoAlerta) {
        Alert alert = new Alert(tipoAlerta);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
