package UTIL;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    public static final String ARG_CONFIG_PROPERTY = "erp.config.file";
    public static final String ENV_CONFIG_FILE = "ERP_CONFIG_FILE";

    private static final Path PROGRAM_DATA_CONFIG = Paths.get(
            defaultProgramDataDirectory(),
            "ERP 2.0",
            "config",
            "application.properties");
    private static final Path DEV_CONFIG = Paths.get("config", "application-dev.properties");

    private static DatabaseConfig current;

    private final String host;
    private final int port;
    private final String databaseName;
    private final String user;
    private final String password;
    private final boolean useSSL;
    private final Path sourcePath;

    private DatabaseConfig(Properties properties, Path sourcePath) {
        this.host = property(properties, "db.host", "localhost");
        this.port = parsePort(property(properties, "db.port", "3306"));
        this.databaseName = property(properties, "db.name", "gemini_teste");
        this.user = property(properties, "db.user", "root");
        this.password = property(properties, "db.password", "");
        this.useSSL = Boolean.parseBoolean(property(properties, "db.useSSL", "false"));
        this.sourcePath = sourcePath;
    }

    public static DatabaseConfig fromValues(String host, int port, String databaseName, String user, String password,
            boolean useSSL) {
        Properties properties = defaultProperties();
        properties.setProperty("db.host", host);
        properties.setProperty("db.port", String.valueOf(port));
        properties.setProperty("db.name", databaseName);
        properties.setProperty("db.user", user);
        properties.setProperty("db.password", password);
        properties.setProperty("db.useSSL", String.valueOf(useSSL));
        return new DatabaseConfig(properties, load().getSourcePath());
    }

    public static void configureFromArgs(String[] args) {
        String configPath = findConfigArgument(args);
        if (configPath != null && !configPath.isBlank()) {
            System.setProperty(ARG_CONFIG_PROPERTY, configPath);
            current = null;
        }
    }

    public static synchronized DatabaseConfig load() {
        if (current == null) {
            current = loadFromResolvedPath();
        }
        return current;
    }

    public static synchronized void reload() {
        current = loadFromResolvedPath();
    }

    public String buildJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                + "?useSSL=" + useSSL
                + "&allowPublicKeyRetrieval=true";
    }

    public String describeTarget() {
        return host + ":" + port + "/" + databaseName;
    }

    public String describeSource() {
        if (sourcePath == null) {
            return "valores padrão";
        }
        return sourcePath.toAbsolutePath().toString();
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public void testConnection() throws SQLException {
        try (Connection ignored = DriverManager.getConnection(buildJdbcUrl(), user, password)) {
            // Apenas valida a conexão.
        }
    }

    public synchronized void save() throws IOException {
        Path path = sourcePath != null ? sourcePath : resolveConfigPath();
        if (path == null) {
            path = DEV_CONFIG;
        }

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (OutputStream output = Files.newOutputStream(path)) {
            toProperties().store(output, "ERP 2.0 database configuration");
        }
        current = new DatabaseConfig(toProperties(), path);
    }

    private Properties toProperties() {
        Properties properties = new Properties();
        properties.setProperty("db.host", host);
        properties.setProperty("db.port", String.valueOf(port));
        properties.setProperty("db.name", databaseName);
        properties.setProperty("db.user", user);
        properties.setProperty("db.password", password);
        properties.setProperty("db.useSSL", String.valueOf(useSSL));
        return properties;
    }

    private static DatabaseConfig loadFromResolvedPath() {
        Path path = resolveConfigPath();
        Properties properties = defaultProperties();

        if (path != null && Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException e) {
                throw new IllegalStateException("Não foi possível ler a configuração do banco em "
                        + path.toAbsolutePath(), e);
            }
        }

        return new DatabaseConfig(properties, path);
    }

    private static Path resolveConfigPath() {
        Path argumentPath = configuredPath(System.getProperty(ARG_CONFIG_PROPERTY));
        if (argumentPath != null) {
            createTemplateIfMissing(argumentPath);
            return argumentPath;
        }

        Path environmentPath = configuredPath(System.getenv(ENV_CONFIG_FILE));
        if (environmentPath != null) {
            createTemplateIfMissing(environmentPath);
            return environmentPath;
        }

        if (Files.exists(PROGRAM_DATA_CONFIG)) {
            return PROGRAM_DATA_CONFIG;
        }

        if (Files.exists(DEV_CONFIG)) {
            return DEV_CONFIG;
        }

        if (createTemplateIfMissing(PROGRAM_DATA_CONFIG)) {
            return PROGRAM_DATA_CONFIG;
        }

        if (createTemplateIfMissing(DEV_CONFIG)) {
            return DEV_CONFIG;
        }

        return null;
    }

    private static Path configuredPath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Paths.get(value.trim());
    }

    private static boolean createTemplateIfMissing(Path path) {
        if (Files.exists(path)) {
            return true;
        }

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (OutputStream output = Files.newOutputStream(path)) {
                defaultProperties().store(output, "ERP 2.0 database configuration");
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static Properties defaultProperties() {
        Properties properties = new Properties();
        properties.setProperty("db.host", "localhost");
        properties.setProperty("db.port", "3306");
        properties.setProperty("db.name", "gemini_teste");
        properties.setProperty("db.user", "root");
        properties.setProperty("db.password", "");
        properties.setProperty("db.useSSL", "false");
        return properties;
    }

    private static String property(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Porta do banco inválida em db.port: " + value, e);
        }
    }

    private static String findConfigArgument(String[] args) {
        if (args == null) {
            return null;
        }

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null) {
                continue;
            }
            if (arg.startsWith("--config=")) {
                return arg.substring("--config=".length());
            }
            if ("--config".equals(arg) && i + 1 < args.length) {
                return args[i + 1];
            }
        }

        return null;
    }

    private static String defaultProgramDataDirectory() {
        String programData = System.getenv("ProgramData");
        if (programData == null || programData.isBlank()) {
            return "C:\\ProgramData";
        }
        return programData;
    }
}
