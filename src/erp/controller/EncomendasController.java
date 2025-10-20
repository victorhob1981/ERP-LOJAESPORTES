package erp.controller;

import erp.model.EncomendaVO;
import UTIL.ConexaoBanco;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class EncomendasController implements Initializable {

   
    @FXML private TextField txtNomeCliente;
    @FXML private TextField txtClube;
    @FXML private TextField txtModelo;
    @FXML private ComboBox<String> cbTipo;
    @FXML private ComboBox<String> cbTamanho;
    @FXML private Button btnAdicionarEncomenda;
    @FXML private TableView<EncomendaVO> tblEncomendas;
    @FXML private TableColumn<EncomendaVO, String> colCliente;
    @FXML private TableColumn<EncomendaVO, String> colClube;
    @FXML private TableColumn<EncomendaVO, String> colModelo;
    @FXML private TableColumn<EncomendaVO, String> colTipo;
    @FXML private TableColumn<EncomendaVO, String> colTamanho;
    @FXML private TableColumn<EncomendaVO, LocalDate> colDataEncomenda;
    @FXML private TableColumn<EncomendaVO, String> colStatus;
    @FXML private Button btnCancelar;
    @FXML private Button btnMarcarEntregue;

   
    private final ObservableList<EncomendaVO> listaEncomendas = FXCollections.observableArrayList();
    private final ObservableList<String> tamanhosAdulto = FXCollections.observableArrayList("P", "M", "G", "GG", "2GG", "3GG", "4GG");
private final ObservableList<String> tamanhosInfantil = FXCollections.observableArrayList("16", "18", "20", "22", "24", "26", "28");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTabela();
        configurarComponentes();
        carregarEncomendas();
    }
    
    private void configurarComponentes() {
    // Popula o ComboBox de Tipo
    cbTipo.getItems().addAll("Masculina", "Feminina", "Infantil");

    // Adiciona o listener para trocar os tamanhos
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

    // Lógica dos botões
    btnCancelar.setDisable(true);
    btnMarcarEntregue.setDisable(true);

    tblEncomendas.getSelectionModel().selectedItemProperty().addListener((_obs, _, newVal) -> {
        boolean selecionado = newVal != null;
        btnCancelar.setDisable(!selecionado);
        btnMarcarEntregue.setDisable(!selecionado);
    });
}

    private void configurarTabela() {
        tblEncomendas.setItems(listaEncomendas);
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nomeCliente"));
        colClube.setCellValueFactory(new PropertyValueFactory<>("clube"));
        colModelo.setCellValueFactory(new PropertyValueFactory<>("modelo"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTamanho.setCellValueFactory(new PropertyValueFactory<>("tamanho"));
        colDataEncomenda.setCellValueFactory(new PropertyValueFactory<>("dataEncomenda"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void carregarEncomendas() {
        listaEncomendas.clear();
        String sql = "SELECT E.EncomendaClienteID, C.NomeCliente, E.Clube, E.Modelo, E.Tipo, E.Tamanho, E.DataEncomenda, E.StatusEncomenda " +
                     "FROM EncomendasCliente E JOIN Clientes C ON E.ClienteID = C.ClienteID " +
                     "WHERE E.StatusEncomenda NOT IN ('EntregueAoCliente', 'Cancelada') " +
                     "ORDER BY E.DataEncomenda ASC";

        try (Connection con = ConexaoBanco.conectar();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            
            while(rs.next()) {
                listaEncomendas.add(new EncomendaVO(
                    rs.getInt("EncomendaClienteID"),
                    rs.getString("NomeCliente"),
                    rs.getString("Clube"),
                    rs.getString("Modelo"),
                    rs.getString("Tipo"),
                    rs.getString("Tamanho"),
                    rs.getDate("DataEncomenda").toLocalDate(),
                    rs.getString("StatusEncomenda")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Erro de Banco de Dados", "Não foi possível carregar as encomendas.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void adicionarEncomenda() {
        if (txtNomeCliente.getText().trim().isEmpty() || txtModelo.getText().trim().isEmpty() ||
            txtClube.getText().trim().isEmpty() || cbTipo.getValue() == null || cbTamanho.getValue() == null) {
            mostrarAlerta("Erro", "Todos os campos da encomenda são obrigatórios.", Alert.AlertType.ERROR);
            return;
        }

        Connection con = null;
        try {
            con = ConexaoBanco.conectar();
            con.setAutoCommit(false);

            Integer clienteId = getClienteID(con, txtNomeCliente.getText().trim());

            String sql = "INSERT INTO EncomendasCliente (ClienteID, DataEncomenda, StatusEncomenda, Clube, Modelo, Tipo, Tamanho) " +
                         "VALUES (?, CURDATE(), 'Pendente', ?, ?, ?, ?)";
            
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, clienteId);
               pst.setString(2, txtClube.getText().trim().toUpperCase()); // Adicionado .toUpperCase() 
                pst.setString(3, txtModelo.getText().trim());
                pst.setString(4, cbTipo.getValue());
                pst.setString(5, cbTamanho.getValue());
                pst.executeUpdate();
            }

            con.commit(); 
            mostrarAlerta("Sucesso", "Nova encomenda registrada!", Alert.AlertType.INFORMATION);
            limparFormulario();
            carregarEncomendas();

        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            mostrarAlerta("Erro de Banco de Dados", "Não foi possível registrar a encomenda.", Alert.AlertType.ERROR);
        } finally {
            if (con != null) try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private Integer getClienteID(Connection con, String nomeCliente) throws SQLException {
        if (nomeCliente == null || nomeCliente.trim().isEmpty()) {
            throw new SQLException("Nome do cliente não pode ser vazio para uma encomenda.");
        }
        String sqlBusca = "SELECT ClienteID FROM Clientes WHERE NomeCliente = ?";
        try (PreparedStatement pst = con.prepareStatement(sqlBusca)) {
            pst.setString(1, nomeCliente);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt("ClienteID");
            } else {
                String sqlNovo = "INSERT INTO Clientes (NomeCliente) VALUES (?)";
                try (PreparedStatement pstNovo = con.prepareStatement(sqlNovo, Statement.RETURN_GENERATED_KEYS)) {
                    pstNovo.setString(1, nomeCliente);
                    pstNovo.executeUpdate();
                    ResultSet chaves = pstNovo.getGeneratedKeys();
                    if (chaves.next()) {
                        return chaves.getInt(1);
                    } else {
                        throw new SQLException("Falha ao cadastrar novo cliente.");
                    }
                }
            }
        }
    }

    @FXML
    private void marcarComoEntregue() {
        EncomendaVO selecionada = tblEncomendas.getSelectionModel().getSelectedItem();
        if (selecionada == null) return;
        atualizarStatusEncomenda(selecionada.getEncomendaId(), "EntregueAoCliente", "Encomenda marcada como entregue.");
    }

    @FXML
    private void cancelarEncomenda() {
        EncomendaVO selecionada = tblEncomendas.getSelectionModel().getSelectedItem();
        if (selecionada == null) return;
        
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar Cancelamento");
        confirmacao.setHeaderText("Deseja realmente cancelar a encomenda de '" + selecionada.getModelo() + "' para o cliente " + selecionada.getNomeCliente() + "?");
        Optional<ButtonType> resultado = confirmacao.showAndWait();

        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            atualizarStatusEncomenda(selecionada.getEncomendaId(), "Cancelada", "Encomenda cancelada com sucesso.");
        }
    }
    
    private void atualizarStatusEncomenda(int encomendaId, String novoStatus, String msgSucesso) {
        String sql = "UPDATE EncomendasCliente SET StatusEncomenda = ? WHERE EncomendaClienteID = ?";
        try (Connection con = ConexaoBanco.conectar(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, novoStatus);
            pst.setInt(2, encomendaId);
            pst.executeUpdate();
            mostrarAlerta("Sucesso", msgSucesso, Alert.AlertType.INFORMATION);
            carregarEncomendas(); 
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível atualizar o status da encomenda.", Alert.AlertType.ERROR);
        }
    }

    private void limparFormulario() {
        txtNomeCliente.clear();
        txtClube.clear();
        txtModelo.clear();
        cbTipo.getSelectionModel().selectFirst();
        cbTamanho.getSelectionModel().selectFirst();
        txtNomeCliente.requestFocus();
    }
    
    private void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}