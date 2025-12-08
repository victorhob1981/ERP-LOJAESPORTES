# 🛒 ERP Sports Store - Sistema de Gestão Comercial

> Sistema Desktop desenvolvido em Java para gerenciamento integral de uma loja de artigos esportivos, abrangendo desde o controle de estoque até a análise financeira.

## 📌 Sobre o Projeto

Este projeto foi desenvolvido como parte do portfólio acadêmico do curso de **Sistemas de Informação na UFRRJ**. O objetivo foi criar uma solução **ERP (Enterprise Resource Planning)** funcional que simula o dia a dia de um comércio varejista.

A aplicação resolve problemas reais de gestão, permitindo o controle de grades de produtos (tamanhos e tipos), fluxo de caixa, gestão de encomendas de clientes e pedidos a fornecedores.


---

## 🚀 Funcionalidades Principais

O sistema é dividido em módulos integrados:

### 📦 Gestão de Estoque
- Controle detalhado por **Modelo, Clube, Tipo (Masculino/Feminino/Infantil)** e **Tamanho**.
- Suporte a grade de tamanhos Adulto (P ao 4GG) e Infantil (16 ao 28).
- Visualização rápida de itens com baixo estoque.

### 💰 Financeiro & Vendas
- **PDV Completo:** Cálculo de subtotal, descontos e troco.
- **Formas de Pagamento:** Suporte a Dinheiro, Pix, Cartão e Vendas "Fiado" (Pendente).
- **Dashboard Financeiro:** Gráficos de Faturamento vs. Custo vs. Lucro.
- Cálculo automático de Ticket Médio e Margem de Lucro.

### 🚚 Cadeia de Suprimentos
- **Encomendas de Clientes:** Registro e acompanhamento de pedidos específicos.
- **Pedidos a Fornecedores:** Gestão de compras para reposição.
- **Conferência de Entrada:** Validação de itens recebidos vs. itens pedidos.

### 📊 Relatórios e Business Intelligence
- Relatórios de "Clube Mais Vendido" e "Tamanho Mais Vendido".
- Gráficos de barras para análise de performance de vendas.

---

## 🛠️ Tecnologias e Arquitetura

O projeto foi construído seguindo o padrão arquitetural **MVC (Model-View-Controller)** para garantir a separação de responsabilidades e facilidade de manutenção.

* **Linguagem:** Java (JDK 21+ recomendado).
* **Interface Gráfica:** JavaFX (com FXML para definição de layouts).
* **Banco de Dados:** MySQL (8.0+).
* **Conectividade:** JDBC puro (Java Database Connectivity) para performance e controle de transações.
* **Bibliotecas:** `mysql-connector-java`, `javafx-controls`, `javafx-fxml`.

### Estrutura de Pastas
- `src/erp/model`: Classes de objeto de valor (VO) e regras de negócio.
- `src/erp/view`: Arquivos `.fxml` da interface.
- `src/erp/controller`: Lógica de interação entre a view e o model.
- `UTIL`: Classes utilitárias para conexão com banco de dados.

---

## 🔧 Como Executar

### Pré-requisitos
- Java JDK 17 ou superior.
- MySQL Server instalado e rodando.
- SDK do JavaFX configurado na sua IDE ou via linha de comando.

### Passo a Passo
1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/victorhob1981/ERP-LOJAESPORTES.git]
    ```
2.  **Configuração do Banco:**
    - Crie um banco de dados no MySQL chamado `gemini_erp` (ou ajuste no arquivo `ConexaoBanco.java`).
    - Execute o script SQL disponível na pasta `database/` para criar as tabelas.
3.  **Configuração da IDE (VS Code / Eclipse / IntelliJ):**
    - Adicione as bibliotecas do JavaFX e o Driver MySQL ao `CLASSPATH` ou `Module Path`.
    - Ajuste as credenciais de banco em `src/UTIL/ConexaoBanco.java`:
      ```java
      private static final String URL = "jdbc:mysql://localhost:3306/gemini_erp";
      private static final String USUARIO = "seu_usuario";
      private static final String SENHA = "sua_senha";
      ```
4.  **Executar:**
    - Rode a classe principal: `src/erp/application/Main.java`.

---

## 👨‍💻 Autor

**[Victor Hugo de Oliveira Barbosa]** *Aluno de Sistemas de Informação - UFRRJ*

Estudante apaixonado por desenvolvimento de software, com foco em Java e soluções corporativas. Buscando oportunidade de estágio para aplicar conhecimentos em arquitetura de software e banco de dados.

LinkedIn: https://www.linkedin.com/in/victor-ho-barbosa 
Email: victorhob23@gmail.com
---
