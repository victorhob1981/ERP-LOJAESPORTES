package erp.controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class CatalogoController {

    @FXML
    private BorderPane catalogoRoot;

    @FXML
    public void initialize() {
        try {
            Class<?> factoryClass = Class.forName("com.sincronizador.SincronizadorEmbeddedFactory");
            Method criar = factoryClass.getMethod("criar");
            Object page = criar.invoke(null);

            if (!(page instanceof Parent)) {
                throw new IllegalStateException("Factory do sincronizador nao retornou um Parent JavaFX.");
            }

            catalogoRoot.setCenter((Parent) page);
        } catch (Exception e) {
            catalogoRoot.setCenter(criarErro(e));
        }
    }

    private VBox criarErro(Exception e) {
        Label titulo = new Label("Nao foi possivel carregar o sincronizador de catalogo.");
        titulo.getStyleClass().add("page-title");

        Label subtitulo = new Label("Verifique se o sincronizador e suas dependencias estao no classpath do ERP.");
        subtitulo.getStyleClass().add("muted-label");

        TextArea detalhes = new TextArea(stackTrace(e));
        detalhes.setEditable(false);
        detalhes.setWrapText(false);
        detalhes.setPrefRowCount(16);

        VBox box = new VBox(12, titulo, subtitulo, detalhes);
        box.setStyle("-fx-padding: 25;");
        return box;
    }

    private String stackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
