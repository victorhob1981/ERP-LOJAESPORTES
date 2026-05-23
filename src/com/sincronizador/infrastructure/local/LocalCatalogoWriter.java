package com.sincronizador.infrastructure.local;

import com.sincronizador.application.port.CatalogoLocalWriter;
import com.sincronizador.domain.model.Disponibilidade;
import com.sincronizador.domain.model.SKU;
import com.sincronizador.domain.service.GeradorDeLegenda;
import com.sincronizador.infrastructure.util.Md5Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

public class LocalCatalogoWriter implements CatalogoLocalWriter {

    private static final String INDEX_FILE = "./data/catalogo/catalogo-local.properties";

    private final Path catalogoDir;
    private final Path indexFile;
    private final Properties index = new Properties();

    public LocalCatalogoWriter(Path catalogoDir) {
        this(catalogoDir, Path.of(INDEX_FILE));
    }

    public LocalCatalogoWriter(Path catalogoDir, Path indexFile) {
        this.catalogoDir = Objects.requireNonNull(catalogoDir, "catalogoDir nao pode ser nulo");
        this.indexFile = Objects.requireNonNull(indexFile, "indexFile nao pode ser nulo");
    }

    @Override
    public synchronized boolean publicar(SKU sku, Disponibilidade disponibilidade, File imagemLocal) {
        Objects.requireNonNull(sku, "sku nao pode ser nulo");
        Objects.requireNonNull(disponibilidade, "disponibilidade nao pode ser nula");
        validarImagem(imagemLocal);
        preparar();

        String key = skuKey(sku);
        String fileName = montarNomeArquivo(key, disponibilidade, imagemLocal);
        Path destino = catalogoDir.resolve(fileName);

        boolean mudou = copiarSeDiferente(imagemLocal.toPath(), destino);

        String antigo = index.getProperty(key);
        if (antigo != null && !antigo.isBlank() && !antigo.equals(fileName)) {
            excluirArquivoIndexado(antigo);
            mudou = true;
        }

        if (!fileName.equals(antigo)) {
            index.setProperty(key, fileName);
            persistirIndex();
        }

        return mudou;
    }

    @Override
    public synchronized boolean remover(SKU sku) {
        Objects.requireNonNull(sku, "sku nao pode ser nulo");
        preparar();

        String key = skuKey(sku);
        String fileName = index.getProperty(key);
        if (fileName == null || fileName.isBlank()) return false;

        boolean removeu = excluirArquivoIndexado(fileName);
        index.remove(key);
        persistirIndex();
        return removeu;
    }

    @Override
    public synchronized int removerAusentes(Set<SKU> skusAtivos) {
        preparar();

        Set<String> keysAtivas = new HashSet<>();
        if (skusAtivos != null) {
            for (SKU sku : skusAtivos) {
                if (sku != null) keysAtivas.add(skuKey(sku));
            }
        }

        int removidos = 0;
        for (String key : new HashSet<>(index.stringPropertyNames())) {
            if (keysAtivas.contains(key)) continue;

            String fileName = index.getProperty(key);
            if (fileName != null && !fileName.isBlank() && excluirArquivoIndexado(fileName)) {
                removidos++;
            }
            index.remove(key);
        }

        persistirIndex();
        return removidos;
    }

    private void preparar() {
        inicializar();
        carregarIndex();
    }

    private void inicializar() {
        try {
            Files.createDirectories(catalogoDir);
            Path parent = indexFile.getParent();
            if (parent != null) Files.createDirectories(parent);
            if (!Files.exists(indexFile)) Files.createFile(indexFile);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao inicializar catalogo local em: " + catalogoDir, e);
        }
    }

    private void carregarIndex() {
        try (InputStream in = Files.newInputStream(indexFile)) {
            index.clear();
            index.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao carregar indice do catalogo local", e);
        }
    }

    private void persistirIndex() {
        try (OutputStream out = Files.newOutputStream(indexFile, StandardOpenOption.TRUNCATE_EXISTING)) {
            index.store(out, "Mapeamento SKU -> arquivo no catalogo local");
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar indice do catalogo local", e);
        }
    }

    private boolean copiarSeDiferente(Path origem, Path destino) {
        try {
            if (Files.exists(destino) && Files.isRegularFile(destino)) {
                String md5Origem = Md5Utils.md5Hex(origem.toFile());
                String md5Destino = Md5Utils.md5Hex(destino.toFile());
                if (md5Origem.equalsIgnoreCase(md5Destino)) return false;
            }

            Files.copy(origem, destino, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao copiar imagem para catalogo local: " + destino, e);
        }
    }

    private boolean excluirArquivoIndexado(String fileName) {
        try {
            Path alvo = catalogoDir.resolve(fileName).normalize();
            if (!alvo.startsWith(catalogoDir.normalize())) {
                return false;
            }
            return Files.deleteIfExists(alvo);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao remover arquivo antigo do catalogo local: " + fileName, e);
        }
    }

    private String montarNomeArquivo(String key, Disponibilidade disponibilidade, File imagemLocal) {
        String legenda = GeradorDeLegenda.gerarLegenda(disponibilidade);
        String base = sanitizarNomeArquivo(legenda);
        String ext = extrairExtensao(imagemLocal.getName());

        int maxBase = 180 - ext.length();
        if (base.length() > maxBase) base = base.substring(0, maxBase).trim();
        if (base.isBlank()) base = "produto";

        String preferido = base + ext;
        if (!nomeUsadoPorOutroSku(key, preferido)) return preferido;

        String sufixo = " - " + slugCurto(key);
        int maxComSufixo = 180 - ext.length() - sufixo.length();
        String baseComSufixo = base;
        if (baseComSufixo.length() > maxComSufixo) {
            baseComSufixo = baseComSufixo.substring(0, maxComSufixo).trim();
        }

        return baseComSufixo + sufixo + ext;
    }

    private String sanitizarNomeArquivo(String nome) {
        if (nome == null) return "";
        String limpo = nome.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ").trim();
        limpo = limpo.replaceAll("\\s+", " ");
        return limpo;
    }

    private String extrairExtensao(String nome) {
        if (nome == null) return ".png";
        int idx = nome.lastIndexOf('.');
        if (idx < 0) return ".png";
        String ext = nome.substring(idx).toLowerCase(Locale.ROOT).trim();
        if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp")) return ext;
        return ".png";
    }

    private boolean nomeUsadoPorOutroSku(String keyAtual, String fileName) {
        for (String key : index.stringPropertyNames()) {
            if (key.equals(keyAtual)) continue;
            if (fileName.equals(index.getProperty(key))) return true;
        }
        return false;
    }

    private String slugCurto(String s) {
        if (s == null) return "produto";
        String x = s.replace("|", "_").trim();
        x = x.replaceAll("[^a-zA-Z0-9_\\- ]", "_");
        x = x.replaceAll("\\s+", "_");
        x = x.replaceAll("_+", "_");
        if (x.length() > 48) x = x.substring(0, 48);
        return x.isBlank() ? "produto" : x;
    }

    private void validarImagem(File arquivo) {
        Objects.requireNonNull(arquivo, "imagemLocal nao pode ser nula");
        if (!arquivo.exists() || !arquivo.isFile()) {
            throw new IllegalArgumentException("Imagem invalida: " + arquivo.getAbsolutePath());
        }
    }

    private String skuKey(SKU sku) {
        String clube = safeUpper(sku.getClube());
        String modelo = safeUpper(sku.getModelo());
        String tipo = sku.getTipo() == null ? "" : sku.getTipo().name().toUpperCase(Locale.ROOT);
        return clube + "|" + modelo + "|" + tipo;
    }

    private String safeUpper(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}
