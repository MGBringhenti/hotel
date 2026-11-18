# Hotel — sistema de gestão de reservas

Projeto de gestão hoteleira com interface web em português, APIs separadas por responsabilidade e notificações por eventos. Permite administrar clientes, usuários, quartos e tipos de quarto, consultar disponibilidade em um calendário e acompanhar reservas do cadastro à conclusão.

O ambiente completo pode ser iniciado com Docker Compose. O envio de e-mails é demonstrado pelo Mailpit: as mensagens ficam disponíveis em uma caixa de entrada local, sem depender de um provedor real.

## Conteúdo

- [Visão geral e arquitetura](#visão-geral-e-arquitetura)
- [Funcionalidades e perfis](#funcionalidades-e-perfis)
- [Como iniciar](#como-iniciar)
- [Como usar o sistema](#como-usar-o-sistema)
- [Comandos do dia a dia](#comandos-do-dia-a-dia)
- [APIs e autenticação](#apis-e-autenticação)
- [Dados e notificações](#dados-e-notificações)
- [Desenvolvimento e testes](#desenvolvimento-e-testes)
- [MCP opcional](#mcp-opcional)
- [Diagnóstico rápido](#diagnóstico-rápido)
- [Documentação complementar](#documentação-complementar)

## Visão geral e arquitetura

| Componente | Tecnologias | Responsabilidade |
|---|---|---|
| Front-end | React, Vite, React Router, CSS | Interface, navegação por perfil, formulários e calendário |
| cliente-service | Java, Spring Boot, Spring Security, JPA | Clientes, usuários, login e recuperação de senha |
| quarto-service | Java, Spring Boot, JPA | Quartos, tipos e preços de diárias |
| reserva-service | Java, Spring Boot, JPA | Reservas, disponibilidade, calendário e publicação de eventos |
| notificacoes-service | Java, Spring Boot, Spring Kafka, SMTP | Consumo de eventos e envio de notificações |
| PostgreSQL | Banco relacional e Flyway | Persistência e evolução versionada dos esquemas |
| Kafka | Mensageria | Transporte dos eventos de reserva |
| Mailpit | Servidor SMTP de desenvolvimento | Recebimento e visualização local dos e-mails |
| Nginx | Servidor web e proxy | Publicação do front-end e encaminhamento às APIs no Docker |
| hotel-mcp | Spring AI / MCP | Integração opcional de ferramentas com assistentes de IA |

No ambiente Docker, o navegador acessa o Nginx, que encaminha as chamadas às três APIs HTTP. O serviço de reservas consulta as APIs de clientes e quartos e registra eventos para o Kafka. O serviço de notificações consome esses eventos, consulta o destinatário e envia a mensagem ao Mailpit.

Cada serviço possui seu próprio banco lógico. No ambiente local completo, os quatro bancos compartilham uma instância PostgreSQL.

```text
Hotel/
├── README.md                 # Visão geral e uso do projeto
├── compose.yaml              # Ambiente completo
├── iniciar.ps1               # Preparação e inicialização no Windows
├── .env.example              # Referência das configurações locais
├── docker/                   # Imagens Java, chaves, bancos e guia Docker
├── front-end/                # Interface React, testes e guia do front-end
└── back-end/
    ├── cliente-service/
    ├── quarto-service/
    ├── reserva-service/
    ├── notificacoes-service/
    ├── hotel-mcp/             # Integração opcional
    ├── docs/sql/             # Apoio ao reprocessamento de notificações
    └── compose.yaml          # Apenas Kafka e Mailpit para execução manual
```

**Os dois arquivos Compose têm finalidades diferentes.** Para iniciar tudo junto, use o da raiz. O de `back-end/` atende ao desenvolvimento com as aplicações executadas separadamente.

## Funcionalidades e perfis

| Perfil | Principais possibilidades |
|---|---|
| ADMIN | Administrar usuários e vínculos, clientes, quartos, tipos e reservas; alterar situações dos cadastros |
| RECEPCIONISTA | Cadastrar e editar clientes e quartos, consultar tipos, criar e acompanhar reservas de clientes |
| CLIENTE | Consultar disponibilidade, criar reservas próprias, acompanhar e cancelar suas reservas conforme as regras |

Todos os perfis podem trocar a própria senha e solicitar recuperação de acesso. As APIs verificam as permissões e a propriedade dos dados, independentemente dos menus apresentados na interface.

### Cadastros e interface

- Clientes com nome, CPF, e-mail, telefone e situação.
- Usuários com login, senha protegida por BCrypt, perfil e e-mail de recuperação.
- Vínculo entre o cadastro do cliente e sua conta de acesso.
- Quartos com número, tipo e situação `DISPONIVEL` ou `INATIVO`.
- Tipos de quarto com nome, capacidade e preço da diária.
- Listagens e seletores paginados, formulários e interface responsiva.
- Calendário por quarto com períodos ocupados e seleção de entrada e saída.

Não há cadastro público de contas. A equipe cadastra o cliente e o administrador cria e vincula o usuário. O perfil é escolhido na criação da conta; a edição atual permite alterar login e e-mail, não o perfil.

### Regras de reserva

- Uma reserva começa como `PENDENTE`.
- As transições são `PENDENTE → CONFIRMADA/CANCELADA` e `CONFIRMADA → CONCLUIDA/CANCELADA`.
- Apenas reservas pendentes podem ter cliente, quarto e datas alterados pela equipe.
- O cliente pode cancelar somente suas próprias reservas, respeitando a regra de datas.
- A entrada não pode estar no passado e a saída deve ser posterior à entrada.
- O cancelamento deve ocorrer antes da entrada; a confirmação, até a data de entrada; a conclusão, a partir da saída.
- A data de saída não ocupa uma diária: outra reserva pode começar nesse mesmo dia.
- O total é calculado no servidor: quantidade de diárias × diária contratada.
- Alterar as datas preserva a diária contratada; trocar o quarto utiliza a diária atual do novo quarto.
- O banco impede reservas sobrepostas para o mesmo quarto nos estados que bloqueiam o período.
- O controle de versão rejeita atualizações baseadas em dados desatualizados.

Consultar disponibilidade não bloqueia o quarto. A disponibilidade é conferida novamente ao gravar a reserva. As regras de data usam o fuso `America/Sao_Paulo`.

## Como iniciar

### Pré-requisitos

Para o ambiente completo no Windows:

1. Docker Desktop instalado e iniciado, usando contêineres Linux.
2. PowerShell e uma cópia do projeto.
3. Acesso à internet na primeira compilação para baixar imagens e dependências.

Não é necessário instalar Java, Node.js, Maven ou PostgreSQL no computador para executar tudo pelo Docker. Esses componentes são fornecidos pelas imagens.

### Primeira execução

No PowerShell, entre na pasta em que salvou o projeto. Exemplo:

```powershell
cd C:\dev\Hotel
.\iniciar.ps1
```

O script:

1. Cria o `.env` da raiz com senhas aleatórias, caso ainda não exista.
2. Compila as imagens do front-end e dos serviços Java.
3. Inicia PostgreSQL, Kafka e Mailpit.
4. Prepara os bancos, os tópicos Kafka e as chaves RSA.
5. Inicia as aplicações; o Flyway aplica as migrações de cada serviço.
6. Cria o administrador inicial quando necessário e aguarda os serviços ficarem prontos.

A primeira execução pode levar vários minutos. As configurações e chaves existentes são preservadas nas próximas inicializações.

Se o PowerShell bloquear a execução do arquivo:

```powershell
powershell -ExecutionPolicy Bypass -File .\iniciar.ps1
```

### Acesso e credenciais

| Recurso | Endereço padrão |
|---|---|
| Sistema Hotel | http://localhost:8080 |
| Caixa de e-mails Mailpit | http://localhost:8025 |
| PostgreSQL do Docker | `localhost:5433` |

Abra o `.env` da raiz no editor e consulte `ADMIN_INICIAL_LOGIN` e `ADMIN_INICIAL_SENHA` para o primeiro login. Não existe uma senha fixa compartilhada pelo projeto. Depois do acesso, use **Minha conta** para trocar a senha.

Alterar `ADMIN_INICIAL_SENHA` no `.env` não muda a senha de uma conta já criada. O bootstrap inicial não é um mecanismo de redefinição de senha.

As portas podem ser ajustadas por `HOTEL_PORT`, `MAILPIT_PORT` e `POSTGRES_PORT` no `.env`. Ele contém credenciais locais e não deve ser enviado ao Git.

## Como usar o sistema

### Preparar os primeiros cadastros

Com uma conta de administrador:

1. Cadastre um **tipo de quarto**, informando nome, capacidade e preço da diária.
2. Cadastre um **quarto**, informando seu número e selecionando o tipo criado.
3. Cadastre um **cliente**, com seus dados e e-mail para notificações.
4. Consulte a **disponibilidade**, escolha o quarto e as datas de entrada e saída.
5. Crie a **reserva**, selecionando também o cliente quando estiver usando uma conta da equipe.
6. Acompanhe a reserva na listagem e utilize as ações permitidas para confirmar, cancelar ou concluir.
7. Abra o Mailpit para conferir as notificações geradas.

Uma instalação nova não importa os dados do PostgreSQL instalado no Windows. Os cadastros devem ser feitos pela interface ou pelas APIs.

### Dar acesso a um cliente

1. Em **Usuários**, o administrador cria uma conta com perfil `CLIENTE` e e-mail de recuperação.
2. Em **Clientes**, vincula o cadastro correspondente à conta criada.
3. Mantém ambos ativos para permitir o login do cliente.
4. O cliente entra com seu login e senha e passa a criar e acompanhar suas próprias reservas.

O e-mail de recuperação pertence ao usuário; o e-mail utilizado nas notificações de reserva pertence ao cadastro do cliente.

### Login e recuperação de senha

Na interface, basta informar login e senha. O front-end recebe o JWT e o envia automaticamente às APIs; não é necessário copiar tokens manualmente.

A sessão fica em memória: recarregar a página exige novo login. O JWT tem validade de duas horas. A interface também encerra a sessão quando identifica expiração ou uma resposta de autenticação inválida.

Para recuperar o acesso:

1. Use a opção de recuperação de senha e informe o e-mail de recuperação da conta.
2. Abra o Mailpit em http://localhost:8025.
3. Copie o código recebido e informe-o na tela de redefinição junto da nova senha.

O código é de uso único e vale 30 minutos. A resposta ao pedido é genérica, mesmo quando a conta não existe. No escopo deste projeto, os e-mails ficam no Mailpit e não chegam a uma caixa externa real.

## Comandos do dia a dia

Execute na raiz do projeto:

```powershell
# Iniciar e reconstruir imagens após alterações no código
.\iniciar.ps1

# Iniciar novamente usando as imagens já compiladas
.\iniciar.ps1 -NaoReconstruir

# Conferir os serviços
 docker compose ps

# Acompanhar os logs; Ctrl+C encerra apenas a visualização
 docker compose logs -f --tail 80

# Consultar um serviço específico
 docker compose logs --tail 100 reserva-service

# Parar o ambiente preservando os volumes
 docker compose down
```

Fechar o terminal não para os contêineres. Após reiniciar o computador, abra o Docker Desktop e use o comando de inicialização novamente.

**Não acrescente `-v` ao comando de parada se quiser manter os dados e as chaves.** Os volumes armazenam bancos, eventos, mensagens e chaves JWT.

## APIs e autenticação

No Docker completo, o Nginx publica as APIs pela mesma porta do site:

| API | Prefixo externo | Exemplos de rotas |
|---|---|---|
| Clientes e autenticação | `/api/clientes` | `/auth/login`, `/clientes`, `/usuarios` |
| Quartos | `/api/quartos` | `/quarto`, `/tipoQuarto` |
| Reservas | `/api/reservas` | `/reservas`, `/reservas/minhas`, `/reservas/calendario`, `/reservas/disponibilidade` |

Exemplo de login no Postman:

```http
POST http://localhost:8080/api/clientes/auth/login
Content-Type: application/json

{
  "login": "SEU_LOGIN",
  "senha": "SUA_SENHA"
}
```

Use o `accessToken` da resposta como **Bearer Token** nas requisições protegidas. Por exemplo, a listagem de quartos usa `GET http://localhost:8080/api/quartos/quarto`.

As listagens paginadas usam `pagina` (começa em zero) e `tamanho` (padrão 20, máximo 100). Calendário e disponibilidade recebem `idQuarto`, `dataInicio` e `dataFinal`, com datas `YYYY-MM-DD`.

O cliente-service assina JWT com a chave privada RSA; as APIs validam com a chave pública. Somente o serviço de autenticação recebe a chave privada. O serviço de notificações obtém seu próprio token de serviço com um segredo compartilhado, sem depender de um login de administrador.

Trocar a senha ou sair da interface não revoga imediatamente JWTs já emitidos. Eles permanecem válidos até expirar. Não há refresh token nem revogação global nesta implementação.

## Dados e notificações

### PostgreSQL e Flyway

| Banco | Dados principais |
|---|---|
| `hotel_clientes` | Clientes, usuários e recuperação de senha |
| `hotel_quarto` | Quartos e tipos de quarto |
| `hotel_reservas` | Reservas e eventos pendentes de publicação |
| `hotel_notificacoes` | Eventos recebidos e processamento de notificações |

O Docker prepara os bancos em um volume novo; o Flyway de cada serviço cria e atualiza suas tabelas. Hibernate valida o esquema. Mudanças estruturais devem ser feitas por novas migrações, sem editar migrações que já foram aplicadas.

Para conectar o pgAdmin ao ambiente Docker, use host `localhost`, porta `5433`, usuário `hotel` e a senha de `POSTGRES_PASSWORD` no `.env`. Alterar essa variável não troca automaticamente a senha de um banco já inicializado.

### Eventos e e-mails

A reserva e seu evento são gravados na mesma transação por meio de uma **outbox**. Um publicador envia os eventos pendentes ao tópico `hotel.reservas.v1`; o notificacoes-service registra o recebimento e processa o envio ao Mailpit.

Há deduplicação por identificador de evento, tentativas de processamento e uma fila de mensagens com falha (`hotel.reservas.v1.DLT`). Falhas posteriores no envio de e-mail possuem seu próprio controle de tentativas no banco de notificações.

Para investigar, consulte os logs de reserva e notificações e verifique se o evento está pendente na outbox, na DLT ou na tabela de notificações. O [script de reprocessamento](back-end/docs/sql/reprocessar_notificacao.sql) auxilia no reagendamento de uma notificação que atingiu o limite de tentativas, após corrigir a causa. Não apague a deduplicação nem republique eventos indiscriminadamente: o envio SMTP não tem garantia de exatamente uma vez.

A recuperação de senha usa envio direto pelo cliente-service após a transação, sem passar pelo Kafka.

## Desenvolvimento e testes

### Desenvolver o front-end

Para executar o Vite fora do Docker, instale Node.js compatível com o projeto (a imagem de compilação utiliza Node 24) e execute:

```powershell
cd C:\dev\Hotel\front-end
npm ci
npm run dev
```

O endereço padrão é http://localhost:5173. Por padrão, o front-end procura as APIs em `localhost:9001`, `9002` e `9003`.

Se as APIs estiverem no Docker completo, configure `front-end/.env.local` com:

```dotenv
VITE_CLIENTES_URL=http://localhost:8080/api/clientes
VITE_QUARTOS_URL=http://localhost:8080/api/quartos
VITE_RESERVAS_URL=http://localhost:8080/api/reservas
```

Nesse caso, inclua `http://localhost:5173` nas origens de `FRONTEND_ORIGINS` das três APIs no Compose e recrie os serviços afetados. Reinicie o Vite após alterar as variáveis. Variáveis `VITE_` são públicas e não devem conter segredos.

Para desenvolvimento Java fora do Docker, cada serviço tem seu próprio Maven Wrapper e exige banco, credenciais e dependências configurados. O `cliente-service` precisa das chaves privada e pública; quarto e reserva usam apenas a pública. O Compose de `back-end/` fornece somente Kafka e Mailpit. Não execute os dois ambientes simultaneamente nas mesmas portas.

### Verificações do front-end

Na pasta `front-end/`:

```powershell
npm run lint
npm run build
npm test
npm run test:e2e
```

Os testes de navegador usam Playwright com Microsoft Edge, iniciam o Vite na porta 4175 e simulam as APIs. Não substituem a validação integrada com os serviços reais.

### Validações registradas

| Rodada | Resultado registrado |
|---|---|
| Back-end — 13/11/2026 | 53 testes aprovados nos quatro serviços, com PostgreSQL temporário e bancos isolados |
| Front-end — 17/11/2026 | Lint e build aprovados, 8 testes de regras e 15 cenários de navegador aprovados |
| Integração Docker — 17/11/2026 | Login pelo navegador, cadastros, disponibilidade, criação/confirmacão/cancelamento de reserva, listagem real e notificação via Kafka até o Mailpit |

Esses resultados são registros das rodadas anteriores, não uma execução a cada atualização deste documento. A integração validada não cobre todas as combinações de perfis, concorrência e recuperação de falhas em ambiente real.

## MCP opcional

O módulo `hotel-mcp` expõe ferramentas como `listar_quartos` e `consultar_reserva_por_id` para clientes compatíveis com Model Context Protocol. Ele chama as APIs autenticadas e respeita as permissões do usuário cujo token foi configurado.

Não é necessário iniciar o MCP para usar o Hotel e ele não faz parte do Compose completo. A aplicação está configurada para HTTP Streamable na porta 9004; a configuração legada em `back-end/.vscode/mcp.json` usa STDIO e precisa ser alinhada antes de conectar esse cliente.

O token é fornecido por `HOTEL_API_TOKEN`. Ao expirar, é necessário obter outro token, atualizar o ambiente e reiniciar o processo MCP. No uso com o Docker completo, as URLs das APIs devem apontar para os prefixos do Nginx, por meio de `HOTEL_QUARTO_URL` e `HOTEL_RESERVA_URL`. Mantenha essa integração restrita ao ambiente local; o token das APIs não protege automaticamente a conexão entre o assistente e o servidor MCP.

## Diagnóstico rápido

| Sintoma | O que verificar |
|---|---|
| Docker não conecta | Abra o Docker Desktop e aguarde o mecanismo iniciar |
| Site não abre | Execute `docker compose ps` e confira a porta de `HOTEL_PORT` |
| Compilação demora ou falha por memória | Consulte os logs; o script compila sequencialmente para reduzir consumo |
| Porta ocupada | Ajuste as portas no `.env` e evite iniciar o Compose antigo simultaneamente |
| Login falha | Confira as credenciais atuais; as do `.env` só criam o administrador inicial |
| Cliente não consegue entrar | Verifique conta ativa, cadastro ativo e vínculo entre ambos |
| Requisição retorna 401 | Faça novo login; o token pode estar ausente ou expirado |
| Requisição retorna 409 | Atualize os dados e confira versão, datas e disponibilidade |
| E-mail não aparece | Confira Mailpit e logs de cliente ou notificações, conforme o tipo de mensagem |
| Dados antigos não aparecem | O banco do Docker é separado do PostgreSQL local anterior |
| `chaves`, `topicos` ou `kafka-volume` aparecem encerrados | Saída com código zero é esperada: são tarefas de preparação |

## Escopo e limites

Este projeto demonstra gestão de hotel em ambiente local. Não inclui pagamentos, provedor real de e-mail, cadastro público, refresh token ou infraestrutura de produção. As interfaces do Compose ficam publicadas apenas em `127.0.0.1`.

As verificações entre APIs não formam uma transação distribuída. A proteção contra sobreposição de reservas é garantida no banco de reservas, mas isso não torna atômicas todas as alterações simultâneas entre os serviços.

## Documentação complementar

- [Guia Docker](docker/README.md): inicialização, configurações, volumes, portas e operação do ambiente completo.
- [Guia do front-end](front-end/README.md): organização da interface, decisões, comandos e testes.
- [Compose completo](compose.yaml): definição dos serviços e das dependências.
- [Script de inicialização](iniciar.ps1): preparação automática do ambiente Windows.
- [Reprocessamento de notificações](back-end/docs/sql/reprocessar_notificacao.sql): procedimento SQL para falhas de envio.

Este README é o ponto de entrada do projeto; os guias específicos aprofundam cada área.
