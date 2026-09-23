# HelpDesk Lite

<div align="center">
  <p>
    <strong>Gestão simples, organizada e inteligente para chamados internos</strong>
  </p>
</div>

<div align="center">
  <code>helpdesk / support / workflow / operations</code>
</div>

---

## Equipe

<table>
  <thead>
    <tr>
      <th align="left">Nome</th>
      <th align="left">RM</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Edmilson Gato Junior</td>
      <td>RM377042</td>
    </tr>
    <tr>
      <td>Elton Rodrigues de Melo Leite</td>
      <td>RM377314</td>
    </tr>
    <tr>
      <td>Raphael Flores da Costa</td>
      <td>RM379021</td>
    </tr>
    <tr>
      <td>Yuri de França Cordeiro</td>
      <td>RM379108</td>
    </tr>
  </tbody>
</table>

---

## Sobre o projeto

O HelpDesk Lite é uma solução pensada para organizar e otimizar o atendimento de demandas internas em uma empresa. Ele centraliza solicitações de diferentes áreas, como TI, Facilities e RH, em um único ambiente para facilitar acompanhamento, triagem e resolução.

A proposta do projeto simula um ambiente real de suporte interno, com diferentes perfis de acesso, regras de negócio e fluxo claro de atendimento, cobrindo toda a jornada desde a abertura do chamado até a sua conclusão.

---

## Ideia do sistema

Imagine um ambiente em que todos os pedidos internos são registrados em um único lugar, sem perder controle por e-mail, planilha ou comunicação informal. O HelpDesk Lite resolve isso ao permitir que:

- usuários abram chamados de forma simples e organizada;
- atendentes assumam e respondam solicitações;
- administradores acompanhem a operação geral;
- a equipe tenha mais visibilidade sobre o andamento dos casos.

Essa abordagem melhora a organização, reduz retrabalho e acelera a resolução dos problemas.

---

## Fluxo principal

O sistema foi pensado para funcionar com três papéis principais:

### 1. Solicitante
- abre chamados;
- acompanha o status das demandas;
- comenta quando necessário;
- confirma a resolução.

### 2. Atendente
- visualiza chamados ativos;
- assume o atendimento;
- atualiza o status;
- responde e resolve a solicitação.

### 3. Administrador
- gerencia usuários e categorias;
- acompanha o panorama geral do suporte;
- mantém o ambiente organizado e controlado.

---

## Objetivos

- centralizar demandas internas em um único lugar;
- facilitar o acompanhamento de cada chamado;
- organizar o fluxo de atendimento por perfil;
- manter maior controle sobre prioridades e responsabilidades;
- oferecer uma visão geral do desempenho do suporte.

---

## Organização do repositório

A estrutura atual do projeto está organizada da seguinte forma:

```text
HelpDesk.Lite.FIAP/
├── README.md
├── Atividade 1/
│   └── 01_Arquitetura_HelpDesk_Lite v3.md
├── Atividade 2/
│   ├── 02_Prompt_Geracao_Contexto v3.md
│   └── 03_Prompt_Implementacao v3.md
├── backend/          # API e regras de negócio (Java 21 + Spring Boot 3)
├── frontend/         # interface do sistema (Vue 3 + Quasar 2)
├── docs/             # documentação complementar
│   ├── ARQUITETURA.md
│   └── MVP.md        # como rodar, demonstrar e operar o MVP
├── .gitignore
├── .ai/              # arquivos de contexto e padronização
└── assets/           # recursos visuais e materiais do projeto
```

### Organização por momento

- `Atividade 1/`: reúne a base da arquitetura e o desenho do sistema;
- `Atividade 2/`: contém os prompts de geração de contexto e de implementação;
- `backend/`: responsável pela lógica e pelo processamento do sistema;
- `frontend/`: a camada de interação com o usuário;
- `docs/`: guarda registros e documentação complementar;
- `.ai/`: armazena regras e contexto do projeto.

Essa organização ajuda a manter o projeto em evolução com clareza e disciplina.

---

## Como rodar

O MVP roda 100% local, sem Docker e sem banco para instalar. São dois terminais:

```bash
cd backend && ./mvnw spring-boot:run
```

```bash
cd frontend && npm install && npx quasar dev
```

A interface abre em `http://localhost:9000` e a API em `http://localhost:8080`
(Swagger em `/swagger-ui.html`). Na tela de login, escolha entre **Ana** (solicitante),
**Bruno** (atendente) ou **Carla** (administradora) para ver o sistema por cada perfil.

O passo a passo completo — pré-requisitos, usuários de teste, roteiro de demonstração,
endpoints, testes e guia de operação — está em **[docs/MVP.md](docs/MVP.md)**.

---

## Documentação de apoio

- [Atividade 1/01_Arquitetura_HelpDesk_Lite v3.md](Atividade%201/01_Arquitetura_HelpDesk_Lite%20v3.md)
- [Atividade 2/02_Prompt_Geracao_Contexto v3.md](Atividade%202/02_Prompt_Geracao_Contexto%20v3.md)
- [Atividade 2/03_Prompt_Implementacao v3.md](Atividade%202/03_Prompt_Implementacao%20v3.md)
- [docs/MVP.md](docs/MVP.md) — guia de execução, demonstração e operação do MVP
- [.ai/](.ai/) — contexto do projeto: padrões, arquitetura (ADRs), stack e regras de negócio

---

## Conclusão

O HelpDesk Lite é uma proposta de sistema de suporte interno moderno, prática e funcional. O objetivo principal é simular um ambiente real de atendimento, com organização, rastreabilidade e clareza no fluxo de chamados.

Além disso, o projeto também representa um trabalho de colaboração e arquitetura, unindo teoria e prática em uma solução pensada para resolver problemas reais de suporte dentro de uma empresa.

---

> “Todo o conteúdo deste projeto foi criado com apoio e colaboração de inteligência artificial, com revisão e adaptação humana para garantir clareza, organização e qualidade.”
