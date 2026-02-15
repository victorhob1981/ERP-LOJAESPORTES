package UTIL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoBanco {

    private static final String URL = getEnvOrDefault("ERP_DB_URL",
            "jdbc:mysql://localhost:3306/gemini_erp?allowPublicKeyRetrieval=true");
    private static final String USUARIO = getEnvOrDefault("ERP_DB_USER", "root");
    private static final String SENHA = getEnvOrDefault("ERP_DB_PASSWORD", "Senhalp3");

    private static String getEnvOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }

    public static void main(String[] args) {
        try (Connection conexao = conectar()) {
            if (conexao != null) {
                System.out.println("Conexão com o banco de dados estabelecida com sucesso!");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao conectar no banco: " + e.getMessage());
        }
    }
}
