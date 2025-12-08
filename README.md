# ERP - Loja de Artigos Esportivos

Sistema ERP acadêmico desenvolvido em Java para gerenciar uma loja de artigos esportivos.  
O sistema permite o **cadastro e manutenção de produtos, clientes e vendas**, simulando o fluxo básico de um pequeno comércio.

---

## 🎯 Objetivo do projeto

Este projeto foi desenvolvido na disciplina de Sistemas de Informação / Programação Orientada a Objetos com o objetivo de:

- Praticar **orientação a objetos em Java**  
- Modelar um **sistema de gestão** (ERP simplificado)  
- Trabalhar conceitos como **camadas de apresentação, domínio e persistência**  
- Simular processos reais de **cadastro, consulta e movimentação de estoque/vendas**

---

## 🧩 Funcionalidades principais

- **Cadastro de produtos**
  - Inclusão, edição, exclusão e listagem de produtos
  - Atributos típicos: código, descrição, categoria, preço, quantidade em estoque

- **Registro de vendas**
  - Seleção de cliente e produtos
  - Cálculo automático do total da venda
  - Atualização de estoque

- **Consultas**
  - Listagem de produtos cadastrados
  - Listagem de clientes
  - Histórico básico de vendas (conforme implementação)

## 🛠️ Tecnologias utilizadas

- **Linguagem:** Java  
- **Paradigma:** Programação Orientada a Objetos (POO)  
- **Interface:** aplicação desktop (JavaFX)  
- **IDE utilizada:** (ex.: Eclipse / VSCode)  
- **Outros recursos:**
  - Organização em pacotes (camadas de `model`, `view`, `controller`)
  - Classes utilitárias na pasta `UTIL`
  - Dependências externas na pasta `lib`

## 🗂️ Estrutura do projeto

```text
ERP-LOJAESPORTES/
├── .vscode/              # Configurações de ambiente (opcional)
├── UTIL/                 # Classes utilitárias (validação, mensagens, etc.)
├── bin/                  # Arquivos compilados (.class)
├── lib/                  # Bibliotecas externas (se houver)
└── src/
    └── erp/
        ├── model/        # Classes de domínio (Produto, Cliente, Venda, ItemVenda, etc.)
        ├── dao/          # Classes de acesso a dados (separação de persistência)
        ├── view/         # Telas / formulários da interface gráfica
        ├── controller/   # Lógica de controle entre view e model
        └── Main.java     # Classe principal para iniciar a aplicação
