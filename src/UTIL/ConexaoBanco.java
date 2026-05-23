package UTIL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoBanco {

    public static Connection conectar() throws SQLException {
        DatabaseConfig config = DatabaseConfig.load();
        try {
            return DriverManager.getConnection(config.buildJdbcUrl(), config.getUser(), config.getPassword());
        } catch (SQLException e) {
            throw new SQLException("Erro ao conectar no MySQL em " + config.describeTarget()
                    + " usando configuração " + config.describeSource()
                    + ". Detalhe: " + e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
        }
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
