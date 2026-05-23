package erp.application;

import UTIL.DatabaseConfig;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class Main extends Application {

  @Override
public void start(Stage primaryStage) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/erp/view/MainLayout.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 750);

        primaryStage.setTitle("ERP 2.0 - Sistema de Gestão");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    } catch(Exception e) {
        e.printStackTrace();
        logStartupError(e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro ao iniciar");
        alert.setHeaderText("Não foi possível abrir o ERP 2.0");
        alert.setContentText("Detalhes salvos em: " + startupLogPath());
        alert.showAndWait();
        javafx.application.Platform.exit();
    }
}

    public static void main(String[] args) {
        DatabaseConfig.configureFromArgs(args);
        launch(args);
    }

    private static void logStartupError(Exception e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Files.writeString(startupLogPath(), sw.toString());
        } catch (Exception ignored) {
        }
    }

    private static Path startupLogPath() {
        return Path.of(System.getProperty("user.home"), "ERP-2.0-startup-error.log");
    }
}
