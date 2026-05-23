package erp.controller;

import erp.model.ProdutoAgregadoVO;
import erp.model.ProdutoAgregadoVO.DetalheTamanho;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;

public class TelaVendasController implements Initializable {

    private static final Comparator<String> tamanhoComparator;
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

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

    @FXML private ComboBox<ProdutoAgregadoVO> cbProduto;
    @FXML private ComboBox<String> cbTipo;
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
    @FXML private Button btnAdicionarItem;
    @FXML private Button btnRemoverItem;
    @FXML private TableView<ItemVendaCarrinho> tblItensVenda;
    @FXML private TableColumn<ItemVendaCarrinho, String> colItemProduto;
    @FXML private TableColumn<ItemVendaCarrinho, String> colItemTipo;
    @FXML private TableColumn<ItemVendaCarrinho, String> colItemTamanho;
    @FXML private TableColumn<ItemVendaCarrinho, Integer> colItemQuantidade;
    @FXML private TableColumn<ItemVendaCarrinho, Double> colItemPrecoUnitario;
    @FXML private TableColumn<ItemVendaCarrinho, Double> colItemTotal;

    private final ObservableList<ProdutoAgregadoVO> listaProdutosSugeridos = FXCollections.observableArrayList();
    private final ObservableList<ItemVendaCarrinho> itensCarrinho = FXCollections.observableArrayList();
    private ToggleGroup grupoTamanhos = new ToggleGroup();
    private ProdutoAgregadoVO produtoSelecionado;
    private DetalheTamanho tamanhoSelecionadoDetalhe;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarComboBoxProduto();
        configurarTabelaCarrinho();
        configurarListeners();
        cbMetodoPagamento.getItems().addAll("Pix", "Cartão de Crédito", "Dinheiro");
        limparFormularioVenda();
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

        cbProduto.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().length() < 3) {
                listaProdutosSugeridos.clear();
                cbProduto.hide();
            } else if (cbProduto.isFocused()
                    && (produtoSelecionado == null || !newValue.equals(produtoSelecionado.getDescricaoModelo()))) {
                buscarProdutosSugeridos(newValue);
            }
        });

        cbProduto.valueProperty().addListener((obs, oldValue, novoProduto) -> {
            produtoSelecionado = novoProduto;
            atualizarOpcoesDeTipo();
        });
    }

    private void configurarTabelaCarrinho() {
        tblItensVenda.setItems(itensCarrinho);
        colItemProduto.setCellValueFactory(new PropertyValueFactory<>("produto"));
        colItemTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colItemTamanho.setCellValueFactory(new PropertyValueFactory<>("tamanho"));
        colItemQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colItemPrecoUnitario.setCellValueFactory(new PropertyValueFactory<>("precoUnitario"));
        colItemTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colItemPrecoUnitario.setCellFactory(col -> formatarCelulaMoeda());
        colItemTotal.setCellFactory(col -> formatarCelulaMoeda());

        tblItensVenda.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> btnRemoverItem.setDisable(newSelection == null));
    }

    private TableCell<ItemVendaCarrinho, Double> formatarCelulaMoeda() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean empty) {
                super.updateItem(valor, empty);
                setText(empty || valor == null ? null : currencyFormat.format(valor));
            }
        };
    }

    private void buscarProdutosSugeridos(String textoBusca) {
        Map<String, ProdutoAgregadoVO> mapaProdutos = new HashMap<>();
        String sql = "SELECT ProdutoID, clube, modelo, tipo, tamanho, PrecoVendaAtual, CustoMedioPonderado, QuantidadeEstoque "
                + "FROM Produtos "
                + "WHERE (CONCAT(clube, ' ', modelo) LIKE ?) AND QuantidadeEstoque > 0 "
                + "ORDER BY clube, modelo, tipo, tamanho "
                + "LIMIT 50";

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
                        rs.getDouble("CustoMedioPonderado"));
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
        limparTamanhos();
        txtValorVenda.clear();
        tamanhoSelecionadoDetalhe = null;

        if (produtoSelecionado != null) {
            List<String> tipos = new ArrayList<>(produtoSelecionado.getTipos());
            cbTipo.setItems(FXCollections.observableArrayList(tipos));
            if (tipos.size() == 1) {
                cbTipo.getSelectionModel().selectFirst();
            }
        }
    }

    private void atualizarOpcoesDeTamanho() {
        limparTamanhos();
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
                });
                fpTamanhosDisponiveis.getChildren().add(btnTamanho);
            }
        }
    }

    private void limparTamanhos() {
        fpTamanhosDisponiveis.getChildren().clear();
        grupoTamanhos.getToggles().clear();
    }

    private void configurarListeners() {
        btnAdicionarItem.setOnAction(event -> adicionarItemVenda());
        btnRemoverItem.setOnAction(event -> removerItemSelecionado());
        btnSalvarVenda.setOnAction(event -> salvarVenda());

        cbTipo.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, novoTipo) -> {
            if (novoTipo != null) {
                atualizarOpcoesDeTamanho();
            }
        });

        txtDesconto.textProperty().addListener((obs, oldValue, newValue) -> atualizarResumoVenda());
        chkPago.setOnAction(event -> {
            dpDataPrometida.setDisable(chkPago.isSelected());
            if (chkPago.isSelected()) {
                dpDataPrometida.setValue(null);
            }
        });
    }

    @FXML
    private void adicionarItemVenda() {
        if (produtoSelecionado == null || cbTipo.getValue() == null || tamanhoSelecionadoDetalhe == null
                || txtQuantidadeVendida.getText().trim().isEmpty() || txtValorVenda.getText().trim().isEmpty()) {
            mostrarAlerta("Erro de Validação", "Produto, tipo, tamanho, quantidade e preço são obrigatórios para adicionar o item.", Alert.AlertType.ERROR);
            return;
        }

        int quantidadeVendida;
        double precoUnitarioVenda;
        try {
            quantidadeVendida = Integer.parseInt(txtQuantidadeVendida.getText().trim());
            precoUnitarioVenda = Double.parseDouble(txtValorVenda.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            mostrarAlerta("Erro de Formato", "Quantidade ou preço unitário inválido.", Alert.AlertType.ERROR);
            return;
        }

        if (quantidadeVendida <= 0) {
            mostrarAlerta("Erro de Validação", "A quantidade deve ser maior que zero.", Alert.AlertType.ERROR);
            return;
        }

        if (precoUnitarioVenda <= 0) {
            mostrarAlerta("Erro de Validação", "O preço unitário deve ser maior que zero.", Alert.AlertType.ERROR);
            return;
        }

        int quantidadeJaReservada = quantidadeReservadaNoCarrinho(tamanhoSelecionadoDetalhe.getProdutoId());
        if (quantidadeJaReservada + quantidadeVendida > tamanhoSelecionadoDetalhe.getEstoque()) {
            mostrarAlerta("Erro de Estoque",
                    "Estoque insuficiente. Disponível: " + tamanhoSelecionadoDetalhe.getEstoque()
                            + ". Já adicionado nesta venda: " + quantidadeJaReservada + ".",
                    Alert.AlertType.ERROR);
            return;
        }

        itensCarrinho.add(new ItemVendaCarrinho(
                produtoSelecionado.getDescricaoModelo(),
                cbTipo.getValue(),
                tamanhoSelecionadoDetalhe.getProdutoId(),
                obterTamanhoSelecionado(),
                quantidadeVendida,
                precoUnitarioVenda,
                tamanhoSelecionadoDetalhe.getCustoMedio()));

        limparItemAtual();
        atualizarResumoVenda();
    }

    private String obterTamanhoSelecionado() {
        ToggleButton selecionado = (ToggleButton) grupoTamanhos.getSelectedToggle();
        return selecionado == null ? "" : selecionado.getText();
    }

    private int quantidadeReservadaNoCarrinho(int produtoId) {
        return itensCarrinho.stream()
                .filter(item -> item.getProdutoId() == produtoId)
                .mapToInt(ItemVendaCarrinho::getQuantidade)
                .sum();
    }

    private void removerItemSelecionado() {
        ItemVendaCarrinho itemSelecionado = tblItensVenda.getSelectionModel().getSelectedItem();
        if (itemSelecionado != null) {
            itensCarrinho.remove(itemSelecionado);
            atualizarResumoVenda();
        }
    }

    private void atualizarResumoVenda() {
        try {
            double subtotal = calcularSubtotalCarrinho();
            double desconto = txtDesconto.getText().trim().isEmpty()
                    ? 0.0
                    : Double.parseDouble(txtDesconto.getText().trim().replace(",", "."));
            double totalAPagar = subtotal - desconto;
            lblSubtotalCalculado.setText(currencyFormat.format(subtotal));
            lblDescontoAplicado.setText(currencyFormat.format(desconto));
            lblTotalAPagar.setText(currencyFormat.format(Math.max(totalAPagar, 0.0)));
        } catch (NumberFormatException e) {
            lblSubtotalCalculado.setText("R$ 0,00");
            lblDescontoAplicado.setText("R$ 0,00");
            lblTotalAPagar.setText("R$ 0,00");
        }
    }

    private double calcularSubtotalCarrinho() {
        return itensCarrinho.stream()
                .mapToDouble(ItemVendaCarrinho::getTotal)
                .sum();
    }

    @FXML
    private void salvarVenda() {
        if (itensCarrinho.isEmpty()) {
            mostrarAlerta("Erro de Validação", "Adicione pelo menos um item antes de finalizar a venda.", Alert.AlertType.ERROR);
            return;
        }

        if (dpDataVenda.getValue() == null || cbMetodoPagamento.getValue() == null) {
            mostrarAlerta("Erro de Validação", "Data da venda e método de pagamento são obrigatórios.", Alert.AlertType.ERROR);
            return;
        }

        double valorDesconto = 0.0;
        try {
            if (!txtDesconto.getText().trim().isEmpty()) {
                valorDesconto = Double.parseDouble(txtDesconto.getText().trim().replace(",", "."));
            }
            if (valorDesconto < 0) {
                mostrarAlerta("Erro de Validação", "O desconto não pode ser negativo.", Alert.AlertType.ERROR);
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Erro de Formato", "O desconto é inválido.", Alert.AlertType.ERROR);
            return;
        }

        double valorTotalItens = calcularSubtotalCarrinho();
        if (valorDesconto > valorTotalItens) {
            mostrarAlerta("Erro de Validação", "O desconto não pode ser maior que o subtotal da venda.", Alert.AlertType.ERROR);
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

            String sqlVenda = "INSERT INTO Vendas (ClienteID, DataVenda, ValorTotalItens, ValorDesconto, ValorFinalVenda, StatusPagamento, DataPrometidaPagamento, MetodoPagamento) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            long vendaIdGerado;
            try (PreparedStatement pstVenda = con.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                if (clienteId != null) {
                    pstVenda.setInt(1, clienteId);
                } else {
                    pstVenda.setNull(1, java.sql.Types.INTEGER);
                }
                pstVenda.setDate(2, java.sql.Date.valueOf(dataVendaLocal));
                pstVenda.setDouble(3, valorTotalItens);
                pstVenda.setDouble(4, valorDesconto);
                pstVenda.setDouble(5, valorFinalVenda);
                pstVenda.setString(6, pago ? "Pago" : "Pendente");
                if (!pago && dataPrometidaLocal != null) {
                    pstVenda.setDate(7, java.sql.Date.valueOf(dataPrometidaLocal));
                } else {
                    pstVenda.setNull(7, java.sql.Types.DATE);
                }
                pstVenda.setString(8, metodoPagamento);
                pstVenda.executeUpdate();
                ResultSet rsKeys = pstVenda.getGeneratedKeys();
                if (rsKeys.next()) {
                    vendaIdGerado = rsKeys.getLong(1);
                } else {
                    throw new SQLException("Falha ao obter ID da venda.");
                }
            }

            inserirItensDaVenda(con, vendaIdGerado);

            con.commit();
            mostrarAlerta("Sucesso", "Venda registrada com sucesso! ID da Venda: " + vendaIdGerado, Alert.AlertType.INFORMATION);
            limparFormularioVenda();

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            mostrarAlerta("Erro de Banco de Dados", "Não foi possível salvar a venda.\nErro: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void inserirItensDaVenda(Connection con, long vendaIdGerado) throws SQLException {
        String sqlPreco = "UPDATE Produtos SET PrecoVendaAtual = ? WHERE ProdutoID = ? AND PrecoVendaAtual < ?";
        String sqlItemVenda = "INSERT INTO ItensVenda (VendaID, ProdutoID, Quantidade, PrecoVendaUnitarioRegistrado, CustoMedioUnitarioRegistrado) VALUES (?, ?, ?, ?, ?)";
        String sqlEstoque = "UPDATE Produtos SET QuantidadeEstoque = QuantidadeEstoque - ? WHERE ProdutoID = ? AND QuantidadeEstoque >= ?";

        try (PreparedStatement pstPreco = con.prepareStatement(sqlPreco);
                PreparedStatement pstItem = con.prepareStatement(sqlItemVenda);
                PreparedStatement pstEstoque = con.prepareStatement(sqlEstoque)) {

            for (ItemVendaCarrinho item : itensCarrinho) {
                pstPreco.setDouble(1, item.getPrecoUnitario());
                pstPreco.setInt(2, item.getProdutoId());
                pstPreco.setDouble(3, item.getPrecoUnitario());
                pstPreco.executeUpdate();

                pstItem.setLong(1, vendaIdGerado);
                pstItem.setInt(2, item.getProdutoId());
                pstItem.setInt(3, item.getQuantidade());
                pstItem.setDouble(4, item.getPrecoUnitario());
                pstItem.setDouble(5, item.getCustoMedio());
                pstItem.executeUpdate();

                pstEstoque.setInt(1, item.getQuantidade());
                pstEstoque.setInt(2, item.getProdutoId());
                pstEstoque.setInt(3, item.getQuantidade());
                int linhasAtualizadas = pstEstoque.executeUpdate();
                if (linhasAtualizadas == 0) {
                    throw new SQLException("Estoque insuficiente para o produto " + item.getProduto() + " - " + item.getTamanho() + ".");
                }
            }
        }
    }

    private Integer getClienteID(Connection con, String nomeCliente) throws SQLException {
        if (nomeCliente == null || nomeCliente.trim().isEmpty()) {
            String sqlConsumidor = "SELECT ClienteID FROM Clientes WHERE NomeCliente IN ('Cliente Consumidor', 'Consumidor Final') LIMIT 1";
            try (PreparedStatement pst = con.prepareStatement(sqlConsumidor);
                    ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ClienteID");
                }
            }
            String sqlNovo = "INSERT INTO Clientes (NomeCliente) VALUES ('Cliente Consumidor')";
            try (PreparedStatement pstNovo = con.prepareStatement(sqlNovo, Statement.RETURN_GENERATED_KEYS)) {
                pstNovo.executeUpdate();
                ResultSet chaves = pstNovo.getGeneratedKeys();
                if (chaves.next()) {
                    return chaves.getInt(1);
                }
            }
            return null;
        }

        String sqlBusca = "SELECT ClienteID FROM Clientes WHERE NomeCliente = ?";
        try (PreparedStatement pst = con.prepareStatement(sqlBusca)) {
            pst.setString(1, nomeCliente.trim());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt("ClienteID");
            } else {
                String sqlNovo = "INSERT INTO Clientes (NomeCliente) VALUES (?)";
                try (PreparedStatement pstNovo = con.prepareStatement(sqlNovo, Statement.RETURN_GENERATED_KEYS)) {
                    pstNovo.setString(1, nomeCliente.trim());
                    pstNovo.executeUpdate();
                    ResultSet chaves = pstNovo.getGeneratedKeys();
                    if (chaves.next()) {
                        return chaves.getInt(1);
                    } else {
                        throw new SQLException("Falha ao cadastrar cliente, nenhum ID obtido.");
                    }
                }
            }
        }
    }

    @FXML
    private void limparFormularioVenda() {
        itensCarrinho.clear();
        limparItemAtual();
        dpDataVenda.setValue(LocalDate.now());
        chkPago.setSelected(true);
        dpDataPrometida.setValue(null);
        dpDataPrometida.setDisable(true);
        if (cbMetodoPagamento.getItems() != null && !cbMetodoPagamento.getItems().isEmpty()) {
            cbMetodoPagamento.getSelectionModel().selectFirst();
        }
        txtDesconto.clear();
        txtNomeCliente.clear();
        atualizarResumoVenda();
        cbProduto.requestFocus();
    }

    private void limparItemAtual() {
        cbProduto.setValue(null);
        cbProduto.getEditor().clear();
        cbTipo.getItems().clear();
        limparTamanhos();
        txtQuantidadeVendida.setText("1");
        txtValorVenda.clear();
        produtoSelecionado = null;
        tamanhoSelecionadoDetalhe = null;
        cbProduto.requestFocus();
    }

    private void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipoAlerta) {
        Alert alert = new Alert(tipoAlerta);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    public static class ItemVendaCarrinho {
        private final String produto;
        private final String tipo;
        private final int produtoId;
        private final String tamanho;
        private final int quantidade;
        private final double precoUnitario;
        private final double custoMedio;

        public ItemVendaCarrinho(String produto, String tipo, int produtoId, String tamanho, int quantidade,
                double precoUnitario, double custoMedio) {
            this.produto = produto;
            this.tipo = tipo;
            this.produtoId = produtoId;
            this.tamanho = tamanho;
            this.quantidade = quantidade;
            this.precoUnitario = precoUnitario;
            this.custoMedio = custoMedio;
        }

        public String getProduto() {
            return produto;
        }

        public String getTipo() {
            return tipo;
        }

        public int getProdutoId() {
            return produtoId;
        }

        public String getTamanho() {
            return tamanho;
        }

        public int getQuantidade() {
            return quantidade;
        }

        public double getPrecoUnitario() {
            return precoUnitario;
        }

        public double getCustoMedio() {
            return custoMedio;
        }

        public double getTotal() {
            return quantidade * precoUnitario;
        }
    }
}
