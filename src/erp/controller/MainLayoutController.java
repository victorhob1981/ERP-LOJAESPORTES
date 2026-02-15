package erp.controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class MainLayoutController implements Initializable {

    @FXML
    private BorderPane mainBorderPane, contentArea;
    @FXML
    private MenuBar mainMenuBar;
    @FXML
    private VBox sidebar;
    @FXML
    private Button btnNavDashboard, btnNavRegistrarVenda, btnNavHistoricoVendas, btnNavGerenciarEstoque;
    @FXML
    private Button btnNavRegistrarPedido, btnNavAcompanhamento, btnNavEncomendas, btnNavFinanceiro;

    // --- MUDANÇA 1: Declarar o novo botão do FXML ---
    @FXML
    private Button btnNavRelatorioProdutos;

    private List<Button> navButtons;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        navButtons = Arrays.asList(btnNavDashboard, btnNavRegistrarVenda, btnNavHistoricoVendas, btnNavGerenciarEstoque,
                btnNavRegistrarPedido, btnNavAcompanhamento, btnNavEncomendas, btnNavFinanceiro,
                // --- MUDANÇA 2: Adicionar o novo botão à lista ---
                btnNavRelatorioProdutos);
        irParaDashboard(null);
    }

    public void loadPage(String fxmlFileName, Button activeButton) {
        try {
            String fxmlPath = "/erp/view/" + fxmlFileName;
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent pageRoot = loader.load();
            Object loadedController = loader.getController();
            if (loadedController instanceof TelaInicialController) {
                ((TelaInicialController) loadedController).setMainLayoutController(this);
            } else if (loadedController instanceof EstoqueController) {
                ((EstoqueController) loadedController).setMainLayoutController(this);
            }
            contentArea.setCenter(pageRoot);
            setActiveButton(activeButton);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlertaErroDetalhado(fxmlFileName, e);
        }
    }

    private void setActiveButton(Button activeButton) {
        for (Button btn : navButtons) {
            if (btn != null) { // Adicionado verificação de nulo por segurança
                btn.getStyleClass().remove("sidebar-button-active");
            }
        }
        if (activeButton != null) {
            activeButton.getStyleClass().add("sidebar-button-active");
        }
    }

    @FXML
    public void irParaDashboard(ActionEvent event) {
        loadPage("TelaInicialContent.fxml", btnNavDashboard);
    }

    @FXML
    public void irParaRegistrarVenda(ActionEvent event) {
        loadPage("TelaVendas.fxml", btnNavRegistrarVenda);
    }

    @FXML
    public void irParaHistoricoVendas(ActionEvent event) {
        loadPage("TelaHistoricoVendas.fxml", btnNavHistoricoVendas);
    }

    @FXML
    public void irParaGerenciarEstoque(ActionEvent event) {
        loadPage("TelaEstoque.fxml", btnNavGerenciarEstoque);
    }

    @FXML
    public void irParaAcompanhamento(ActionEvent event) {
        loadPage("TelaAcompanhamento.fxml", btnNavAcompanhamento);
    }

    @FXML
    public void irParaEncomendas(ActionEvent event) {
        loadPage("TelaEncomendas.fxml", btnNavEncomendas);
    }

    @FXML
    public void irParaFinanceiro(ActionEvent event) {
        loadPage("TelaFinanceiro.fxml", btnNavFinanceiro);
    }

    @FXML
    public void irParaRegistrarPedido(ActionEvent event) {
        loadPage("TelaRegistrarPedido.fxml", btnNavRegistrarPedido);
    }

    @FXML
    private void acaoSair(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    private void mostrarSobre(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sobre");
        alert.setHeaderText("ERP 2.0 - Sistema de Gestão de Loja Esportiva");
        alert.setContentText("Desenvolvido por Victor Hugo de Oliveira Barbosa\nJoão Carlos\n" +
                "Versão: 2.0.0");
        alert.showAndWait();
    }

    private void mostrarAlertaErroDetalhado(String fxmlFile, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro Crítico de Carregamento");
        alert.setHeaderText("Falha ao carregar a interface '" + fxmlFile + "'");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        VBox dialogPaneContent = new VBox();
        Label label = new Label("Detalhes técnicos do erro:");
        dialogPaneContent.getChildren().addAll(label, textArea);

        alert.getDialogPane().setExpandableContent(dialogPaneContent);
        alert.getDialogPane().setExpanded(true);
        alert.showAndWait();
    }

    // --- MUDANÇA 3: Corrigir a chamada para passar o botão correto ---
    @FXML
    public void irParaRelatorioProdutos(ActionEvent event) {
        loadPage("RelatorioProdutos.fxml", btnNavRelatorioProdutos);
    }
}


