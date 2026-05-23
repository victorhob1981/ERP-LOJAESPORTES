package com.sincronizador.interfaces.controller;

import com.sincronizador.application.dto.EstadoProdutoCatalogo;
import com.sincronizador.application.dto.ProdutoCatalogoStatusDTO;
import com.sincronizador.application.dto.ResultadoSincronizacaoDTO;
import com.sincronizador.application.port.ImagemRepository;
import com.sincronizador.application.usecase.AssociarImagemAoCatalogoUseCase;
import com.sincronizador.application.usecase.GerarStatusDoCatalogoUseCase;
import com.sincronizador.application.usecase.SincronizarCatalogoUseCase;
import com.sincronizador.domain.model.SKU;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainController {

    @FXML
    private TableView<ProdutoCatalogoStatusDTO> tabelaProdutos;

    @FXML
    private TableColumn<ProdutoCatalogoStatusDTO, String> colProduto;

    @FXML
    private TableColumn<ProdutoCatalogoStatusDTO, String> colStatus;

    @FXML
    private ProgressBar pbSync;

    @FXML
    private Label lblSync;

    @FXML
    private Button btnSincronizarCatalogo;

    @FXML
    private Button btnAssociarImagem;

    @FXML
    private Button btnTrocarImagem;

    // ====== NOVO (painel à direita) ======
    @FXML
    private ImageView imgPreview;

    @FXML
    private Label lblSemImagem;

    @FXML
    private Label lblProdutoSelecionado;

    @FXML
    private Label lblStatusSelecionado;

    @FXML
    private Label lblTamanhosSelecionado;

    @FXML
    private Label lblAcaoRecomendada;

    private GerarStatusDoCatalogoUseCase gerarStatusUseCase;
    private SincronizarCatalogoUseCase sincronizarCatalogoUseCase;
    private AssociarImagemAoCatalogoUseCase associarImagemUseCase;

    // necessário para buscar imagem local do SKU ao selecionar
    private ImagemRepository imagemRepository;

    private boolean busy = false;

    @FXML
    public void initialize() {

        colProduto.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getNomeProduto())
        );

        colStatus.setCellValueFactory(cell -> {
            EstadoProdutoCatalogo estado = cell.getValue().getEstado();
            String txt;
            if (estado == EstadoProdutoCatalogo.OK) {
                txt = "● OK";
            } else if (estado == EstadoProdutoCatalogo.SEM_IMAGEM) {
                txt = "● Sem imagem";
            } else if (estado == EstadoProdutoCatalogo.DESATUALIZADO) {
                txt = "● Desatualizado";
            } else {
                txt = "● Órfão";
            }
            return new javafx.beans.property.SimpleStringProperty(txt);
        });

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item);

                if (item.contains("OK")) {
                    setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                } else if (item.contains("Sem imagem")) {
                    setStyle("-fx-text-fill: #ef6c00; -fx-font-weight: bold;");
                } else if (item.contains("Desatualizado")) {
                    setStyle("-fx-text-fill: #f9a825; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #546e7a; -fx-font-weight: bold;");
                }
            }
        });

        // Progresso começa oculto
        ocultarProgresso();

        // Painel começa "vazio"
        limparPainelDetalhe();

        // Listener de seleção -> atualiza painel
        if (tabelaProdutos != null) {
            tabelaProdutos.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                if (!busy) atualizarEstadoBotoes();
                atualizarPainelDetalhe(newV);
            });
        }
    }

    public void setGerarStatusUseCase(GerarStatusDoCatalogoUseCase useCase) {
        this.gerarStatusUseCase = useCase;
        carregarTabela();
    }

    public void setSincronizarCatalogoUseCase(SincronizarCatalogoUseCase useCase) {
        this.sincronizarCatalogoUseCase = useCase;
    }

    public void setAssociarImagemUseCase(AssociarImagemAoCatalogoUseCase useCase) {
        this.associarImagemUseCase = useCase;
    }

    // injeta o repositório local de imagens para o painel
    public void setImagemRepository(ImagemRepository repo) {
        this.imagemRepository = repo;
        if (tabelaProdutos != null) {
            atualizarPainelDetalhe(tabelaProdutos.getSelectionModel().getSelectedItem());
        }
    }

    private void carregarTabela() {
        if (gerarStatusUseCase == null) return;

        ObservableList<ProdutoCatalogoStatusDTO> dados =
                FXCollections.observableArrayList(gerarStatusUseCase.executar());

        tabelaProdutos.setItems(dados);
        atualizarEstadoBotoes();

        ProdutoCatalogoStatusDTO sel = tabelaProdutos.getSelectionModel().getSelectedItem();
        atualizarPainelDetalhe(sel);
    }

    private void atualizarEstadoBotoes() {
        if (busy) return;

        ProdutoCatalogoStatusDTO selecionado = (tabelaProdutos == null)
                ? null
                : tabelaProdutos.getSelectionModel().getSelectedItem();

        boolean temSelecao = selecionado != null;
        boolean semImagem = temSelecao && selecionado.getEstado() == EstadoProdutoCatalogo.SEM_IMAGEM;
        boolean okOuOutro = temSelecao && !semImagem;

        if (btnAssociarImagem != null) btnAssociarImagem.setDisable(!temSelecao);
        if (btnTrocarImagem != null) btnTrocarImagem.setDisable(!okOuOutro);
    }

    private void setBusy(boolean busy) {
        this.busy = busy;

        if (btnSincronizarCatalogo != null) btnSincronizarCatalogo.setDisable(busy);
        if (btnAssociarImagem != null) btnAssociarImagem.setDisable(busy);
        if (btnTrocarImagem != null) btnTrocarImagem.setDisable(busy);

        if (tabelaProdutos != null) tabelaProdutos.setDisable(busy);
    }

    private <T> void runAsync(String tituloErro, Task<T> task, Consumer<T> onSuccess) {
        task.setOnSucceeded(e -> {
            try {
                if (onSuccess != null) onSuccess.accept(task.getValue());
            } finally {
                ocultarProgresso();
                setBusy(false);
                atualizarEstadoBotoes();
            }
        });

        task.setOnFailed(e -> {
            try {
                Throwable ex = task.getException();
                alertErro(tituloErro, ex == null ? "Erro desconhecido." : ex.getMessage());
            } finally {
                ocultarProgresso();
                setBusy(false);
                atualizarEstadoBotoes();
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // =========================================================
    // BOTÃO SINCRONIZAR (com fluxo guiado de pendências)
    // =========================================================

    @FXML
    private void onSincronizar() {
        if (sincronizarCatalogoUseCase == null) {
            alertErro("Sincronizar Catálogo", "UseCase de sincronização não foi injetado.");
            return;
        }
        if (associarImagemUseCase == null) {
            alertErro("Sincronizar Catálogo", "UseCase de associação de imagem não foi injetado.");
            return;
        }

        List<ProdutoCatalogoStatusDTO> pendentes = obterPendentesSemImagem();
        if (!pendentes.isEmpty()) {
            guiarPendenciasAntesDeSincronizar(new ArrayList<>(pendentes));
            return;
        }

        executarSincronizacaoAsync();
    }

    /**
     * ✅ CORREÇÃO:
     * SEM_IMAGEM na tabela significa "não existe no Drive ainda".
     * Pendência real para sincronizar é "não existe imagem LOCAL associada".
     */
    private List<ProdutoCatalogoStatusDTO> obterPendentesSemImagem() {
        if (tabelaProdutos == null || tabelaProdutos.getItems() == null) return List.of();

        // Se ainda não injetou o repo, mantém fallback antigo para não quebrar nada.
        if (imagemRepository == null) {
            return tabelaProdutos.getItems().stream()
                    .filter(p -> p.getEstado() == EstadoProdutoCatalogo.SEM_IMAGEM)
                    .collect(Collectors.toList());
        }

        return tabelaProdutos.getItems().stream()
                .filter(p -> {
                    try {
                        return imagemRepository.obterImagem(p.getSku()).isEmpty();
                    } catch (Exception e) {
                        // se deu erro ao ler, trata como pendente (segurança)
                        return true;
                    }
                })
                .collect(Collectors.toList());
    }

    private void guiarPendenciasAntesDeSincronizar(List<ProdutoCatalogoStatusDTO> pendentes) {
        if (pendentes == null || pendentes.isEmpty()) {
            executarSincronizacaoAsync();
            return;
        }

        ProdutoCatalogoStatusDTO atual = pendentes.get(0);

        ButtonType btSelecionar = new ButtonType("Selecionar imagem...");
        ButtonType btPular = new ButtonType("Pular");
        ButtonType btCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Pendência de imagem");
        alert.setHeaderText("Faltam " + pendentes.size() + " imagem(ns) para sincronizar");
        alert.setContentText("Produto:\n" + atual.getNomeProduto());
        alert.getButtonTypes().setAll(btSelecionar, btPular, btCancelar);

        Optional<ButtonType> escolha = alert.showAndWait();
        if (escolha.isEmpty() || escolha.get() == btCancelar) {
            return;
        }

        if (escolha.get() == btPular) {
            pendentes.remove(0);
            guiarPendenciasAntesDeSincronizar(pendentes);
            return;
        }

        File arquivo = abrirFileChooserImagem("Selecione a imagem do produto");
        if (arquivo == null) return;

        setBusy(true);

        SKU sku = atual.getSku();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                associarImagemUseCase.executar(sku, arquivo);
                return null;
            }
        };

        runAsync("Erro ao associar imagem", task, ignored -> {
            carregarTabela();

            pendentes.remove(0);

            if (!pendentes.isEmpty()) {
                guiarPendenciasAntesDeSincronizar(pendentes);
            } else {
                executarSincronizacaoAsync();
            }
        });
    }

    private void executarSincronizacaoAsync() {
        setBusy(true);

        Task<ResultadoSincronizacaoDTO> task = new Task<>() {
            @Override
            protected ResultadoSincronizacaoDTO call() {

                updateProgress(0, 1);
                updateMessage("0/1 - Preparando...");

                ResultadoSincronizacaoDTO r = sincronizarCatalogoUseCase.executar((atual, total, msg) -> {
                    int t = Math.max(total, 1);
                    int a = Math.min(Math.max(atual, 0), t);

                    updateProgress(a, t);

                    String texto = (msg == null || msg.isBlank()) ? "Processando..." : msg;
                    updateMessage(a + "/" + t + " - " + texto);
                });

                updateProgress(1, 1);
                updateMessage("1/1 - Finalizando...");

                return r;
            }
        };

        mostrarProgresso(task);

        runAsync("Erro ao sincronizar catálogo", task, r -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sincronizador de Catálogo");
            alert.setHeaderText("Sincronização concluída.");

            String msg =
                    "Criados: " + r.getCriados() + "\n" +
                            "Atualizados: " + r.getAtualizados() + "\n" +
                            "Removidos: " + r.getRemovidos() + "\n" +
                            "Pendentes (sem imagem local): " + r.getPendentesCriacaoSemimagem();

            if (r.temErros()) {
                msg += "\n\nErros (primeiros):\n" +
                        r.getErros().stream().limit(6).collect(Collectors.joining("\n"));
            }

            alert.setContentText(msg);
            alert.showAndWait();

            carregarTabela();
        });
    }

    // =========================================================
    // BOTÃO ASSOCIAR (manual)
    // =========================================================

    @FXML
    private void onAssociarImagem() {
        if (associarImagemUseCase == null) {
            alertErro("Associar Imagem", "UseCase de associação não foi injetado.");
            return;
        }

        ProdutoCatalogoStatusDTO selecionado = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            alertErro("Associar Imagem", "Selecione um produto na tabela.");
            return;
        }

        SKU sku = selecionado.getSku();

        File arquivo = abrirFileChooserImagem("Selecione a imagem do produto");
        if (arquivo == null) return;

        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                associarImagemUseCase.executar(sku, arquivo);
                return null;
            }
        };

        runAsync("Erro ao associar imagem", task, ignored -> {
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Imagem associada");
            ok.setHeaderText(null);
            ok.setContentText("Imagem salva localmente para:\n" + selecionado.getNomeProduto());
            ok.showAndWait();

            carregarTabela();
        });
    }

    // =========================================================
    // BOTÃO TROCAR IMAGEM
    // =========================================================

    @FXML
    private void onTrocarImagem() {
        if (associarImagemUseCase == null) {
            alertErro("Trocar Imagem", "UseCase de associação não foi injetado.");
            return;
        }

        ProdutoCatalogoStatusDTO selecionado = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            alertErro("Trocar Imagem", "Selecione um produto na tabela.");
            return;
        }

        if (selecionado.getEstado() == EstadoProdutoCatalogo.SEM_IMAGEM) {
            alertErro("Trocar Imagem", "Este produto ainda não tem imagem associada. Use “Associar Imagem”.");
            return;
        }

        SKU sku = selecionado.getSku();

        File arquivo = abrirFileChooserImagem("Selecione a NOVA imagem do produto");
        if (arquivo == null) return;

        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                associarImagemUseCase.executar(sku, arquivo);
                return null;
            }
        };

        runAsync("Erro ao trocar imagem", task, ignored -> {
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Imagem atualizada");
            ok.setHeaderText(null);
            ok.setContentText("Imagem trocada com sucesso para:\n" + selecionado.getNomeProduto());
            ok.showAndWait();

            carregarTabela();
        });
    }

    private static final String PASTA_PADRAO_IMAGENS =
            "C:\\Users\\Vitinho\\Desktop\\1. Pronta Entrega 🛵";

    private File abrirFileChooserImagem(String titulo) {
        FileChooser fc = new FileChooser();
        fc.setTitle(titulo);
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        // ✅ Tenta abrir na pasta desejada
        try {
            File dir = new File(PASTA_PADRAO_IMAGENS);
            if (dir.exists() && dir.isDirectory()) {
                fc.setInitialDirectory(dir);
            } else {
                // fallback: Desktop padrão do usuário
                File desktop = new File(System.getProperty("user.home"), "Desktop");
                if (desktop.exists() && desktop.isDirectory()) {
                    fc.setInitialDirectory(desktop);
                }
            }
        } catch (Exception ignored) {
            // fallback silencioso: deixa o FileChooser escolher
        }

        Window w = null;
        try {
            if (tabelaProdutos != null && tabelaProdutos.getScene() != null) {
                w = tabelaProdutos.getScene().getWindow();
            }
        } catch (Exception ignored) {}

        return fc.showOpenDialog(w);
    }

    // =========================================================
    // PAINEL DE DETALHE (V1)
    // =========================================================

    private void atualizarPainelDetalhe(ProdutoCatalogoStatusDTO dto) {
        if (dto == null) {
            limparPainelDetalhe();
            return;
        }

        if (lblProdutoSelecionado != null) {
            lblProdutoSelecionado.setText(dto.getNomeProduto());
        }

        if (lblStatusSelecionado != null) {
            lblStatusSelecionado.setText(textoStatus(dto.getEstado()));
        }

        if (lblAcaoRecomendada != null) {
            lblAcaoRecomendada.setText(acaoRecomendada(dto.getEstado()));
        }

        if (lblTamanhosSelecionado != null) {
            lblTamanhosSelecionado.setText(dto.getTamanhosResumo());
        }

        // Imagem local
        boolean temImagemLocal = false;
        File arquivoImagem = null;

        if (imagemRepository != null) {
            try {
                Optional<File> imgOpt = imagemRepository.obterImagem(dto.getSku());
                if (imgOpt.isPresent() && imgOpt.get().exists() && imgOpt.get().isFile()) {
                    temImagemLocal = true;
                    arquivoImagem = imgOpt.get();
                }
            } catch (Exception ignored) {
                // painel não pode derrubar a UI
            }
        }

        if (imgPreview != null) {
            if (temImagemLocal && arquivoImagem != null) {
                Image image = new Image(arquivoImagem.toURI().toString(), true);
                imgPreview.setImage(image);
            } else {
                imgPreview.setImage(null);
            }
        }

        if (lblSemImagem != null) {
            lblSemImagem.setVisible(!temImagemLocal);
        }
    }

    private void limparPainelDetalhe() {
        if (lblProdutoSelecionado != null) lblProdutoSelecionado.setText("Selecione um produto");
        if (lblStatusSelecionado != null) lblStatusSelecionado.setText("—");
        if (lblTamanhosSelecionado != null) lblTamanhosSelecionado.setText("—");
        if (lblAcaoRecomendada != null) lblAcaoRecomendada.setText("—");

        if (imgPreview != null) imgPreview.setImage(null);
        if (lblSemImagem != null) lblSemImagem.setVisible(false);
    }

    private String textoStatus(EstadoProdutoCatalogo estado) {
        if (estado == null) return "—";
        return switch (estado) {
            case OK -> "OK";
            case SEM_IMAGEM -> "Sem imagem";
            case DESATUALIZADO -> "Desatualizado";
            case ORFAO -> "Órfão";
        };
    }

    private String acaoRecomendada(EstadoProdutoCatalogo estado) {
        if (estado == null) return "—";
        return switch (estado) {
            case OK -> "Nenhuma";
            case SEM_IMAGEM -> "Associar imagem";
            case DESATUALIZADO -> "Sincronizar catálogo";
            case ORFAO -> "Remover do Drive";
        };
    }

    // =========================================================
    // PROGRESSO
    // =========================================================

    private void mostrarProgresso(Task<?> task) {
        if (pbSync != null) {
            pbSync.progressProperty().unbind();
            pbSync.progressProperty().bind(task.progressProperty());
            pbSync.setVisible(true);
            pbSync.setManaged(true);
        }
        if (lblSync != null) {
            lblSync.textProperty().unbind();
            lblSync.textProperty().bind(task.messageProperty());
            lblSync.setVisible(true);
            lblSync.setManaged(true);
        }
    }

    private void ocultarProgresso() {
        if (pbSync != null) {
            pbSync.progressProperty().unbind();
            pbSync.setVisible(false);
            pbSync.setManaged(false);
            pbSync.setProgress(0);
        }
        if (lblSync != null) {
            lblSync.textProperty().unbind();
            lblSync.setText("");
            lblSync.setVisible(false);
            lblSync.setManaged(false);
        }
    }

    private void alertErro(String titulo, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(msg == null ? "Erro desconhecido." : msg);
        alert.showAndWait();
    }
}
