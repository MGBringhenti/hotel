# Rodar o Hotel inteiro com Docker

Este ambiente local inclui front-end (Nginx), cliente-service, quarto-service, reserva-service,
notificacoes-service, PostgreSQL, Kafka e Mailpit. O hotel-mcp e opcional e nao faz parte da
inicializacao da interface.

## Primeiro uso no Windows

1. Abra o Docker Desktop e aguarde a inicializacao.
2. No PowerShell:

       cd C:\dev\Hotel
       .\iniciar.ps1

O script gera o .env da raiz na primeira execucao, compila as imagens e inicia os servicos.
Nas proximas vezes, pode usar o mesmo comando. As camadas das imagens sao reaproveitadas.
Para iniciar sem recompilar: .\iniciar.ps1 -NaoReconstruir.

Se o PowerShell bloquear scripts locais, execute apenas este script com:
powershell -ExecutionPolicy Bypass -File .\iniciar.ps1

- Site: http://localhost:8080
- Mailpit: http://localhost:8025
- Login e senha inicial: campos ADMIN_INICIAL_LOGIN e ADMIN_INICIAL_SENHA no .env da raiz.
- Nao envie o .env ao Git. Ele e gerado localmente e esta no .gitignore.
- Depois do primeiro acesso, voce pode trocar a senha pela tela Minha conta.
- Alterar ADMIN_INICIAL_SENHA no .env nao troca a senha de uma conta ja existente.

O .env inicial e criado automaticamente com senhas aleatorias. Nao existe senha padrao fixa.
As chaves RSA sao geradas uma unica vez em volumes separados. Reiniciar nao troca as chaves.

## Comandos principais (na raiz do Hotel)

    docker compose up -d --build
    docker compose ps
    docker compose logs -f --tail 80
    docker compose logs -f cliente-service
    docker compose down

O primeiro comando exige o .env ja configurado. O script iniciar.ps1 faz essa preparacao.
Depois de alterar o codigo, reconstrua as imagens com .\iniciar.ps1.
Fechar o terminal nao para os containers.

## Dados e PostgreSQL

O banco do Docker e NOVO e separado do PostgreSQL instalado no Windows.
Nenhum cadastro do banco anterior e importado, apagado ou alterado automaticamente.

Volumes preservam bancos, eventos Kafka, mensagens Mailpit e chaves JWT.
docker compose down preserva esses volumes.
NAO use docker compose down -v se quiser manter os dados e as chaves.

Para conectar o pgAdmin ao PostgreSQL do Docker:
- Host: localhost
- Porta: 5433 (ou POSTGRES_PORT no .env)
- Usuario: hotel
- Senha: POSTGRES_PASSWORD no .env
- Bancos: hotel_clientes, hotel_quarto, hotel_reservas, hotel_notificacoes

A criacao dos quatro bancos acontece no primeiro uso do volume.
O Flyway de cada servico cria e atualiza suas tabelas.
Trocar POSTGRES_PASSWORD no .env nao altera automaticamente a senha de um volume ja inicializado.

## Enderecos internos e APIs

Dentro do Docker, localhost e o proprio container. Os servicos usam nomes:
postgres, kafka, mailpit, cliente-service e quarto-service.

O navegador acessa apenas a porta 8080. O Nginx encaminha:
- /api/clientes/ para cliente-service:9001/
- /api/quartos/ para quarto-service:9002/
- /api/reservas/ para reserva-service:9003/

Exemplo de login no Postman: POST http://localhost:8080/api/clientes/auth/login.
As portas 9001, 9002, 9003, 9092 e 1025 nao sao publicadas neste ambiente completo.
As propriedades originais das APIs continuam funcionando para execucao fora do Docker;
o Compose sobrepoe os enderecos por variaveis de ambiente.

## Preparacao automatica

- kafka-volume: prepara as permissoes do volume para o usuario do Kafka.
- chaves: gera ou valida o par RSA existente e termina com codigo 0.
- topicos: cria hotel.reservas.v1 e hotel.reservas.v1.DLT, caso ainda nao existam.
- postgres: inicializa os quatro bancos em volume vazio.
- cliente-service: perfil bootstrap-admin cria o administrador inicial caso nao exista.
- notificacoes-service: consome eventos e envia e-mails ao Mailpit.

Os containers chaves e topicos aparecerem como Exited (0) e esperado.
O envio de e-mail e local; nao existe provedor de e-mail real neste escopo.
O Compose aguarda o banco, Kafka e APIs ficarem prontos antes de iniciar seus dependentes.

## Memoria, portas e diagnostico

A primeira compilacao baixa imagens e dependencias e pode demorar varios minutos.
O script limita o paralelismo das compilacoes. Se houver erro de memoria, aumente a memoria
disponivel para o Docker Desktop/WSL e tente novamente.

Nao execute simultaneamente o Compose de back-end/ e o Compose da raiz usando a mesma porta
do Mailpit. O arquivo antigo continua sendo uma opcao para executar apenas Kafka e Mailpit.
Neste ambiente o projeto Compose se chama hotel-completo e usa volumes separados.

Se 8080, 8025 ou 5433 estiverem em uso, ajuste HOTEL_PORT, MAILPIT_PORT ou POSTGRES_PORT no .env.
Nao publique este ambiente local na internet: as interfaces ficam ligadas a 127.0.0.1.

Para conferir migracoes e falhas:
    docker compose logs --tail 100 cliente-service quarto-service reserva-service notificacoes-service

## Validacao integrada realizada em 17/11/2026

- As imagens do front-end e dos quatro servicos Java foram compiladas no Docker.
- PostgreSQL, Kafka e APIs ficaram saudaveis; Flyway inicializou os bancos novos.
- Login realizado pelo navegador em localhost:8080, sem copiar token manualmente.
- Cadastro de cliente, tipo e quarto, consulta de disponibilidade e criacao de reserva pelas APIs reais.
- Calculo de duas diarias de R$ 123,00 conferido: total R$ 246,00.
- Confirmacao e cancelamento da reserva de demonstracao realizados.
- Evento processado pelo Kafka/notificacoes-service e e-mail recebido no Mailpit.
- Reserva real exibida na listagem do front-end.

Foram mantidos neste banco novo os registros de demonstracao: cliente Validacao Docker,
tipo Teste Docker, quarto 9901 e uma reserva CANCELADA. O banco instalado no Windows nao foi alterado.
O ambiente permanece iniciado. Use docker compose down para parar preservando os dados.

Referencias: [ordem de inicializacao do Compose](https://docs.docker.com/compose/how-tos/startup-order/)
e [imagem oficial do PostgreSQL](https://hub.docker.com/_/postgres).
