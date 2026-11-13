# Hotel — README

Atualizado em 13/11/2026. A rodada automatizada executou 53 testes nos quatro serviços, todos aprovados, com PostgreSQL 18.6 temporário e bancos isolados. Consulte a seção 13 para evidências, reprodução e limites da validação. As instruções operacionais continuam distintas dos cenários efetivamente testados.

Este README concentra a documentação vigente do back-end, da operação e do MCP. Os quatro guias anteriores foram consolidados; instruções históricas superadas não devem ser reaplicadas.

## Navegação

- [Escopo e checklist](#1-escopo-fechado-e-checklist-de-entrega)
- [Componentes](#2-componentes-e-endereços)
- [Regras do hotel](#3-funcionalidades-e-regras-implementadas)
- [Autenticação e recuperação](#4-tokens-senhas-e-recuperação)
- [Rotas e permissões](#5-rotas-e-permissões)
- [Inicialização local](#6-configuração-e-inicialização-local)
- [Banco e Flyway](#7-banco-de-dados-e-flyway)
- [Eventos](#8-eventos-mailpit-e-recuperação-operacional)
- [Validação final](#9-validação-final--somente-após-a-implementação)
- [Diagnóstico](#10-diagnóstico-rápido)
- [Reprocessamento e DLT](#operacao-notificacoes)
- [MCP e reutilização](#mcp)
- [Resultados dos testes](#13-resultados-dos-testes--13112026)

## 1. Escopo fechado e checklist de entrega

| Item | Entrega | Estado nesta revisão |
|---|---|---|
| 1 | CORS em cliente, quarto e reserva | Implementado; preflight HTTP aprovado nas três APIs; front-end ainda não integrado |
| 2 | E-mail de recuperação alinhado à validade de 30 minutos | Implementado |
| 3 | ClienteClient retorna 502 para resposta inválida da API de clientes | Implementado |
| 4 | Documentação atual, inicialização, permissões e recuperação de falhas | Documentado neste README |
| 5 | Compilar módulos e completar testes automatizados essenciais | Quatro módulos compilados; 53 testes aprovados; cobertura não exaustiva |
| 6 | Validar Flyway em bancos novos e no ambiente existente | Bancos novos aprovados; atualização do ambiente existente pendente |
| 7 | Validar fluxos HTTP, permissões e concorrência | CORS, autenticação, redefinição e concorrência aprovados; matriz completa ponta a ponta pendente |
| 8 | Validar Kafka, notificações, Mailpit e reprocessamento | Regras de envio, deduplicação e script SQL aprovados; Kafka/SMTP reais e DLT pendentes |

A ordem acordada é concluir 1–4 antes de executar 5–8. Defeitos encontrados nos testes pertencem a esses itens; melhorias opcionais não ampliam automaticamente o escopo.

Ficam fora desta entrega: provedor real de e-mail, revogação imediata de JWT, refresh token, auth-service separado, gateway, Redis, pagamentos, novos relatórios e infraestrutura de produção. Contas são criadas pela equipe autorizada; cadastro público não está implementado. O MCP é uma integração separada, não um requisito para encerrar as APIs.

## 2. Componentes e endereços

| Componente | Endereço local | Banco / responsabilidade |
|---|---|---|
| cliente-service | http://localhost:9001 | hotel_clientes; usuários, clientes e autenticação |
| quarto-service | http://localhost:9002 | hotel_quarto; quartos e tipos |
| reserva-service | http://localhost:9003 | hotel_reservas; reservas, calendário e outbox |
| notificacoes-service | Sem servidor HTTP | hotel_notificacoes; consumo Kafka e envio SMTP |
| Kafka | localhost:9092 para aplicações no Windows | Eventos de reserva |
| Mailpit SMTP | localhost:1025 | Recebimento dos e-mails |
| Mailpit interface | http://localhost:8025 | Visualização dos e-mails |
| PostgreSQL | localhost:5432 | Instalado separadamente do Compose |
| hotel-mcp, opcional | Configuração atual: HTTP na porta 9004 | Adaptador das APIs; conferir a seção MCP antes de usar |

O Compose atual inicia Kafka e Mailpit, não inicia PostgreSQL nem as aplicações Java. No contêiner Kafka, os comandos abaixo usam kafka:19092; no Windows, as aplicações usam localhost:9092.

Java: os projetos declaram nível 17; o ambiente de desenvolvimento utiliza JDK 25. Use o Maven Wrapper de cada módulo. Não existe um wrapper na raiz para executar os quatro serviços juntos.

## 3. Funcionalidades e regras implementadas

### Identidade

- Usuário possui login, senha BCrypt, função, situação, versão e e-mail de recuperação.
- Funções humanas: ADMIN, RECEPCIONISTA e CLIENTE.
- Cliente pode ser vinculado a uma conta; login de CLIENTE exige vínculo com cliente ativo.
- DTOs de resposta de usuário não devem expor senha.
- ADMIN cria contas. A equipe cadastra clientes e o ADMIN administra o vínculo de usuário.
- Desativação preserva dados históricos.

### Reservas

- Equipe cria para um cliente informado; CLIENTE cria por POST /reservas/minhas usando sua identidade validada.
- CLIENTE lista e consulta somente suas próprias reservas e pode cancelá-las conforme a regra de datas.
- Reserva nasce PENDENTE. Transições: PENDENTE → CONFIRMADA/CANCELADA; CONFIRMADA → CONCLUIDA/CANCELADA.
- Somente PENDENTE permite alteração de cliente, quarto e datas pela equipe.
- Entrada inclusiva e saída exclusiva: uma saída no dia 12 permite outra entrada no dia 12.
- Entrada não pode estar no passado; saída deve ser posterior à entrada.
- Cancelamento deve ocorrer antes da entrada; confirmação não pode ocorrer após a data inicial; conclusão não pode ocorrer antes da saída.
- Cliente deve estar ativo e quarto DISPONIVEL ao criar/alterar e ao confirmar.
- Quarto tem status DISPONIVEL ou INATIVO; MANUTENCAO foi removido.
- Valor total = número de diárias × diária contratada, com BigDecimal.
- Alterar datas mantém a diária contratada; trocar o quarto usa a diária atual do novo quarto.
- Alterar apenas status não recalcula preço.
- Controle de versão rejeita alterações concorrentes desatualizadas.
- A exclusão GiST no PostgreSQL impede sobreposição do mesmo quarto para PENDENTE, CONFIRMADA e CONCLUIDA.
- Calendário consulta até 366 dias por vez e retorna períodos ocupados, sem dados pessoais de outros hóspedes.
- As consultas de disponibilidade não garantem uma vaga até o POST ser confirmado; outra reserva pode ocorrer entre consultar e gravar.

A consulta HTTP de cliente/quarto e a gravação da reserva não formam uma transação distribuída. Desativação concorrente de outro serviço não é bloqueada atomicamente. A proteção GiST cobre sobreposição no banco de reservas, não todas as alterações entre serviços. Essa limitação deve ser considerada na validação e não representa uma promessa de consistência global.

## 4. Tokens, senhas e recuperação

### Login humano

POST /auth/login recebe login e senha. O cliente-service assina JWT RS256 com a chave privada; as APIs validam com a chave pública e issuer hotel-auth.

O JWT contém identidade do usuário, função e, para CLIENTE, clienteId. Prazo configurado: 7200 segundos, duas horas. A resposta contém accessToken, tokenType e expiresIn.

Política escolhida: tokens já emitidos continuam válidos até expirar mesmo após troca/recuperação de senha ou alteração de conta. Não há revogação imediata nem refresh token. Apagar o token no front-end faz logout local, mas não invalida uma cópia. Algumas operações consultam cadastros ativos novamente; isso não equivale a revogação global.

PATCH /auth/senha exige login, senhaAtual e senhaNova. A senha nova deve diferir da atual, ter 8–50 caracteres e no máximo 72 bytes UTF-8.

### Recuperação para todos os perfis

1. POST /auth/recuperacao-senha recebe emailRecuperacao.
2. A resposta é genérica, inclusive para conta inexistente, inativa ou pedido dentro do intervalo de 60 segundos.
3. Para conta elegível, gera token aleatório de 32 bytes; salva apenas SHA-256, usuário, versão e datas.
4. Após confirmar a transação, envia o token ao Mailpit diretamente pelo cliente-service, sem Kafka.
5. O token vale 30 minutos.
6. POST /auth/recuperacao-senha/redefinir recebe token e senhaNova.
7. O serviço confere validade, utilização, situação e versão da conta; grava BCrypt e utilização na mesma transação.
8. A alteração da versão invalida outros pedidos anteriores de recuperação; não invalida JWTs de login.

Exemplo de solicitação:

```json
{"emailRecuperacao":"admin@hotel.test"}
```

Exemplo de redefinição — substituir os marcadores, não usar como credenciais reais:

```json
{"token":"TOKEN_RECEBIDO_NO_MAILPIT","senhaNova":"NOVA_SENHA_ESCOLHIDA"}
```

Limitações assumidas: envio após commit não tem fila durável nem repetição automática; se o processo cair ou o SMTP falhar, pode ser necessário solicitar novamente após o intervalo. O intervalo é por conta, não um rate limit global por IP. A resposta genérica não garante tempo de resposta idêntico. O e-mail é informado pela administração; não há fluxo separado de comprovação de posse do endereço.

Usuários antigos podem estar sem emailRecuperacao. Um ADMIN deve consultar a versão atual e preencher o endereço pelo PUT /usuarios/{id}, enviando login, emailRecuperacao e versao. Não inventar endereços para contas reais.

### Autenticação entre serviços

notificacoes-service usa clientId notificacoes-service e o segredo compartilhado para POST /auth/servicos/token. O token dura 300 segundos e possui função SERVICO_NOTIFICACOES. Esse endpoint é próprio do projeto, não uma implementação completa de servidor OAuth2.

O cliente interno mantém o token em cache, renova antes do vencimento e tenta renovar uma vez após 401. Não utiliza o JWT de um administrador.

## 5. Rotas e permissões

Equipe = ADMIN ou RECEPCIONISTA. “Todos” abaixo significa os três perfis humanos autenticados. Permissão HTTP não substitui as validações de propriedade, situação e versão no service. Rotas não previstas são negadas.

### cliente-service

| Método e rota | Acesso |
|---|---|
| POST /auth/login | Público |
| POST /auth/recuperacao-senha | Público |
| POST /auth/recuperacao-senha/redefinir | Público; exige token de recuperação válido no corpo |
| PATCH /auth/senha | Todos; própria senha |
| POST /auth/servicos/token | Público na camada HTTP; exige credenciais do serviço |
| GET /usuarios e /usuarios/{id} | ADMIN |
| POST /usuarios | ADMIN |
| PUT /usuarios/{id} | ADMIN |
| PATCH /usuarios/{id}/situacao | ADMIN |
| GET /clientes | Equipe |
| GET /clientes/{id} | Equipe ou CLIENTE proprietário |
| POST /clientes | Equipe |
| PUT /clientes/{id} | Equipe |
| PUT /clientes/{id}/usuario | ADMIN |
| PATCH /clientes/{id}/situacao | ADMIN |
| GET /interno/clientes/{id}/destinatario | SERVICO_NOTIFICACOES |

### quarto-service

| Método e rota | Acesso |
|---|---|
| GET /quarto e /quarto/{id} | Todos |
| POST /quarto | Equipe |
| PUT /quarto/{id} | Equipe |
| PATCH /quarto/{id}/status | ADMIN |
| GET /tipoQuarto e /tipoQuarto/{id} | Todos |
| POST /tipoQuarto | ADMIN |
| PUT /tipoQuarto/{id} | ADMIN |
| DELETE /tipoQuarto/{id} | ADMIN; restrições de vínculo continuam aplicáveis |

### reserva-service

| Método e rota | Acesso |
|---|---|
| GET /reservas | Equipe: todas; CLIENTE: próprias |
| GET /reservas/{id} | Equipe ou proprietário |
| POST /reservas | Equipe |
| POST /reservas/minhas | CLIENTE |
| PUT /reservas/{id} | Equipe |
| PATCH /reservas/{id}/status | Equipe ou proprietário; cliente apenas cancela |
| GET /reservas/disponibilidade | Todos |
| GET /reservas/calendario | Todos |

Disponibilidade/calendário usam idQuarto, dataInicio e dataFinal na query, com datas YYYY-MM-DD. Listagens paginadas usam pagina (inicial 0) e tamanho (padrão 20, máximo 100).

### CORS e respostas

As três APIs HTTP configuram origem padrão http://localhost:5173, substituível por FRONTEND_ORIGINS (origens separadas por vírgula). Métodos permitidos: GET, POST, PUT, PATCH, DELETE e OPTIONS. Headers aceitos: Authorization, Content-Type e Accept; Location é exposto para leitura, quando presente na resposta. Sem cookies de autenticação; JWT enviado explicitamente em Authorization.

OPTIONS de preflight é tratado pelo CORS integrado ao Spring Security. CORS não autentica e não limita clientes fora do navegador.

Códigos comuns: 400 dados inválidos, 401 autenticação inválida/ausente, 403 acesso proibido, 404 recurso não encontrado ou não visível, 409 conflito de versão/integridade/regra, 502 resposta inválida de dependência e 503 falha de comunicação. Login retorna 200; criação normalmente 201; troca/redefinição de senha retorna 204. Nem todo erro de segurança passa pelo GlobalExceptionHandler.

## 6. Configuração e inicialização local

Os comandos desta seção ficam preparados para a etapa de validação. Não foram executados durante a atualização do guia.

### Infraestrutura

Abra o Docker Desktop e confirme que PostgreSQL está iniciado. No PowerShell:

```powershell
Set-Location "C:\Users\mgbri\OneDrive\Documentos\Hotel"
docker compose up -d kafka mailpit
docker compose ps
```

Se os tópicos ainda não existirem, crie ambos com três partições. A DLT precisa ter pelo menos as mesmas partições do tópico principal:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 --create --if-not-exists --topic hotel.reservas.v1 --partitions 3 --replication-factor 1
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 --create --if-not-exists --topic hotel.reservas.v1.DLT --partitions 3 --replication-factor 1
```

--if-not-exists não corrige partições de um tópico que já existe. O código não cria tópicos automaticamente.

### Credenciais em cada terminal

Abra um terminal por serviço. Execute este bloco em cada terminal; pode ser antes ou depois de entrar na pasta:

```powershell
$env:DB_USERNAME = Read-Host "Usuário do PostgreSQL"
$senhaBanco = Read-Host "Senha do PostgreSQL" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $senhaBanco).Password
Remove-Variable senhaBanco
```

Os textos entre aspas são perguntas, não locais para escrever a senha. Digite a resposta quando solicitado. As variáveis pertencem ao processo do terminal e são herdadas pelo Java iniciado nele; um terminal independente não recebe automaticamente esses valores.

No terminal do cliente-service e no do notificacoes-service, informe o MESMO segredo, com 32–72 bytes UTF-8:

```powershell
$segredoNotificacoes = Read-Host "Segredo compartilhado do notificacoes-service" -AsSecureString
$env:NOTIFICACOES_CLIENT_SECRET = [System.Net.NetworkCredential]::new("", $segredoNotificacoes).Password
Remove-Variable segredoNotificacoes
```

Não colocar senhas, tokens ou chaves privadas no repositório. Não imprimir variáveis secretas para diagnosticar; se necessário, confira apenas seu comprimento.

### Terminal 1: cliente-service

Após informar banco e segredo:

```powershell
Set-Location "C:\Users\mgbri\OneDrive\Documentos\Hotel\cliente-service"
$env:JWT_PRIVATE_KEY_LOCATION = "file:C:/Users/mgbri/.hotel-keys/private.pem"
$env:JWT_PUBLIC_KEY_LOCATION = "file:C:/Users/mgbri/.hotel-keys/public.pem"
.\mvnw.cmd spring-boot:run
```

Use as chaves existentes e confirme os caminhos. Não gere outro par a cada inicialização. Somente cliente-service recebe a chave privada.

Para criar o primeiro ADMIN em um banco novo, no terminal do cliente-service já configurado, execute uma vez:

```powershell
$env:ADMIN_INICIAL_LOGIN = Read-Host "Login do administrador inicial"
$env:ADMIN_INICIAL_EMAIL = Read-Host "E-mail de recuperação do administrador"
$senhaAdmin = Read-Host "Senha inicial do administrador" -AsSecureString
$env:ADMIN_INICIAL_SENHA = [System.Net.NetworkCredential]::new("", $senhaAdmin).Password
Remove-Variable senhaAdmin
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=bootstrap-admin"
```

Depois use inicialização normal. O bootstrap não altera uma conta ADMIN ativa existente, inclusive não preenche retroativamente seu e-mail. Login existente com função diferente ou inativo causa erro.

### Terminal 2: quarto-service

Após informar o banco:

```powershell
Set-Location "C:\Users\mgbri\OneDrive\Documentos\Hotel\quarto-service"
$env:JWT_PUBLIC_KEY_LOCATION = "file:C:/Users/mgbri/.hotel-keys/public.pem"
.\mvnw.cmd spring-boot:run
```

### Terminal 3: reserva-service

Após informar o banco:

```powershell
Set-Location "C:\Users\mgbri\OneDrive\Documentos\Hotel\reserva-service"
$env:JWT_PUBLIC_KEY_LOCATION = "file:C:/Users/mgbri/.hotel-keys/public.pem"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:OUTBOX_HABILITADO = "true"
.\mvnw.cmd spring-boot:run
```

### Terminal 4: notificacoes-service

Após informar banco e o mesmo segredo do cliente-service:

```powershell
Set-Location "C:\Users\mgbri\OneDrive\Documentos\Hotel\notificacoes-service"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:NOTIFICACOES_ENVIO_HABILITADO = "true"
.\mvnw.cmd spring-boot:run
```

Este serviço não abre página HTTP. A interface para ver e-mails é http://localhost:8025. O envio de notificações fica desabilitado por padrão quando NOTIFICACOES_ENVIO_HABILITADO não é definido.

### Variáveis adicionais

| Variável | Onde | Padrão |
|---|---|---|
| FRONTEND_ORIGINS | Três APIs | http://localhost:5173 |
| CLIENTE_SERVICE_URL | Reserva e notificações | http://localhost:9001 |
| QUARTO_SERVICE_URL | Reserva | http://localhost:9002 |
| DB_URL | Notificações | jdbc:postgresql://localhost:5432/hotel_notificacoes |
| MAIL_HOST / MAIL_PORT | Cliente | localhost / 1025 |
| EMAIL_REMETENTE | Cliente | seguranca@hotel.test |

As URLs de banco das três APIs e o SMTP de notificações estão atualmente definidos diretamente em application.properties. Não assumir que DB_URL ou MAIL_HOST alteram todos os módulos.

Para encerrar uma aplicação, Ctrl+C no respectivo terminal. Para parar a infraestrutura preservando dados, docker compose stop. Não usar down -v como procedimento normal: ele remove volumes. Após reiniciar o computador, repetir a inicialização e as variáveis nos novos terminais.

## 7. Banco de dados e Flyway

Flyway versiona alterações de esquema; pgAdmin é a ferramenta para visualizar e administrar o banco. Hibernate está em validate, não update. O banco precisa existir antes da conexão; Flyway cria/altera tabelas, não cria os bancos PostgreSQL.

| Banco | Migrações presentes |
|---|---|
| hotel_clientes | V1 usuários/clientes; V2 CPF; V3 e-mail cliente; V4 e-mail recuperação; V5 tokens recuperação |
| hotel_quarto | V1 tipos/quartos; V2 histórica; V3 versão quartos; V4 validações; V5 versão tipos; V6 remoção MANUTENCAO |
| hotel_reservas | V1 reservas e restrições; V2 outbox |
| hotel_notificacoes | V1 eventos recebidos; V2 notificações; V3 descarte |

A V2 de quartos é uma migração histórica vazia: não preenchê-la agora se já foi aplicada. Criar nova versão para alterações futuras.

baseline-on-migrate=false e clean-disabled=true. Bancos existentes já podem ter baseline; bancos vazios executam V1 em diante. Não habilitar baseline automaticamente nem fazer repair sem entender o histórico. A migração de reservas requer permissão para a extensão btree_gist.

A validação final deve usar bancos isolados, incluindo um cenário vazio e outro com histórico existente. Não apagar o banco pessoal para testar. A consulta de flyway_schema_history deve ser feita no banco correto.

## 8. Eventos, Mailpit e recuperação operacional

Fluxo: reserva e outbox são gravadas na mesma transação → publicador envia ao Kafka → consumidor registra evento e notificação na mesma transação → agendador busca destinatário e envia ao Mailpit.

- Outbox repete falhas com atraso crescente limitado a 300 segundos e não abandona o evento por limite de tentativas.
- A fila de saída preserva ordem por reserva enquanto há eventos anteriores pendentes.
- Kafka usa chave da reserva. O consumidor exige chave igual ao idReserva do corpo.
- Eventos recebidos têm UUID único; repetição do mesmo evento não cria nova notificação.
- Evento mais antigo que outra versão já recebida pode ter notificação descartada como EVENTO_SUPERADO.
- Falhas de consumo vão para hotel.reservas.v1.DLT após a política de repetição; IllegalArgumentException é classificada como não repetível.
- Falhas de envio SMTP/consulta de destinatário têm até 10 tentativas; depois registram LIMITE_TENTATIVAS.
- Mailpit é o destino final escolhido para o projeto, não uma etapa pendente de integração com provedor real.
- Entrega é pelo menos uma vez: SMTP confirmado seguido de falha na gravação pode causar e-mail duplicado.
- A DLT e a tabela notificacao representam estágios diferentes: o script SQL não recupera mensagens que nunca chegaram à tabela.

Procedimentos detalhados: [operação e reprocessamento](#operacao-notificacoes).
Script existente: [reprocessar_notificacao.sql](docs/sql/reprocessar_notificacao.sql).

## 9. Validação final — somente após a implementação

### Item 5: compilação e testes automatizados

Executar o wrapper dentro de cada módulo, com configuração de testes adequada:

```powershell
.\mvnw.cmd test
```

Não apontar testes destrutivos para bancos pessoais. Testes de contexto podem exigir banco, chaves e serviços; configurar ambiente isolado antes. Não assumir que testes existentes cobrem as funcionalidades novas.

Cobertura essencial: regras de datas/preços/status; permissões/propriedade; senha BCrypt; recuperação expirada/usada/versão diferente; intervalo entre pedidos; concorrência; duplicação de eventos; falha e retomada de envio.

### Item 6: migrações

Validar todos os bancos vazios, histórico aplicado, compatibilidade das entidades e constraints, inclusive GiST. Registrar versões e resultado sem expor credenciais.

### Item 7: HTTP

Validar ADMIN, RECEPCIONISTA e dois CLIENTEs distintos. Verificar 401/403, acesso próprio versus alheio, versões desatualizadas e respostas sem senha. Confirmar CORS para origem permitida e rejeição da origem não permitida. Testar preflight e requisição real.

Criar reserva por cliente vinculado, comparar cálculo, alterar datas preservando diária, trocar quarto, confirmar/cancelar/concluir. Duas reservas concorrentes para o mesmo período devem produzir apenas uma gravação válida. Conferir calendário e saída exclusiva. Confirmar a política aceita: JWT antigo permanece utilizável até expiração, sujeito às regras da operação.

### Item 8: integração assíncrona

Criar/alterar reserva e verificar outbox, consumo e Mailpit. Simular interrupções controladas de Kafka e Mailpit, retomada, repetição do mesmo evento, descarte por versão nova, limite de tentativas, reprocessamento SQL e recuperação de um evento da DLT. Não considerar apenas o log Started como prova do fluxo completo.

Registrar evidências e defeitos por item. O resultado efetivamente executado está na seção 13; os demais cenários continuam pendentes.

## 10. Diagnóstico rápido

| Sintoma | Verificação |
|---|---|
| mvnw.cmd não encontrado | Terminal na pasta do módulo correto |
| Conexão recusada | Processo/porta e log de inicialização; não é erro de JWT |
| 401 | Token ausente, expirado, inválido ou chave/issuer incompatível |
| 403 | Perfil sem permissão ou regra de negócio de acesso |
| Serviço não inicia por chave | Caminho file:, arquivo PEM e tipo público/privado |
| Flyway histórico não encontrado | Banco/schema correto, inicialização concluída e Flyway habilitado |
| Mailpit não abre | Docker Desktop, contêiner e porta 8025 |
| Evento não vira e-mail | Distinguir outbox, Kafka/DLT, evento_recebido e notificacao |
| Segredo de serviço inválido | Mesmo valor nos dois terminais, 32–72 bytes UTF-8 |
| CORS no navegador, Postman funciona | Origem exata, porta, preflight e Authorization permitido |

Não alterar permissões, apagar volumes ou recriar chaves apenas para contornar um sintoma sem identificar a causa.

<a id="operacao-notificacoes"></a>

## 11. Operação de notificações e reprocessamento

Atualizado em 13/11/2026. Procedimentos preparados por leitura do código; nenhum comando desta página foi executado durante a documentação. Usar na etapa 8 da checklist, depois de configurar o ambiente.

### 11.1. Identificar onde o evento parou

| Etapa | Evidência | Ação |
|---|---|---|
| Reserva gravada, evento ainda não publicado | outbox_evento.publicado_em nulo no hotel_reservas | Corrigir Kafka/configuração e aguardar repetição automática |
| Consumidor não conseguiu processar | Mensagem em hotel.reservas.v1.DLT | Corrigir causa e avaliar republicação individual |
| Evento recebido, e-mail pendente | notificacao.enviado_em e descartado_em nulos no hotel_notificacoes | Conferir agendador habilitado, cliente-service e SMTP |
| Limite de envio atingido | motivo_descarte = LIMITE_TENTATIVAS | Corrigir causa e usar script SQL para um evento |
| Evento ultrapassado por versão mais recente | motivo_descarte = EVENTO_SUPERADO | Não reativar automaticamente |
| E-mail enviado | enviado_em preenchido | Conferir a interface do Mailpit |

Falha de Kafka antes de receber o evento não é a mesma coisa que falha SMTP depois de recebê-lo.

### 11.2. Notificação interrompida após 10 tentativas

1. Corrigir Mailpit, conectividade, credenciais internas ou os dados responsáveis pela falha.
2. Abrir o banco hotel_notificacoes no pgAdmin.
3. Abrir [reprocessar_notificacao.sql](docs/sql/reprocessar_notificacao.sql).
4. Executar somente o SELECT e identificar um evento.
5. Substituir o UUID zerado na CTE alvo pelo evento_id escolhido. Não alterar a proteção contra o marcador zerado.
6. Executar somente o UPDATE.
7. Uma linha retornada indica reagendamento; zero linhas significa que não houve alteração.
8. Com o serviço rodando e NOTIFICACOES_ENVIO_HABILITADO=true, acompanhar o resultado no banco e no Mailpit.

O script não apaga registros; zera o contador para outro ciclo de até 10 tentativas. O contador deixa de representar o total histórico. Ele não reativa eventos marcados como enviados, descartados por outro motivo ou com versão mais recente já recebida.

Mesmo quando enviado_em está nulo, o SMTP pode ter aceitado uma tentativa anterior antes de uma falha no banco. Reprocessar pode duplicar o e-mail. Não há garantia de envio exatamente uma vez.

### 11.3. Inspecionar a DLT sem alterar o grupo da aplicação

No PowerShell, na raiz do projeto, com Kafka iniciado:

```powershell
Set-Location "C:\Users\mgbri\OneDrive\Documentos\Hotel"
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:19092 --topic hotel.reservas.v1.DLT --from-beginning --max-messages 10 --timeout-ms 10000 --consumer-property enable.auto.commit=false --property print.partition=true --property print.offset=true --property print.key=true --property print.headers=true
```

A consulta limita a saída e pode terminar por timeout se não houver mensagens suficientes. Não utilizar --group hotel-notificacoes-v1: esse é o grupo da aplicação. O consumidor de diagnóstico não deve modificar seus offsets.

Registre tópico, partição, offset e identificação do evento investigado. Leia a exceção nos headers/logs; alguns valores de headers são binários e podem não ser legíveis na saída textual. Não publicar logs completos que contenham dados pessoais.

Consultar não remove a mensagem do Kafka. Registros permanecem sujeitos à política de retenção do tópico.

### 11.4. Republicar individualmente depois de corrigir a causa

Este é um procedimento manual para um evento conhecido, não um consumidor automático de DLT.

1. Identificar por partição e offset o evento que falhou.
2. Corrigir a causa antes de republicar; caso contrário, ele pode voltar para a DLT.
3. Consultar evento_recebido pelo UUID. Se já estiver registrado, a deduplicação impedirá uma nova notificação; investigar a tabela notificacao e usar a seção 2 se aplicável.
4. Obter a chave e o JSON original. Preservar UUID, tipo, data original, versão da reserva e dados do evento.
5. Não inventar UUID novo para forçar processamento. Se o próprio conteúdo estiver inválido, investigar a origem e definir a correção antes de enviar; não “consertar” reservas históricas por tentativa.
6. Republicar apenas o evento revisado para hotel.reservas.v1.
7. Conferir recebimento, deduplicação, notificação e eventual nova entrada na DLT.

Para ler um registro específico, substitua os números de exemplo por partição e offset reais:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:19092 --topic hotel.reservas.v1.DLT --partition 0 --offset 0 --max-messages 1 --timeout-ms 10000 --consumer-property enable.auto.commit=false --property print.key=true --property "key.separator=|"
```

A saída útil tem o formato CHAVE|JSON. Confira o offset identificado antes de copiar. Não copiar linhas de log como se fossem uma mensagem.

Abra o produtor interativo:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:19092 --topic hotel.reservas.v1 --property parse.key=true --property "key.separator=|" --producer-property acks=all --producer-property enable.idempotence=true
```

Cole UMA linha com a chave original, o separador | e o JSON original completo. Não cole literalmente CHAVE|JSON. A chave precisa corresponder a dados.idReserva em texto, como exige o consumidor. O separador é a primeira ocorrência de |. Pressione Enter para publicar e Ctrl+C para encerrar o produtor.

Não incluir os headers de erro da DLT no corpo JSON. Esse procedimento repõe chave e conteúdo; não conserva timestamp/headers do registro Kafka, embora criadoEm no JSON permaneça original. Não é adequado a contratos que dependam desses metadados; o consumidor atual usa chave e corpo.

A publicação manual não remove o registro antigo da DLT e não garante a ordem histórica em relação a versões já publicadas. O processador de notificações consulta versões mais recentes já recebidas e pode descartar a notificação antiga. Não reduzir versões nem apagar a deduplicação para obrigar envio.

### 11.5. Resultado e registro da operação

Anotar no registro de validação: evento, causa, correção, partição/offset de origem, momento da republicação e resultado. Não usar remoção do tópico ou reset global de offsets como forma de recuperar um único evento.

Distinguir:
- Evento aceito pelo consumidor.
- Notificação criada ou já existente.
- Notificação enviada, pendente ou descartada.
- E-mail visível no Mailpit.

O comando do produtor terminar sem erro não prova que o e-mail foi enviado.

### 11.6. Referências dos comandos

- [Apache Kafka — Quickstart](https://kafka.apache.org/quickstart/): uso dos utilitários de produção e consumo.
- [Apache Kafka — opções de saída do consumidor](https://cwiki.apache.org/confluence/spaces/KAFKA/pages/103090995/KIP-431+Support+of+printing+additional+ConsumerRecord+fields+in+DefaultMessageFormatter): chave, partição, offset e headers.
- [Código do leitor de linhas do produtor, versão 4.1.2](https://github.com/apache/kafka/blob/4.1.2/tools/src/main/java/org/apache/kafka/tools/LineMessageReader.java): parse.key e key.separator.

A fonte de verdade das regras de processamento é o código local de KafkaConsumerConfig, ReservaEventoConsumer, EventoRecebidoService e NotificacaoProcessador. Os comandos ainda precisam ser validados com a imagem usada pelo projeto, apache/kafka:4.1.2.

<a id="mcp"></a>

## 12. MCP: conceito, estado e integração

### 12.1 O que é

MCP (Model Context Protocol) padroniza como uma aplicação de IA descobre e chama ferramentas. O host é a aplicação de IA; o cliente MCP é seu componente de conexão; hotel-mcp é o servidor; ferramentas são operações disponíveis. Recursos fornecem contexto e prompts fornecem modelos de instrução, mas o Hotel implementa somente ferramentas.

Fluxo: pessoa → assistente/host → cliente MCP → hotel-mcp → API HTTP autenticada → banco da API. O servidor não consulta o banco diretamente e não substitui autenticação, autorização ou regras do hotel. Não treina o modelo nem precisa chamar uma IA: o modelo está no host.

Há duas conexões diferentes: host → MCP e MCP → APIs. HOTEL_API_TOKEN autentica a segunda. Não é uma chave de fornecedor de IA nem autentica automaticamente o acesso ao servidor MCP.

### 12.2 Estado observado em 13/11/2026

| Item | Estado |
|---|---|
| Boot / Spring AI / Java declarados | 4.1.1 / 2.0.1 / 17 |
| Starter | spring-ai-starter-mcp-server-webmvc |
| Aplicação | protocol=STREAMABLE, stdio=false, porta 9004 |
| Configuração .vscode/mcp.json | Ainda type=stdio, command=java |
| listar_quartos | pagina e tamanho; GET /quarto; retorna página |
| consultar_reserva_por_id | id positivo; GET /reservas/{id} |
| Credencial | HOTEL_API_TOKEN, fixa durante o processo |
| URLs | HOTEL_QUARTO_URL e HOTEL_RESERVA_URL, padrões 9002 e 9003 |

**Divergência pendente do módulo opcional:** o transporte configurado no VS Code não corresponde ao da aplicação. Isso foi documentado, não corrigido nesta consolidação. O MCP permanece fora dos oito itens de conclusão das APIs.

listar_quartos lista todos os status, não calcula disponibilidade por período. Paginar até last=true; não assumir que uma página contém todos os quartos.

HotelApiClient usa timeout de conexão de 3 segundos e leitura de 5 segundos. Token ausente, 401, 403, 404 e falha de comunicação geram mensagens específicas sem devolver corpos de erro ou credenciais ao assistente.

### 12.3 Arquivos que compõem o MCP

| Arquivo no módulo hotel-mcp | Responsabilidade |
|---|---|
| pom.xml | Starter WebMVC MCP, cliente REST e versões |
| HotelMcpApplication.java | Inicialização e registro de ToolCallbackProvider |
| HotelTools.java | Métodos com @Tool e parâmetros com @ToolParam |
| HotelApiClient.java | HTTP, Bearer, paginação e tratamento de falhas |
| application.properties | Transporte, porta, URLs e token |
| logback-spring.xml | Logs em STDERR |
| .vscode/mcp.json, na raiz do projeto | Conexão do host ao servidor |

Os arquivos Java ficam em src/main/java/br/com/best2bee/hotel_mcp; as configurações da aplicação, em src/main/resources.

### 12.4 Procedimento preparado para o transporte HTTP atual

Não foi executado nesta revisão. Primeiro alinhar a configuração do cliente MCP com a aplicação. O transporte HTTP precisa de proteção própria; não existe SecurityConfig no módulo MCP inspecionado. Não publicar esse processo na rede com um JWT compartilhado. Para demonstração local, o comando abaixo limita a escuta a 127.0.0.1.

1. Iniciar as APIs necessárias e fazer login pela API de clientes.
2. No PowerShell que iniciará o MCP, informar somente o accessToken, sem o prefixo Bearer:

```powershell
Set-Location "C:\Users\mgbri\OneDrive\Documentos\Hotel\hotel-mcp"
$tokenMcp = Read-Host "JWT do usuário, sem Bearer" -AsSecureString
$env:HOTEL_API_TOKEN = [System.Net.NetworkCredential]::new("", $tokenMcp).Password
Remove-Variable tokenMcp
$env:SERVER_ADDRESS = "127.0.0.1"
$env:SPRING_AI_MCP_SERVER_STREAMABLE_HTTP_MCP_ENDPOINT = "/mcp"
.\mvnw.cmd spring-boot:run
```

3. Na configuração MCP do cliente escolhido, usar transporte HTTP. Exemplo preparado para a entrada hotel em .vscode/mcp.json (preservar outras entradas existentes):

```json
{
  "servers": {
    "hotel": {
      "type": "http",
      "url": "http://localhost:9004/mcp"
    }
  }
}
```

4. Conectar pelo gerenciamento de servidores MCP do host e confirmar que as duas ferramentas aparecem na sessão.
5. Consultar uma página de quartos e uma reserva acessível ao usuário do token.

Esse exemplo é uma orientação para corrigir a divergência; o arquivo .vscode/mcp.json não foi alterado nesta tarefa. Disponibilidade no VS Code não significa disponibilidade em outro aplicativo ou chat. Cada host precisa de sua conexão.

Com HTTP, o processo Java é iniciado separadamente; conectar o host não substitui sua inicialização. Abrir a URL no navegador não constitui um teste completo do protocolo.

Para gerar JAR, quando necessário:

```powershell
.\mvnw.cmd package -DskipTests
java -jar target/hotel-mcp-0.0.1-SNAPSHOT.jar
```

O primeiro comando não executa testes, mas pode compilar seus fontes. Os dois comandos são alternativas ao spring-boot:run; não iniciar duas instâncias na mesma porta. As variáveis devem estar configuradas no terminal que inicia Java.

### 12.5 Expiração e permissões

O MCP não faz login automaticamente nem renova JWT. Ao expirar:

1. Fazer novo login em cliente-service.
2. Parar o processo MCP.
3. Atualizar HOTEL_API_TOKEN no terminal que iniciará o novo processo, usando a leitura protegida acima.
4. Reiniciar o MCP e reconectar o host se necessário.

Não é preciso recompilar apenas para trocar o token. Alterar uma variável em outro terminal não modifica um processo já iniciado. Não colar JWT no chat, JSON versionado ou documentação.

O token não precisa ser ADMIN: deve pertencer ao usuário cujas permissões se pretende usar. CLIENTE está sujeito às restrições de propriedade da API. Todos os acessos de um processo MCP usam essa mesma identidade; não é um servidor multiusuário com isolamento por pessoa.

### 12.6 STDIO e reutilização em outro projeto

STDIO comunica pelo stdin/stdout do processo iniciado pelo host. HTTP comunica por uma URL de servidor iniciado separadamente. Não misturar a configuração de um transporte com a do outro. Para adotar STDIO novamente, alinhar starter/propriedades/cliente conforme a versão utilizada; os exemplos antigos não descrevem o estado atual.

Em STDIO, stdout é reservado ao protocolo: logs devem ir a STDERR e o banner deve ser desativado. O projeto mantém essa configuração de logs, embora hoje use HTTP.

Passos para reutilizar a estrutura:

1. Definir as operações que a IA pode executar, começando por consultas.
2. Implementar e proteger os endpoints na API antes de expô-los como ferramentas.
3. Selecionar versões compatíveis de Java, Spring Boot e Spring AI e um único transporte.
4. Criar cliente HTTP com URL externa, timeouts, DTOs e erros sanitizados.
5. Criar métodos @Tool com nomes e descrições claros e parâmetros @ToolParam.
6. Registrar as ferramentas com MethodToolCallbackProvider, como em HotelMcpApplication.
7. Configurar a conexão no host, credenciais e permissões mínimas.
8. Validar argumentos, paginação, resultados vazios, acesso negado, token expirado e API indisponível.
9. Para ferramentas de escrita, definir confirmação, autorização e proteção contra repetição antes de disponibilizá-las.

Conteúdo retornado pelas ferramentas é dado, não uma instrução superior para o assistente. Não devolver senhas, hashes, chaves ou tokens. Não compartilhar token ADMIN entre usuários. Uso remoto exige autenticação host → MCP e uma estratégia de identidade nas APIs; não está pronto apenas por trocar STDIO por HTTP.

### 12.7 Diagnóstico e referências

| Sintoma | Conferir |
|---|---|
| Ferramenta não disponível | Conexão e ferramentas habilitadas na sessão/host correto |
| Processo roda mas não conecta | Transporte do cliente e do servidor; divergência STDIO/HTTP |
| 401 na ferramenta | JWT expirado/inválido; renovar e reiniciar |
| 403 | Permissões do usuário do token |
| 404 ao consultar reserva | ID inexistente ou inacessível ao usuário |
| Conexão recusada | API/MCP iniciado, porta e URL |
| Erro de JSON no protocolo STDIO | Logs/banner escritos no stdout |
| Configuração interativa não suportada pelo host | Consultar suporte do host; não inserir segredo diretamente no arquivo |

- [MCP: arquitetura](https://modelcontextprotocol.io/docs/learn/architecture).
- [Spring AI: servidor MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html).
- [VS Code: configuração MCP](https://code.visualstudio.com/docs/agents/reference/mcp-configuration).
- [VS Code: gerenciamento de servidores](https://code.visualstudio.com/docs/agents/run/manage-mcp-servers).

Esta seção preserva a orientação reutilizável do antigo guia, corrigindo o transporte e a assinatura da ferramenta de paginação conforme os arquivos atuais.

## 13. Resultados dos testes — 13/11/2026

| Módulo | Testes | Falhas / erros |
|---|---:|---:|
| cliente-service | 18 | 0 |
| quarto-service | 13 | 0 |
| reserva-service | 14 | 0 |
| notificacoes-service | 8 | 0 |
| **Total** | **53** | **0** |

Ambiente: Java 25.0.2 compilando com release 17; PostgreSQL 18.6 em contêiner temporário, porta 15432 restrita a 127.0.0.1. As três APIs usaram portas HTTP aleatórias durante os testes. Nenhuma chave de produção foi utilizada: os testes geram pares temporários. Os bancos normais do Hotel não foram alterados.

Os quatro testes de contexto agora ativam o perfil test com bancos exclusivos: hotel_clientes_test, hotel_quartos_test, hotel_reservas_test e hotel_notificacoes_test. A conexão normal da aplicação não é usada por esses testes. O perfil desabilita consumo Kafka, agendador de outbox e envio automático de notificações.

### O que foi comprovado

- Compilação dos quatro serviços e inicialização dos contextos com Hibernate validate.
- Aplicação das 5/6/2/3 migrações de clientes/quartos/reservas/notificações em bancos vazios; novas inicializações validaram o histórico aplicado.
- JWT válido, expirado, emissor incorreto e assinatura de outra chave nos testes existentes.
- CORS por HTTP nas três APIs: origem permitida recebe preflight 200, origem proibida 403; recurso protegido sem token recebe 401.
- Login e redefinição de senha por HTTP: token de recuperação de uso único, senha antiga rejeitada e nova aceita; JWT anterior permanece válido conforme a política escolhida.
- Recuperação: hash persistido em vez de token original, prazo de 30 minutos, intervalo entre pedidos, conta inexistente/inativa, versão alterada, expiração e limite de bytes da senha.
- Reservas: cálculo, preservação de diária, conflito de período, quarto inativo, versão desatualizada, acesso alheio e restrição de confirmação por cliente.
- Duas inserções concorrentes no PostgreSQL: apenas uma reserva para o mesmo quarto/período foi persistida.
- Notificações: sucesso, repetição temporária, décima falha, sucesso na décima tentativa e evento superado.
- Deduplicação persistida de um evento e execução do script SQL de reprocessamento para um evento de teste.

Os testes unitários de notificações simulam SMTP e a consulta do destinatário. Os de reservas simulam as APIs dependentes. Isso não comprova o fluxo completo entre todos os processos.

### Correção encontrada

Na disputa simultânea da exclusão GiST, o PostgreSQL pode rejeitar uma operação por sobreposição (23P01) ou abortar uma transação por deadlock (40P01). Ambos impedem a duplicação.

Foi acrescentado tratamento de PessimisticLockingFailureException ao GlobalExceptionHandler de reservas para retornar 409 com orientação de nova consulta, sem expor detalhes SQL. Um teste HTTP com MockMvc verifica esse contrato. O teste concorrente aceita apenas esses dois estados conhecidos e exige exatamente uma inserção bem-sucedida; outros erros continuam reprovando o teste.

### Como repetir

Com Docker Desktop iniciado e porta 15432 livre, na raiz do projeto:

```powershell
docker run --name hotel-validacao-20261113 -d --rm -p 127.0.0.1:15432:5432 -e POSTGRES_HOST_AUTH_METHOD=trust postgres:18.6
docker exec hotel-validacao-20261113 pg_isready -U postgres
```

Aguardar pg_isready indicar que aceita conexões. Este contêiner é exclusivamente descartável, usa autenticação trust apenas para facilitar os testes locais e não deve ser reutilizado como ambiente real. Não executar se já houver outro ambiente ocupando a porta.

Criar os bancos somente na primeira inicialização desse contêiner:

```powershell
docker exec hotel-validacao-20261113 createdb -U postgres hotel_clientes_test
docker exec hotel-validacao-20261113 createdb -U postgres hotel_quartos_test
docker exec hotel-validacao-20261113 createdb -U postgres hotel_reservas_test
docker exec hotel-validacao-20261113 createdb -U postgres hotel_notificacoes_test
```

Executar cada comando abaixo a partir da raiz Hotel:

```powershell
Push-Location cliente-service
.\mvnw.cmd -B test
Pop-Location

Push-Location quarto-service
.\mvnw.cmd -B test
Pop-Location

Push-Location reserva-service
.\mvnw.cmd -B test
Pop-Location

Push-Location notificacoes-service
.\mvnw.cmd -B test
Pop-Location
```

Conferir BUILD SUCCESS de cada módulo. Não sobrescrever spring.datasource.url por variáveis ou parâmetros externos ao repetir a suíte. Alguns testes inserem dados sintéticos nos bancos de teste. Os agendadores e listeners ficam desligados; Kafka e Mailpit não são necessários para esta suíte.

Relatórios detalhados ficam em target/surefire-reports de cada módulo; target/validacao-testes.log guarda a última execução feita pelo assistente. Esses arquivos são gerados e não substituem os testes versionados.

Ao terminar, remover somente o ambiente temporário criado para esta execução:

```powershell
docker stop hotel-validacao-20261113
```

Como foi iniciado com --rm, o contêiner temporário é removido ao parar. Não usar esse comando com nomes de contêineres de desenvolvimento.

### O que ainda não foi comprovado nesta rodada

- Migração de cópia representativa dos bancos existentes, incluindo o histórico com baseline.
- Matriz HTTP completa de ADMIN/RECEPCIONISTA/dois CLIENTEs em todos os endpoints e o fluxo de reserva entre processos reais.
- Entrega real reserva → outbox → Kafka → consumidor → Mailpit.
- Interrupção/retomada real de Kafka e Mailpit, publicação e recuperação pela DLT.
- Envio real do e-mail de recuperação. O teste HTTP insere um pedido de recuperação sintético para validar a redefinição sem SMTP.
- Integração do MCP e uso pelo front-end real.

A aprovação destes 53 testes não encerra automaticamente os itens 6–8.

