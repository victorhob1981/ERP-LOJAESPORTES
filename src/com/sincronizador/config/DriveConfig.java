package com.sincronizador.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.List;

public final class DriveConfig {

    private static final String APPLICATION_NAME = "Sincronizador Catalogo";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final Path DEV_GOOGLE_DIR = Paths.get("config", "google");
    private static final Path PROGRAM_DATA_GOOGLE_DIR = Paths.get(
            defaultProgramDataDirectory(),
            "ERP 2.0",
            "google"
    );

    // salva o token aqui (pra não logar toda vez)
    private static final java.io.File TOKENS_DIR = resolverTokensDir().toFile();

    private DriveConfig() {}

    public static Drive criarDrive() {
        try {
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            GoogleClientSecrets clientSecrets = carregarClientSecrets();
            GoogleAuthorizationCodeFlow flow = criarFlow(httpTransport, clientSecrets);
            Credential credential = autorizar(flow);

            try {
                validarCredencial(credential);
            } catch (TokenResponseException e) {
                if (!ehTokenRevogadoOuExpirado(e)) {
                    throw e;
                }
                limparTokens();
                flow = criarFlow(httpTransport, clientSecrets);
                credential = autorizar(flow);
            }

            return new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

        } catch (GeneralSecurityException | java.io.IOException e) {
            throw new RuntimeException("Erro ao autenticar no Google Drive via OAuth", e);
        }
    }

    private static GoogleClientSecrets carregarClientSecrets() throws IOException {
        return GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(new FileInputStream(resolverCredentialsPath().toFile()))
        );
    }

    private static GoogleAuthorizationCodeFlow criarFlow(
            HttpTransport httpTransport,
            GoogleClientSecrets clientSecrets
    ) throws IOException {
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientSecrets,
                List.of(DriveScopes.DRIVE)
        )
                .setDataStoreFactory(new FileDataStoreFactory(TOKENS_DIR))
                .setAccessType("offline")
                .build();
    }

    private static Credential autorizar(GoogleAuthorizationCodeFlow flow) throws IOException {
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        return new AuthorizationCodeInstalledApp(flow, receiver)
                .authorize("user");
    }

    private static void validarCredencial(Credential credential) throws IOException {
        if (credential.getRefreshToken() != null) {
            credential.refreshToken();
            return;
        }

        Long expiraEm = credential.getExpiresInSeconds();
        if (credential.getAccessToken() == null || expiraEm == null || expiraEm <= 60) {
            credential.refreshToken();
        }
    }

    private static boolean ehTokenRevogadoOuExpirado(TokenResponseException e) {
        return e.getDetails() != null
                && "invalid_grant".equals(e.getDetails().getError());
    }

    private static void limparTokens() throws IOException {
        Path tokensPath = TOKENS_DIR.toPath();
        if (!Files.exists(tokensPath)) return;

        try (var paths = Files.walk(tokensPath)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static Path resolverCredentialsPath() {
        String configured = System.getProperty("sincronizador.credentials.json");
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured.trim());
        }

        Path dev = DEV_GOOGLE_DIR.resolve("credentials.json");
        if (Files.exists(dev)) return dev;

        Path programData = PROGRAM_DATA_GOOGLE_DIR.resolve("credentials.json");
        if (Files.exists(programData)) return programData;

        return Paths.get("credentials.json");
    }

    private static Path resolverTokensDir() {
        String configured = System.getProperty("sincronizador.tokens");
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured.trim());
        }

        Path dev = DEV_GOOGLE_DIR.resolve("tokens");
        if (Files.exists(dev)) return dev;

        return PROGRAM_DATA_GOOGLE_DIR.resolve("tokens");
    }

    private static String defaultProgramDataDirectory() {
        String programData = System.getenv("ProgramData");
        if (programData == null || programData.isBlank()) {
            return "C:\\ProgramData";
        }
        return programData;
    }
}
