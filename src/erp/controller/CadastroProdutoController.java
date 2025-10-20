package erp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDateTime;

import erp.model.ProdutoCadastro;  

public class CadastroProdutoController {

     
    @FXML private TextField txtModelo;
    @FXML private TextField txtClube;
    @FXML private ComboBox<String> cbTipo;          
    @FXML private ComboBox<String> cbTamanho;
    @FXML private TextField txtQuantidade;
    @FXML private TextField txtPrecoVenda;       
    @FXML private TextField txtCustoCompra;     

    @FXML private TableView<ProdutoCadastro> tblListaProdutos;
    @FXML private TableColumn<ProdutoCadastro, String> colModelo;
    @FXML private TableColumn<ProdutoCadastro, String> colClube;
    @FXML private TableColumn<ProdutoCadastro, String> colTipo; 
    @FXML private TableColumn<ProdutoCadastro, String> colTamanho;
    @FXML private TableColumn<ProdutoCadastro, Integer> colQuantidade;
    @FXML private TableColumn<ProdutoCadastro, Double> colPrecoVenda; 
    @FXML private TableColumn<ProdutoCadastro, Double> colCustoCompra;

    private ObservableList<ProdutoCadastro> listaProdutosTemporaria; 
    private final ObservableList<String> tamanhosAdulto = FXCollections.observableArrayList("P", "M", "G", "GG", "2GG", "3GG", "4GG");
private final ObservableList<String> tamanhosInfantil = FXCollections.observableArrayList("16", "18", "20", "22", "24", "26", "28");

    @FXML
public void initialize() {
    listaProdutosTemporaria = FXCollections.observableArrayList();
    tblListaProdutos.setItems(listaProdutosTemporaria);

    colModelo.setCellValueFactory(cellData -> cellData.getValue().modeloProperty());
    colClube.setCellValueFactory(cellData -> cellData.getValue().clubeProperty());
    colTipo.setCellValueFactory(cellData -> cellData.getValue().tipoProperty());
    colTamanho.setCellValueFactory(cellData -> cellData.getValue().tamanhoProperty());
    colQuantidade.setCellValueFactory(cellData -> cellData.getValue().quantidadeProperty().asObject());
    colPrecoVenda.setCellValueFactory(cellData -> cellData.getValue().precoVendaProperty().asObject());
    colCustoCompra.setCellValueFactory(cellData -> cellData.getValue().custoCompraProperty().asObject());

    // Configuração dos ComboBoxes
    cbTipo.getItems().addAll("Masculina", "Feminina", "Infantil");

    // Listener para trocar os tamanhos dinamicamente
    cbTipo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
        if ("Infantil".equals(newValue)) {
            cbTamanho.setItems(tamanhosInfantil);
        } else {
            cbTamanho.setItems(tamanhosAdulto);
        }
        cbTamanho.getSelectionModel().selectFirst();
    });

    // Define os valores iniciais
    cbTipo.getSelectionModel().selectFirst();
    cbTamanho.setItems(tamanhosAdulto);
    if (!tamanhosAdulto.isEmpty()) {
        cbTamanho.getSelectionModel().selectFirst();
    }
}

    @FXML
    public void adicionarProdutoNaLista() { 
        String modelo = txtModelo.getText().trim();
        String clube = txtClube.getText().trim();
        String tipo = cbTipo.getValue();
        String tamanho = cbTamanho.getValue();
        
       
        if (modelo.isEmpty() || clube.isEmpty() || tipo == null || tamanho == null ||
            txtQuantidade.getText().trim().isEmpty() || 
            txtPrecoVenda.getText().trim().isEmpty() || 
            txtCustoCompra.getText().trim().isEmpty()) {
            mostrarAlerta("Erro de Validação", "Todos os campos são obrigatórios.", Alert.AlertType.ERROR);
            return;
        }

        try {
            int quantidade = Integer.parseInt(txtQuantidade.getText().trim());
            double precoVenda = Double.parseDouble(txtPrecoVenda.getText().trim().replace(",","."));
            double custoCompra = Double.parseDouble(txtCustoCompra.getText().trim().replace(",","."));

            if (quantidade <= 0 || precoVenda <= 0 || custoCompra <= 0) {
                mostrarAlerta("Erro de Validação", "Quantidade, Preço de Venda e Custo de Compra devem ser maiores que zero.", Alert.AlertType.ERROR);
                return;
            }

            ProdutoCadastro produto = new ProdutoCadastro(modelo, clube, tipo, tamanho, quantidade, precoVenda, custoCompra);
            listaProdutosTemporaria.add(produto);

            limparFormulario();
        } catch (NumberFormatException e) {
            mostrarAlerta("Erro de Formato", "Quantidade, Preço ou Custo inválidos. Use números válidos.", Alert.AlertType.ERROR);
        }
    }

    private void limparFormulario() {
        txtModelo.clear();
        txtClube.clear();
        
        if (!cbTipo.getItems().isEmpty()) cbTipo.getSelectionModel().selectFirst();
        if (!cbTamanho.getItems().isEmpty()) cbTamanho.getSelectionModel().selectFirst();
        txtQuantidade.clear();
        txtPrecoVenda.clear();
        txtCustoCompra.clear();
        txtModelo.requestFocus(); 
    }

    @FXML
    public void salvarProdutosNoBanco() { 
        if (listaProdutosTemporaria.isEmpty()) {
            mostrarAlerta("Informação", "Nenhum produto na lista para salvar.", Alert.AlertType.INFORMATION);
            return;
        }

        Connection con = null;
        try {
            con = UTIL.ConexaoBanco.conectar();
            con.setAutoCommit(false); 

            String sqlSelect = "SELECT ProdutoID, QuantidadeEstoque, CustoMedioPonderado FROM Produtos WHERE Modelo = ? AND Clube = ? AND Tipo = ? AND Tamanho = ?";
            String sqlInsert = "INSERT INTO Produtos (Modelo, Clube, Tipo, Tamanho, DescricaoCompleta, PrecoVendaAtual, QuantidadeEstoque, CustoMedioPonderado, DataUltimaEntradaEstoque) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String sqlUpdate = "UPDATE Produtos SET PrecoVendaAtual = ?, QuantidadeEstoque = ?, CustoMedioPonderado = ?, DataUltimaEntradaEstoque = ? WHERE ProdutoID = ?";

            for (ProdutoCadastro prodCadastro : listaProdutosTemporaria) {
                String descricaoCompleta = String.format("%s %s %s %s", prodCadastro.getModelo(), prodCadastro.getClube(), prodCadastro.getTipo(), prodCadastro.getTamanho());
                Timestamp dataEntrada = Timestamp.valueOf(LocalDateTime.now());

                try (PreparedStatement pstSelect = con.prepareStatement(sqlSelect)) {
                    pstSelect.setString(1, prodCadastro.getModelo());
                    pstSelect.setString(2, prodCadastro.getClube());
                    pstSelect.setString(3, prodCadastro.getTipo());
                    pstSelect.setString(4, prodCadastro.getTamanho());
                    ResultSet rs = pstSelect.executeQuery();

                    if (rs.next()) { 
                        int produtoIDExistente = rs.getInt("ProdutoID");
                        int estoqueAntigo = rs.getInt("QuantidadeEstoque");
                        double custoMedioAntigo = rs.getDouble("CustoMedioPonderado");

                        int quantidadeNovaNoLote = prodCadastro.getQuantidade();
                        double custoNovoLote = prodCadastro.getCustoCompra();
                        
                        int novoEstoqueTotal = estoqueAntigo + quantidadeNovaNoLote;
                        double novoCustoMedioPonderado;

                        if (novoEstoqueTotal == 0) { 
                            novoCustoMedioPonderado = custoNovoLote; 
                        } else if (estoqueAntigo == 0) {
                             novoCustoMedioPonderado = custoNovoLote;
                        }
                        else {
                            novoCustoMedioPonderado = ((estoqueAntigo * custoMedioAntigo) + (quantidadeNovaNoLote * custoNovoLote)) / novoEstoqueTotal;
                        }
                        
                        try (PreparedStatement pstUpdate = con.prepareStatement(sqlUpdate)) {
                            pstUpdate.setDouble(1, prodCadastro.getPrecoVenda()); 
                            pstUpdate.setInt(2, novoEstoqueTotal);
                            pstUpdate.setDouble(3, novoCustoMedioPonderado);
                            pstUpdate.setTimestamp(4, dataEntrada);
                            pstUpdate.setInt(5, produtoIDExistente);
                            pstUpdate.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement pstInsert = con.prepareStatement(sqlInsert)) {
                            pstInsert.setString(1, prodCadastro.getModelo());
                            pstInsert.setString(2, prodCadastro.getClube());
                            pstInsert.setString(3, prodCadastro.getTipo());
                            pstInsert.setString(4, prodCadastro.getTamanho());
                            pstInsert.setString(5, descricaoCompleta);
                            pstInsert.setDouble(6, prodCadastro.getPrecoVenda());
                            pstInsert.setInt(7, prodCadastro.getQuantidade());
                            pstInsert.setDouble(8, prodCadastro.getCustoCompra()); 
                            pstInsert.setTimestamp(9, dataEntrada);
                            pstInsert.executeUpdate();
                        }
                    }
                    rs.close();
                }
            }
            con.commit(); 
            listaProdutosTemporaria.clear(); 
            mostrarAlerta("Sucesso", "Produtos salvos no banco de dados com sucesso!", Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback(); 
                } catch (SQLException ex) {
                    System.err.println("Erro ao fazer rollback: " + ex.getMessage());
                }
            }
            mostrarAlerta("Erro de Banco", "Erro ao salvar produtos: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    System.err.println("Erro ao fechar conexão: " + e.getMessage());
                }
            }
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