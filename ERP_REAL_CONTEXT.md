# ERP_REAL_CONTEXT

## 1. Resumo executivo

O ERP real e um aplicativo desktop Java/JavaFX com JDBC direto para MySQL. A tabela central de catalogo e estoque e `produtos`, definida em `database.sql` e usada diretamente pelos controllers em `src/erp/controller`.

Conclusoes confirmadas por arquivos do repo:

- Fonte real de produtos/SKUs: tabela `produtos` (`database.sql`).
- ID real do produto: `ProdutoID`, chave primaria auto-incremental (`database.sql`).
- Estoque atual: `QuantidadeEstoque` em `produtos` (`database.sql`, `src/erp/controller/TelaVendasController.java`, `src/erp/controller/AcompanhamentoController.java`).
- Preco atual: `PrecoVendaAtual` em `produtos` (`database.sql`, `src/erp/controller/TelaVendasController.java`).
- SKU vendavel real: `Clube + Modelo + Tipo + Tamanho`, com constraints unicas no banco e buscas usando exatamente esses campos (`database.sql`, `src/erp/controller/CadastroProdutoController.java`, `src/erp/controller/RegistrarPedidoController.java`, `src/erp/application/ImportadorPedidoFornecedorCsv.java`).
- Produto logico para conversa/catalogo visual: `Clube + Modelo + Tipo`; os tamanhos sao variacoes de disponibilidade no ERP e no sincronizador de catalogo (`src/erp/controller/EstoqueController.java`, `src/erp/model/ProdutoAgregadoVO.java`, `src/com/sincronizador/domain/model/SKU.java`).
- Nao ha API HTTP/REST identificada no ERP. A integracao do chatbot deve preferir snapshot/sync controlado a partir do banco ou de um script, nao consumo de endpoint existente.
- Imagens nao estao efetivamente ligadas pelo campo `produtos.CaminhoImagem` no codigo analisado. O fluxo real de imagens esta no modulo `com.sincronizador`, com arquivos locais em `data/catalogo/imagens`, mapeamento `data/catalogo/imagens.properties`, publicacao local em `data/catalogo/catalogo-local.properties` e sincronizacao com Google Drive.

Recomendacao principal: o chatbot deve manter um snapshot proprio em Postgres, sincronizado a partir do MySQL do ERP, usando `products.id = produtos.ProdutoID` para SKUs reais. Para conversa agrupada, o chatbot deve agrupar linhas por `club + model + type` e listar tamanhos/estoque a partir das linhas de `produtos`.

## 2. Estrutura real do ERP

Estrutura observada:

- `src/erp/application`: entrada da aplicacao JavaFX e importadores operacionais.
- `src/erp/controller`: controllers JavaFX com queries SQL diretas.
- `src/erp/model`: VOs/modelos usados pela UI.
- `src/erp/view`: telas FXML e CSS.
- `src/UTIL`: configuracao e conexao JDBC.
- `src/com/sincronizador`: modulo de sincronizacao de catalogo/imagens com Google Drive e catalogo local.
- `database.sql`: dump MySQL com schema e dados exemplo.
- `data/catalogo`: metadados e arquivos locais do catalogo de imagens.
- `lib`: dependencias, incluindo MySQL Connector e bibliotecas Google Drive.

O `README.md` confirma a arquitetura MVC, Java, JavaFX, MySQL 8+ e JDBC puro. `src/erp/application/Main.java` inicia a aplicacao JavaFX e carrega `MainLayout.fxml`. `src/erp/controller/MainLayoutController.java` roteia telas internas como vendas, estoque, pedidos, encomendas, financeiro, relatorio e catalogo.

## 3. Banco de dados identificado

Banco identificado:

- Tipo: MySQL.
- Evidencia: `database.sql` e `README.md`.
- Dump: `database.sql` informa `Host: localhost`, `Database: gemini_erp` e `Server version 8.0.42`.
- Configuracao runtime: `src/UTIL/DatabaseConfig.java` monta URL `jdbc:mysql://host:port/databaseName` e usa defaults `localhost`, `3306`, `gemini_teste`, `root`, senha vazia, `useSSL=false`.
- Arquivo de exemplo: `config/application.properties.example` define `db.host`, `db.port`, `db.name`, `db.user`, `db.password`, `db.useSSL`.
- Conexao: `src/UTIL/ConexaoBanco.java` chama `DatabaseConfig.load()` e abre `DriverManager.getConnection(...)`.

Observacao: existe divergencia entre o nome do banco no dump (`gemini_erp`) e os defaults/config exemplo (`gemini_teste`). A base real em producao fica pendente de validacao.

## 4. Tabelas relevantes

### `produtos`

Fonte principal de catalogo, estoque, preco e custo. Definida em `database.sql`.

Campos:

| Campo | Tipo | Relevancia |
| --- | --- | --- |
| `ProdutoID` | `int NOT NULL AUTO_INCREMENT` | Chave primaria e ID do SKU real. |
| `Modelo` | `varchar(150) NOT NULL` | Modelo/nome comercial. |
| `Clube` | `varchar(150) NOT NULL` | Clube/selecao/marca de conversa. |
| `Tipo` | `enum('Masculina','Feminina','Infantil') NOT NULL` | Segmento/tipo da camisa. |
| `Tamanho` | `varchar(20) NOT NULL` | Tamanho da variacao vendavel. |
| `DescricaoCompleta` | `varchar(500) DEFAULT NULL` | Descricao textual; nem sempre preenchida. |
| `PrecoVendaAtual` | `decimal(10,2) NOT NULL DEFAULT '0.00'` | Preco atual usado em venda e snapshot. |
| `QuantidadeEstoque` | `int NOT NULL DEFAULT '0'` | Estoque atual do SKU. |
| `CustoMedioPonderado` | `decimal(10,2) NOT NULL DEFAULT '0.00'` | Custo medio para financeiro/historico. |
| `DataCadastro` | `datetime DEFAULT CURRENT_TIMESTAMP` | Cadastro da linha. |
| `DataUltimaEntradaEstoque` | `datetime DEFAULT NULL` | Ultima entrada identificada. |
| `CaminhoImagem` | `varchar(500) DEFAULT NULL` | Campo existe, mas uso funcional nao foi encontrado no codigo. |

Constraints:

- `PRIMARY KEY (ProdutoID)`.
- `UNIQUE KEY idx_produto_unico (Modelo, Clube, Tipo, Tamanho)`.
- `UNIQUE KEY uq_produto (Clube, Modelo, Tipo, Tamanho)`.

### `clientes`

Definida em `database.sql`; usada por vendas e encomendas.

Campos:

- `ClienteID int NOT NULL AUTO_INCREMENT`, chave primaria.
- `NomeCliente varchar(255) NOT NULL`.
- `ContatoCliente varchar(255) DEFAULT NULL`.

Usos:

- `src/erp/controller/TelaVendasController.java` busca/cria cliente ao registrar venda.
- `src/erp/controller/EncomendasController.java` busca/cria cliente ao registrar encomenda.

### `vendas`

Definida em `database.sql`; cabecalho da venda.

Campos principais:

- `VendaID int NOT NULL AUTO_INCREMENT`, chave primaria.
- `ClienteID int DEFAULT NULL`, FK para `clientes`.
- `DataVenda datetime DEFAULT CURRENT_TIMESTAMP`.
- `ValorTotalItens`, `ValorDesconto`, `ValorFinalVenda`.
- `StatusPagamento enum('Pago','Pendente')`.
- `DataPrometidaPagamento`.
- `MetodoPagamento`.
- `ValorPago`.

Relevancia para chatbot: historico/atendimento humano; nao deve ser usado para confirmar pagamento automaticamente, porque o objetivo do chatbot exclui confirmacao de pagamento.

### `itensvenda`

Definida em `database.sql`; itens vendidos.

Campos principais:

- `ItemVendaID int NOT NULL AUTO_INCREMENT`, chave primaria.
- `VendaID int NOT NULL`, FK para `vendas`.
- `ProdutoID int NOT NULL`, FK para `produtos`.
- `Quantidade int NOT NULL`, com check `Quantidade > 0`.
- `PrecoVendaUnitarioRegistrado decimal(10,2) NOT NULL`.
- `CustoMedioUnitarioRegistrado decimal(10,2) NOT NULL`.

Relevancia: historico de venda por SKU e custo/preco registrados no momento da venda.

### `pedidosfornecedor`

Definida em `database.sql`; cabecalho de pedidos ao fornecedor.

Campos principais:

- `PedidoFornecedorID int NOT NULL AUTO_INCREMENT`, chave primaria.
- `DataPedido date DEFAULT (curdate())`.
- `NomeFornecedor`.
- `CustoTotalEstimadoItens`.
- `TaxaImportacaoTotal`.
- `CustoTotalFinalPedido`.
- `StatusPedido enum('Realizado','Recebido Parcialmente','Recebido Integralmente')`.

Observacao: o codigo usa tambem status `'Cancelado'` em filtros (`src/erp/controller/AcompanhamentoController.java`, `src/erp/controller/TelaInicialController.java`), mas o enum do dump nao inclui `Cancelado`. Isso fica pendente de validacao contra o banco real.

### `itenspedidofornecedor`

Definida em `database.sql`; itens de pedidos ao fornecedor.

Campos principais:

- `ItemPedidoFornecedorID int NOT NULL AUTO_INCREMENT`, chave primaria.
- `PedidoFornecedorID int NOT NULL`, FK para `pedidosfornecedor`.
- `ProdutoID int NOT NULL`, FK para `produtos`.
- `QuantidadePedida int NOT NULL`, check `> 0`.
- `CustoUnitarioFornecedor decimal(10,2) NOT NULL`.
- `CustoUnitarioComTaxas decimal(10,2) DEFAULT NULL`.
- `QuantidadeRecebida int NOT NULL DEFAULT '0'`, check `>= 0`.
- `DataRecebimento date DEFAULT NULL`.
- `Chegou tinyint(1) NOT NULL DEFAULT '0'`.

Relevancia: entrada futura/pendente de estoque e acompanhamento de compras.

### `encomendascliente`

Definida em `database.sql`; encomendas de clientes.

Campos principais:

- `EncomendaClienteID int NOT NULL AUTO_INCREMENT`, chave primaria.
- `ClienteID int NOT NULL`, FK para `clientes`.
- `DataEncomenda date DEFAULT (curdate())`.
- `Clube`, `Modelo`, `Tipo`, `Tamanho` como campos textuais obrigatorios.
- `Observacao text`.
- `StatusEncomenda enum('Pendente','PedidoAoFornecedorFeito','ProdutoChegou','EntregueAoCliente','Cancelada')`.
- `ProdutoIDAssociado int DEFAULT NULL`, FK opcional para `produtos`, `ON DELETE SET NULL`.

Relevancia: atendimento humano e contexto de solicitacoes fora do estoque. O controller atual registra encomendas por campos textuais e nao preenche `ProdutoIDAssociado`.

### `vendas_temp`

Tabela temporaria/importacao definida em `database.sql`, com campos textuais como `Produto`, `Preco_Unitario`, `Pagamento`, `clube_temp`, `modelo_temp`, `tipo_temp`. Nao encontrei uso no codigo Java analisado. Relevancia para chatbot: baixa, salvo historico legado pendente de validacao.

### Promocoes

Nao foi encontrada tabela de promocoes no schema nem fluxo de promocao explicito no codigo. Existe desconto por venda (`ValorDesconto`, campo de UI em `TelaVendasController.java`), mas nao promocao de produto persistida.

## 5. Tabela de produtos

`produtos` representa linhas vendaveis reais. Cada linha tem um `ProdutoID`, uma combinacao de `Clube`, `Modelo`, `Tipo`, `Tamanho`, preco, estoque e custo.

A hipotese esta confirmada:

```text
Clube + Modelo + Tipo + Tamanho = SKU vendavel real
```

Evidencias:

- `database.sql` cria duas constraints unicas envolvendo `Modelo/Clube/Tipo/Tamanho` e `Clube/Modelo/Tipo/Tamanho`.
- `src/erp/controller/CadastroProdutoController.java` procura produto existente com `WHERE Modelo = ? AND Clube = ? AND Tipo = ? AND Tamanho = ?`; se existe, soma estoque; se nao existe, insere.
- `src/erp/controller/RegistrarPedidoController.java` usa a mesma chave logica em `findOrCreateProdutoID`.
- `src/erp/application/ImportadorPedidoFornecedorCsv.java` tambem usa a mesma chave para localizar/criar produto.
- `src/erp/application/ImportadorCargaInicialEstoque.java` agrega por `ProdutoKey(clube, modelo, tipo)` e insere uma linha por tamanho com saldo positivo.

Chave logica e agrupamentos:

- SKU real/vendavel: `Clube + Modelo + Tipo + Tamanho`.
- Produto logico para conversa: `Clube + Modelo + Tipo`.
- Agrupamento parcial usado na tela de vendas: a busca cria descricao `"clube modelo"` e depois separa por `tipo` e `tamanho` via `ProdutoAgregadoVO`. Como pode haver o mesmo clube/modelo em mais de um tipo, o chatbot deve incluir `type` no produto logico para evitar misturar masculino/feminino/infantil.

Campos solicitados:

| Campo solicitado | Status no ERP | Evidencia |
| --- | --- | --- |
| `ProdutoID` | Confirmado | `database.sql`, controllers de vendas/pedidos/cadastro. |
| `Clube` | Confirmado | `database.sql`, queries em `src/erp/controller`. |
| `Modelo` | Confirmado | `database.sql`, queries em `src/erp/controller`. |
| `Tipo` | Confirmado | enum no schema; valores `Masculina`, `Feminina`, `Infantil`. |
| `Tamanho` | Confirmado | `varchar(20)`; grades adultas e infantis no codigo. |
| `DescricaoCompleta` | Confirmado, mas opcional | `database.sql`; preenchido em cadastro/importadores. |
| `PrecoVendaAtual` | Confirmado | usado na venda e financeiro. |
| `QuantidadeEstoque` | Confirmado | usado em estoque, venda, catalogo. |
| `CustoMedioPonderado` | Confirmado | usado em estoque/entrada e financeiro. |
| `CaminhoImagem` | Campo existe, uso nao confirmado | schema define campo; nenhum uso encontrado no codigo. |
| `DataCadastro` | Confirmado | schema define default `CURRENT_TIMESTAMP`. |
| `DataUltimaEntradaEstoque` | Confirmado | atualizado em cadastro/entrada. |

## 6. Estoque e movimentacoes

### Fonte de estoque atual

`produtos.QuantidadeEstoque` e a fonte operacional usada pelo ERP para listar estoque, vender e sincronizar catalogo. Evidencias:

- `src/erp/controller/EstoqueController.java`: lista `Produtos WHERE QuantidadeEstoque > 0`.
- `src/erp/controller/TelaVendasController.java`: busca produtos vendaveis com `QuantidadeEstoque > 0`.
- `src/com/sincronizador/infrastructure/erp/ErpEstoqueReader.java`: sincronizador le `Clube, Modelo, Tipo, Tamanho, QuantidadeEstoque FROM produtos WHERE QuantidadeEstoque > 0`.
- `src/erp/controller/FinanceiroController.java`: calcula valor de estoque com `SUM(QuantidadeEstoque * PrecoVendaAtual)`.

### Entradas de estoque

Entradas identificadas:

1. Cadastro/entrada manual em `src/erp/controller/CadastroProdutoController.java`.
   - Se o SKU ja existe, soma a quantidade nova em `QuantidadeEstoque`, recalcula `CustoMedioPonderado` e atualiza `DataUltimaEntradaEstoque`.
   - Se nao existe, insere novo produto com estoque inicial.

2. Recebimento de pedidos a fornecedor em `src/erp/controller/AcompanhamentoController.java`.
   - Atualiza `ItensPedidoFornecedor.QuantidadeRecebida`.
   - Busca estoque/custo atual em `Produtos`.
   - Atualiza `Produtos.QuantidadeEstoque`, `CustoMedioPonderado` e `DataUltimaEntradaEstoque`.
   - Atualiza status do pedido para `Recebido Parcialmente` ou `Recebido Integralmente`.

3. Carga inicial em `src/erp/application/ImportadorCargaInicialEstoque.java`.
   - Exige tabela `Produtos` vazia.
   - Insere uma linha por `Clube/Modelo/Tipo/Tamanho` com saldo positivo.

4. Importacao de pedido fornecedor em `src/erp/application/ImportadorPedidoFornecedorCsv.java`.
   - Cria/atualiza produtos com `QuantidadeEstoque = 0`.
   - Nao representa entrada fisica ate o recebimento.

### Saidas de estoque

Saidas identificadas:

1. Venda em `src/erp/controller/TelaVendasController.java`.
   - Insere cabecalho em `Vendas`.
   - Insere itens em `ItensVenda`.
   - Decrementa estoque com:

```sql
UPDATE Produtos
SET QuantidadeEstoque = QuantidadeEstoque - ?
WHERE ProdutoID = ? AND QuantidadeEstoque >= ?
```

   - Essa condicao reduz risco de estoque negativo em venda concorrente.

2. Troca/alteracao de item vendido em `src/erp/controller/HistoricoVendasController.java`.
   - Reverte estoque do produto antigo com `QuantidadeEstoque = QuantidadeEstoque + ?`.
   - Baixa estoque do produto novo com `QuantidadeEstoque = QuantidadeEstoque - ?`.
   - Atualiza `ItensVenda`.

### Confiabilidade para o chatbot

`QuantidadeEstoque` e confiavel como fonte atual do ERP, com ressalvas:

- O ERP e desktop e usa JDBC direto; nao ha evidencia de locks explicitamente alem das transacoes nas rotinas de venda/entrada.
- A venda protege contra estoque insuficiente no `UPDATE ... WHERE QuantidadeEstoque >= ?`, dentro de transacao.
- O chatbot nao deve prometer disponibilidade absoluta; deve comunicar disponibilidade como "consta em estoque no sistema" ou equivalente e encaminhar para humano antes de fechamento/reserva.
- Se o chatbot mantiver snapshot, pode haver defasagem entre a ultima sincronizacao e vendas realizadas no ERP.

### Logs/historico de movimentacao

Nao foi encontrada tabela generica de movimento de estoque. Historicos existentes:

- Saidas: `vendas` + `itensvenda`.
- Entradas por fornecedor: `pedidosfornecedor` + `itenspedidofornecedor`, com `QuantidadeRecebida`.
- Encomendas: `encomendascliente`, mas nao e movimento de estoque.

Pendente de validacao: se existe alguma tabela de auditoria no banco real que nao esteja no dump `database.sql`.

## 7. Imagens dos produtos

### Campo `CaminhoImagem`

`produtos.CaminhoImagem` existe em `database.sql`, tipo `varchar(500) DEFAULT NULL`, mas nao foi encontrado uso funcional no codigo Java analisado. O dump exemplo insere produtos com `CaminhoImagem = NULL`.

Conclusao: nao tratar `CaminhoImagem` como fonte confiavel de imagem ainda. Marcar como pendente de validacao no banco real.

### Fluxo real de imagens encontrado

O repo tem um modulo dedicado de catalogo/imagens em `src/com/sincronizador`.

Fontes locais:

- `data/catalogo/imagens`: pasta com 95 arquivos `.jpg`, `.png` e `.webp`.
- `data/catalogo/imagens.properties`: mapeia `CLUBE|MODELO|TIPO` para arquivo local. Exemplo de padrao: `FLAMENGO|HOME 2025|MASCULINO=FLAMENGO_HOME_2025_MASCULINO.jpg`.
- `src/com/sincronizador/infrastructure/local/PropertiesImagemRepository.java`: implementa o mapeamento SKU logico -> arquivo local e copia imagens para `data/catalogo/imagens`.

Catalogo local publicado:

- `data/catalogo/catalogo-local.properties`: mapeia `CLUBE|MODELO|TIPO` para nomes de arquivo de catalogo publicados.
- `src/com/sincronizador/infrastructure/local/LocalCatalogoWriter.java`: publica imagens em uma pasta local configurada no codigo.
- `src/com/sincronizador/SincronizadorEmbeddedFactory.java`: define `CATALOGO_LOCAL_DIR` como caminho absoluto em Desktop do usuario.

Google Drive:

- `src/app.properties` define `catalogo.folderId`.
- `src/com/sincronizador/config/DriveConfig.java` autentica com Google Drive via OAuth.
- `src/com/sincronizador/infrastructure/drive/DriveCatalogoReader.java` le arquivos do Drive por `folderId`, metadata e checksum.
- `src/com/sincronizador/infrastructure/drive/DriveCatalogoWriter.java` cria, renomeia, remove, troca imagem e grava appProperties.
- `src/com/sincronizador/infrastructure/drive/DriveMetadataKeys.java` define metadata como `sku_clube`, `sku_modelo`, `sku_tipo`, `sku_key`, `sku_tamanhos_fabrica`.

### Multiplicidade de imagens

O mapeamento real de imagem e por `Clube|Modelo|Tipo`, nao por `ProdutoID` nem por tamanho. Portanto, uma imagem atende a varias linhas/SKUs de tamanhos diferentes.

Nao encontrei suporte a multiplas imagens por produto logico. O modelo atual aponta para uma imagem por `CLUBE|MODELO|TIPO`.

### Como o chatbot deve acessar imagens

Opcoes recomendadas:

1. Preferida para V1: durante o sync ERP -> chatbot, resolver imagem por `club + model + type` usando `data/catalogo/imagens.properties` e copiar/publicar um caminho acessivel ao chatbot.
2. Se o chatbot precisa enviar imagem por WhatsApp, usar uma URL publica/assinada gerada a partir de um storage proprio ou Google Drive com permissao adequada. O ERP desktop nao serve imagens por endpoint.
3. Nao depender diretamente de caminho absoluto local do ERP para o runtime do chatbot, salvo se ambos rodarem na mesma maquina e isso for explicitamente controlado.
4. Manter no Postgres do chatbot `image_path` ou `image_url` como campo derivado do sync, nao assumindo `produtos.CaminhoImagem` ate validacao.

## 8. Encomendas, pedidos e clientes

### Encomendas de clientes

`encomendascliente` tem `ProdutoIDAssociado`, mas o controller atual nao usa esse campo ao criar encomenda. `src/erp/controller/EncomendasController.java` insere `ClienteID`, `DataEncomenda`, `StatusEncomenda`, `Clube`, `Modelo`, `Tipo`, `Tamanho`.

Isso indica que encomendas podem representar:

- produto existente ainda nao associado;
- produto fora do catalogo;
- pedido livre do cliente com descricao textual.

Relevancia para chatbot:

- Pode ajudar atendimento humano a ver demandas do cliente.
- Pode alimentar uma rotina futura de "registrar interesse", mas o chatbot nao deve criar encomenda automaticamente sem regra de negocio aprovada.
- Nao deve ser usado como estoque nem como reserva confirmada.

### Pedidos a fornecedor

`pedidosfornecedor` e `itenspedidofornecedor` referenciam `ProdutoID` obrigatoriamente.

Fluxos:

- `src/erp/controller/RegistrarPedidoController.java`: registra pedido ao fornecedor e usa `findOrCreateProdutoID` para criar SKU com estoque zero se nao existir.
- `src/erp/controller/AcompanhamentoController.java`: acompanha itens pendentes, registra chegada e atualiza estoque.
- `src/erp/application/ImportadorPedidoFornecedorCsv.java`: importa pedido por CSV, criando/atualizando produtos.

Relevancia para chatbot:

- Pode informar contexto interno de itens em transito, mas isso nao equivale a disponibilidade para venda.
- Para V1, manter fora das respostas automatizadas ou usar apenas como sinal para encaminhar humano.

### Clientes e vendas

`clientes` e usado por vendas/encomendas. `vendas` e `itensvenda` registram historico de venda e pagamento. O chatbot descrito nao deve confirmar pagamento, negociar desconto ou fechar venda, entao essas tabelas sao mais relevantes para atendimento humano e historico do que para resposta automatica de catalogo.

## 9. Fluxos de codigo encontrados

### Linguagem/framework

- Java.
- JavaFX/FXML para UI desktop.
- JDBC puro para banco.
- MySQL Connector em `lib/mysql-connector-java-9.3.0.jar`.
- Google Drive API no modulo sincronizador.

Evidencias: `README.md`, `src/erp/application/Main.java`, `src/erp/view/*.fxml`, `src/UTIL/ConexaoBanco.java`, `lib/`.

### Produtos cadastrados/editados

- `src/erp/controller/CadastroProdutoController.java`: cadastro manual/entrada direta em estoque; insere ou atualiza `Produtos`.
- `src/erp/controller/RegistrarPedidoController.java`: ao criar pedido fornecedor, cria produto se nao existir com preco default `140.00`, estoque `0` e custo do item.
- `src/erp/application/ImportadorPedidoFornecedorCsv.java`: cria produto com estoque `0` ou atualiza preco/descricao.
- `src/erp/application/ImportadorCargaInicialEstoque.java`: carga inicial, exigindo `Produtos` vazia.

### Produtos listados/filtrados

- `src/erp/controller/EstoqueController.java`: lista estoque positivo e agrupa por `Clube|Modelo|Tipo`, separando adulto e infantil.
- `src/erp/controller/TelaVendasController.java`: busca por `CONCAT(clube, ' ', modelo) LIKE ?`, filtra `QuantidadeEstoque > 0`, agrupa para escolha de tipo/tamanho.
- `src/erp/controller/RelatorioProdutosController.java`: relatorios de estoque e vendas por clube/tamanho.
- `src/com/sincronizador/infrastructure/erp/ErpEstoqueReader.java`: le estoque positivo para sincronizacao de catalogo.

### Produtos vendidos

- `src/erp/controller/TelaVendasController.java`: registra venda, itens, baixa estoque e atualiza preco atual se a venda teve preco maior que `PrecoVendaAtual`.
- `src/erp/controller/HistoricoVendasController.java`: historico e troca/alteracao de item vendido, com ajuste de estoque.

### Estoque atualizado

- Entrada: `CadastroProdutoController`, `AcompanhamentoController`, `ImportadorCargaInicialEstoque`.
- Saida: `TelaVendasController`, `HistoricoVendasController`.
- Pedidos a fornecedor criam produtos com estoque zero ate chegada: `RegistrarPedidoController`, `ImportadorPedidoFornecedorCsv`.

### API interna aproveitavel

Nao foi identificada API HTTP/REST. O que existe:

- Classes Java reutilizaveis dentro do mesmo processo (`ConexaoBanco`, `ErpEstoqueReader`, repositorios do sincronizador).
- Sincronizador JavaFX/Drive embutido na tela de catalogo.
- Arquivos locais de catalogo/imagens.

Para o chatbot, o reaproveitamento direto de classes Java e possivel apenas se houver um processo Java dedicado. Para um chatbot separado, a integracao mais segura e via banco/snapshot/script.

## 10. Integracao recomendada ERP -> chatbot

Recomendacao: manter snapshot no Postgres do chatbot, alimentado por sync controlado do MySQL do ERP. Evitar leitura direta do MySQL em tempo real pelo bot de WhatsApp.

Motivos:

- O ERP e desktop, sem API de servico.
- O chatbot nao deve arriscar lock/acoplamento ao banco operacional durante conversa.
- Snapshot permite normalizar tipos, imagens e campos esperados pelo chatbot.
- Snapshot permite aplicar politicas de seguranca: nao prometer estoque se sync estiver antigo, ocultar produtos zerados, detectar imagem faltante.

### Estrategia de sync sugerida

Fonte:

```sql
SELECT
  ProdutoID,
  Clube,
  Modelo,
  Tipo,
  Tamanho,
  DescricaoCompleta,
  PrecoVendaAtual,
  QuantidadeEstoque,
  CaminhoImagem,
  DataCadastro,
  DataUltimaEntradaEstoque
FROM produtos;
```

Transformacao:

- `products.id = ProdutoID`.
- Normalizar `Tipo`: ERP usa `Masculina/Feminina/Infantil`; chatbot pode armazenar como texto original ou converter para enum proprio, mantendo mapeamento reversivel.
- Manter produtos com `QuantidadeEstoque = 0` no snapshot com status indisponivel, ou remover da busca publica conforme decisao de produto.
- Resolver imagem por `Clube|Modelo|Tipo` usando `data/catalogo/imagens.properties` ou catalogo publicado, nao por `CaminhoImagem` ate validacao.
- Atualizar `updated_at/synced_at` no Postgres do chatbot.

Frequencia:

- V1 minima: sync manual sob comando/rotina antes de abrir atendimento.
- Melhor V1: sync ao iniciar o chatbot + sync periodico a cada 5 a 15 minutos.
- Operacao mais segura: script agendado no Windows ou servico leve que roda sync periodico e registra status.
- Evento em tempo real: nao recomendado inicialmente, pois o ERP nao possui eventos/hooks.

Politicas para respostas:

- Se `stock_quantity <= 0`: nao oferecer como disponivel; pode dizer que nao consta em estoque e encaminhar para humano se o cliente quiser encomendar.
- Se `last_sync_at` estiver antigo: responder com cautela e encaminhar para humano antes de qualquer afirmacao forte.
- Se `image_path` inacessivel: responder sem imagem e registrar pendencia.
- Se preco mudou entre conversa e atendimento humano: humano confirma preco; chatbot informa o preco "constante no sistema" com ressalva operacional definida.

Riscos:

- Produto removido no ERP: o sync deve marcar como inativo/removido no chatbot, nao reutilizar ID.
- Estoque zerado apos sync: risco natural de defasagem; mitigado com sync frequente e linguagem cautelosa.
- Imagem inacessivel: `imagens.properties` pode apontar para arquivo ausente; o repositorio local limpa associacoes quebradas quando detecta, mas o sync do chatbot deve validar existencia.
- Alteracao de preco: `PrecoVendaAtual` pode ser atualizado em cadastro/importacao e tambem na venda se o preco vendido for maior.
- Conflito de dados: nomes podem ter caixa/espacos diferentes; usar `ProdutoID` para SKU e `Clube/Modelo/Tipo` apenas para agrupamento.

## 11. Mapeamento ERP -> Chatbot

Mapeamento solicitado:

| Chatbot | ERP | Status |
| --- | --- | --- |
| `products.id` | `produtos.ProdutoID` | Viavel e recomendado. |
| `products.club` | `produtos.Clube` | Confirmado. |
| `products.model` | `produtos.Modelo` | Confirmado. |
| `products.type` | `produtos.Tipo` | Confirmado; valores ERP sao `Masculina`, `Feminina`, `Infantil`. |
| `products.size` | `produtos.Tamanho` | Confirmado. |
| `products.current_sale_price` | `produtos.PrecoVendaAtual` | Confirmado. |
| `products.stock_quantity` | `produtos.QuantidadeEstoque` | Confirmado. |
| `products.image_path` | `produtos.CaminhoImagem` ou imagem derivada | `CaminhoImagem` pendente; recomendado derivar de `data/catalogo/imagens.properties` por `club/model/type`. |

Agrupamento:

```text
Produto logico = club + model + type
SKU real = club + model + type + size
```

Esse agrupamento e o melhor alinhamento com o ERP real. A unica nuance: o codigo do ERP em alguns pontos exibe produto como `clube + modelo`, mas preserva `tipo` antes de selecionar tamanho. Para o chatbot, incluir `type` no produto logico evita ambiguidade.

Consulta base recomendada para catalogo conversacional:

```sql
SELECT
  ProdutoID,
  Clube,
  Modelo,
  Tipo,
  Tamanho,
  DescricaoCompleta,
  PrecoVendaAtual,
  QuantidadeEstoque,
  DataUltimaEntradaEstoque
FROM produtos
WHERE QuantidadeEstoque > 0
ORDER BY Clube, Modelo, Tipo, Tamanho;
```

Para manter produtos indisponiveis no snapshot, remover o `WHERE` e filtrar na camada de resposta.

## 12. Divergencias com os documentos atuais do chatbot

Nao analisei os documentos do projeto separado do chatbot porque eles nao estao neste repo do ERP. Comparacao baseada apenas no modelo esperado informado na solicitacao:

- `products.id = ProdutoID`: alinhado.
- `products.club/model/type/size`: alinhado.
- `products.current_sale_price = PrecoVendaAtual`: alinhado.
- `products.stock_quantity = QuantidadeEstoque`: alinhado.
- `products.image_path = CaminhoImagem`: divergencia/pendencia. O campo existe, mas o fluxo real de imagem usa `data/catalogo/imagens.properties`, arquivos locais e Google Drive por `Clube|Modelo|Tipo`.
- `Produto logico = club + model + type`: alinhado com estoque/catalogo.
- `SKU real = club + model + type + size`: alinhado com constraints e fluxos de criacao/venda.
- Se os docs do chatbot assumem API existente do ERP, isso diverge: nao foi encontrada API HTTP no repo.
- Se os docs assumem multiplas imagens por SKU/tamanho, isso diverge: o ERP atual mapeia uma imagem por produto logico `Clube|Modelo|Tipo`.

## 13. Decisoes recomendadas

1. Usar `produtos` como fonte unica de catalogo e estoque para V1.
2. Usar `ProdutoID` como `products.id` no Postgres do chatbot.
3. Tratar `ProdutoID` como SKU real, nao como produto logico.
4. Agrupar conversa por `Clube + Modelo + Tipo`.
5. Exibir tamanhos a partir das linhas agrupadas com `QuantidadeEstoque > 0`.
6. Nao usar `CaminhoImagem` em V1 sem validar dados reais; usar mapeamento de imagens do sincronizador.
7. Nao consultar pedidos/encomendas para prometer chegada, reserva ou entrega; usar apenas para contexto humano.
8. Implementar sync periodico para snapshot Postgres, com `last_sync_at` e validacao de arquivo de imagem.
9. Manter linguagem do chatbot conservadora: "consta no sistema", "posso chamar um atendente para confirmar", sem fechar venda.
10. Nao criar endpoint no ERP agora. Se futuramente necessario, criar endpoint read-only ou exportador, mas isso e fora do escopo desta analise.

## 14. Pendencias de validacao

- Confirmar qual banco real esta em uso: `gemini_erp` no dump ou `gemini_teste` nas configuracoes/defaults.
- Validar no banco real se `produtos.CaminhoImagem` esta preenchido em registros atuais. No codigo, uso nao foi encontrado.
- Validar se existem tabelas adicionais no banco real que nao aparecem em `database.sql`, especialmente auditoria/movimentacao/promocoes.
- Resolver divergencia do status `Cancelado` em pedidos fornecedor: codigo filtra esse status, mas o enum do dump nao inclui `Cancelado`.
- Validar se os valores reais de `Tipo` estao sempre em `Masculina/Feminina/Infantil` ou se ha legado como `Masculino/Feminino`.
- Validar estrategia operacional para disponibilizar imagens ao WhatsApp: Google Drive publico, storage proprio ou copia servida pelo projeto do chatbot.
- Validar se o chatbot deve manter no snapshot produtos com estoque zero como inativos ou apenas produtos disponiveis.
- Validar frequencia de sync aceitavel para a loja considerando volume de vendas simultaneas.
- Validar se o arquivo `config/google/credentials.json` deve permanecer no repo, pois ha credenciais OAuth versionadas. Nao expor esses valores ao chatbot.
- Validar se `data/catalogo/catalogo-local.properties` e a pasta local absoluta em `SincronizadorEmbeddedFactory` representam o ambiente de producao.

## 15. Arquivos analisados

| Caminho | Resumo |
| --- | --- |
| `database.sql` | Dump MySQL com tabelas `clientes`, `produtos`, `encomendascliente`, `pedidosfornecedor`, `itenspedidofornecedor`, `vendas`, `itensvenda`, `vendas_temp`. |
| `README.md` | Descreve ERP desktop Java/JavaFX, MySQL, JDBC, MVC e modulos de estoque, vendas, encomendas e pedidos. |
| `config/application.properties.example` | Exemplo de configuracao MySQL. |
| `src/app.properties` | Configuracao do folderId do catalogo no Google Drive. |
| `src/UTIL/DatabaseConfig.java` | Resolve configuracao do banco, defaults, arquivo ProgramData/dev e monta JDBC URL. |
| `src/UTIL/ConexaoBanco.java` | Abre conexao JDBC usando `DatabaseConfig`. |
| `src/erp/application/Main.java` | Inicializacao JavaFX do ERP. |
| `src/erp/controller/MainLayoutController.java` | Navegacao entre telas e configuracao do banco pela UI. |
| `src/erp/controller/CadastroProdutoController.java` | Cadastro/entrada manual de produtos, soma estoque e recalcula custo medio. |
| `src/erp/controller/TelaVendasController.java` | Busca produtos disponiveis, monta carrinho, registra venda, insere itens e baixa estoque com guarda contra estoque insuficiente. |
| `src/erp/controller/HistoricoVendasController.java` | Historico de vendas, pagamento e troca/alteracao com ajuste de estoque. |
| `src/erp/controller/EstoqueController.java` | Listagem de estoque positivo agrupado por `Clube|Modelo|Tipo`, com grades adulto/infantil. |
| `src/erp/controller/RegistrarPedidoController.java` | Registro de pedidos a fornecedor e criacao de produto com estoque zero se necessario. |
| `src/erp/controller/AcompanhamentoController.java` | Acompanhamento de pedidos fornecedor, recebimento de itens e entrada de estoque. |
| `src/erp/controller/EncomendasController.java` | Registro/listagem/status de encomendas de cliente usando campos textuais de produto. |
| `src/erp/controller/FinanceiroController.java` | Calculos financeiros com vendas, custos e valor de estoque. |
| `src/erp/controller/RelatorioProdutosController.java` | Relatorios por produtos, clubes, tamanhos e estoque. |
| `src/erp/controller/CatalogoController.java` | Embute a UI do sincronizador de catalogo. |
| `src/erp/model/ProdutoAgregadoVO.java` | Modelo de produto agregado por descricao, com variantes por tipo/tamanho e detalhes de `ProdutoID`, preco, estoque e custo. |
| `src/erp/model/ProdutoEstoque.java` | Modelo de estoque adulto agrupado por tamanhos. |
| `src/erp/model/ProdutoEstoqueInfantil.java` | Modelo de estoque infantil por tamanhos numericos. |
| `src/erp/model/EncomendaVO.java` | VO de encomenda exibida na UI. |
| `src/erp/model/PedidoVO.java`, `src/erp/model/ItemPedidoVO.java`, `src/erp/model/ItemPedidoDetalheVO.java` | VOs de pedido fornecedor e itens. |
| `src/erp/application/ImportadorCargaInicialEstoque.java` | Importador de carga inicial de estoque a partir de CSV, insere uma linha por SKU/tamanho. |
| `src/erp/application/ImportadorPedidoFornecedorCsv.java` | Importador de pedidos fornecedor por CSV, cria/atualiza produtos e itens de pedido. |
| `src/com/sincronizador/infrastructure/erp/ErpEstoqueReader.java` | Le estoque positivo do ERP para sincronizacao de catalogo. |
| `src/com/sincronizador/SincronizadorEmbeddedFactory.java` | Monta sincronizador embutido, Google Drive, repositorio local de imagens e catalogo local. |
| `src/com/sincronizador/config/DriveConfig.java` | Autenticacao Google Drive OAuth e resolucao de credenciais/tokens. |
| `src/com/sincronizador/application/usecase/SincronizarCatalogoUseCase.java` | Compara ERP x Drive, cria/atualiza/remove itens e publica catalogo local. |
| `src/com/sincronizador/application/usecase/AssociarImagemAoCatalogoUseCase.java` | Associa imagem local ao SKU logico. |
| `src/com/sincronizador/infrastructure/local/PropertiesImagemRepository.java` | Mapeia `CLUBE|MODELO|TIPO` para arquivo local em `data/catalogo/imagens`. |
| `src/com/sincronizador/infrastructure/local/LocalCatalogoWriter.java` | Publica catalogo local e mantem indice `catalogo-local.properties`. |
| `src/com/sincronizador/infrastructure/drive/DriveCatalogoReader.java` | Le itens de catalogo no Drive por metadata. |
| `src/com/sincronizador/infrastructure/drive/DriveCatalogoWriter.java` | Cria, atualiza, remove e troca imagem no Drive. |
| `src/com/sincronizador/infrastructure/drive/DriveMetadataKeys.java` | Define chaves de metadata do catalogo no Drive. |
| `src/com/sincronizador/domain/model/SKU.java` | Define identidade logica do catalogo por `Produto(clube, modelo, tipo)`. |
| `src/com/sincronizador/domain/model/Produto.java` | Modelo logico do produto de catalogo. |
| `data/catalogo/imagens.properties` | Mapeamento de imagem local por `CLUBE|MODELO|TIPO`. |
| `data/catalogo/catalogo-local.properties` | Indice de arquivos publicados no catalogo local por `CLUBE|MODELO|TIPO`. |
| `data/catalogo/imagens/` | Pasta local com 95 arquivos de imagem de produtos. |
| `config/google/credentials.json` | Credenciais OAuth do sincronizador Google Drive; conteudo sensivel, citado apenas como existencia. |
