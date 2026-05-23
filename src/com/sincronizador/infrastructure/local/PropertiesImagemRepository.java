package com.sincronizador.infrastructure.local;

import com.sincronizador.application.port.ImagemRepository;
import com.sincronizador.domain.model.SKU;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

public class PropertiesImagemRepository implements ImagemRepository {

    private final Path baseDir;
    private final Path imagesDir;
    private final Path propsFile;
    private final Properties props = new Properties();

    public PropertiesImagemRepository() {
        this(Paths.get("./data/catalogo"));
    }

    public PropertiesImagemRepository(Path baseDir) {
        this.baseDir = baseDir;
        this.imagesDir = baseDir.resolve("imagens");
        this.propsFile = baseDir.resolve("imagens.properties");
        inicializar();
        carregar();
    }

    private void inicializar() {
        try {
            Files.createDirectories(imagesDir);
            if (!Files.exists(propsFile)) {
                Files.createDirectories(propsFile.getParent());
                Files.createFile(propsFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao inicializar storage local de imagens", e);
        }
    }

    private synchronized void carregar() {
        try (InputStream in = Files.newInputStream(propsFile)) {
            props.clear();
            props.load(in);
        } catch (IOException e) {
            // Se estiver vazio/novo, ok. Se der erro real, sobe.
            throw new RuntimeException("Falha ao carregar imagens.properties", e);
        }
    }

    private synchronized void persistir() {
        try (OutputStream out = Files.newOutputStream(propsFile, StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(out, "Mapeamento SKU -> arquivo de imagem local (gerado pelo app)");
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar imagens.properties", e);
        }
    }

    @Override
    public synchronized boolean possuiImagem(SKU sku) {
        String key = skuKey(sku);
        String fileName = props.getProperty(key);
        if (fileName == null || fileName.isBlank()) return false;

        Path p = imagesDir.resolve(fileName.trim());
        if (Files.exists(p) && Files.isRegularFile(p)) return true;

        // Se o arquivo sumiu, limpa a associação para não ficar “podre”
        props.remove(key);
        persistir();
        return false;
    }

    @Override
    public synchronized Optional<File> obterImagem(SKU sku) {
        String key = skuKey(sku);
        String fileName = props.getProperty(key);
        if (fileName == null || fileName.isBlank()) return Optional.empty();

        Path p = imagesDir.resolve(fileName.trim());
        if (!Files.exists(p) || !Files.isRegularFile(p)) return Optional.empty();

        return Optional.of(p.toFile());
    }

    @Override
    public synchronized File salvarAssociacao(SKU sku, File imagemOrigem) {
        if (sku == null) throw new IllegalArgumentException("SKU não pode ser nulo");
        if (imagemOrigem == null) throw new IllegalArgumentException("imagemOrigem não pode ser nula");
        if (!imagemOrigem.exists() || !imagemOrigem.isFile()) {
            throw new IllegalArgumentException("Imagem inválida: " + imagemOrigem);
        }

        String key = skuKey(sku);
        String ext = extrairExtensao(imagemOrigem.getName());
        String destinoNome = slug(key) + ext;

        Path destino = imagesDir.resolve(destinoNome);

        try {
            Files.copy(imagemOrigem.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao copiar imagem para o storage local", e);
        }

        props.setProperty(key, destinoNome);
        persistir();

        return destino.toFile();
    }

    @Override
    public synchronized void removerAssociacao(SKU sku) {
        String key = skuKey(sku);
        String fileName = props.getProperty(key);
        if (fileName != null && !fileName.isBlank()) {
            Path p = imagesDir.resolve(fileName.trim());
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
            }
        }
        props.remove(key);
        persistir();
    }

    private String skuKey(SKU sku) {
        // Chave estável (não depende de nome de arquivo)
        String clube = safeUpper(sku.getClube());
        String modelo = safeUpper(sku.getModelo());
        String tipo = (sku.getTipo() == null) ? "" : sku.getTipo().name().toUpperCase(Locale.ROOT);
        return clube + "|" + modelo + "|" + tipo;
    }

    private String safeUpper(String s) {
        return (s == null) ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private String extrairExtensao(String nome) {
        if (nome == null) return ".png";
        int idx = nome.lastIndexOf('.');
        if (idx < 0) return ".png";
        String ext = nome.substring(idx).toLowerCase(Locale.ROOT).trim();
        if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp")) return ext;
        return ".png";
    }

    private String slug(String s) {
        if (s == null) return "imagem";
        String x = s.replace("|", "_").trim();
        x = x.replaceAll("[^a-zA-Z0-9_\\- ]", "_");
        x = x.replaceAll("\\s+", "_");
        x = x.replaceAll("_+", "_");
        if (x.length() > 120) x = x.substring(0, 120);
        return x;
    }
}
