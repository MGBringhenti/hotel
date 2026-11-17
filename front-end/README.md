# Front-end do Hotel

React + Vite. Interface em português para CLIENTE, RECEPCIONISTA e ADMIN.

## Executar

No PowerShell:

    cd C:\dev\Hotel\front-end
    npm install
    npm run dev

Abra o endereço exibido pelo Vite (normalmente http://localhost:5173).
As APIs devem estar iniciadas: clientes em 9001, quartos em 9002 e reservas em 9003.
O fluxo de notificações continua dependendo da infraestrutura descrita no README da raiz.

Para alterar os endereços, copie .env.example para .env.local e ajuste as três URLs.
Variáveis VITE_ são públicas; nunca coloque senhas, chaves privadas ou tokens nelas.
Se mudar a origem do front-end, ajuste o CORS das APIs.

## Funcionalidades

- Login e saída, sessão em memória, expiração automática e tratamento de respostas 401.
- Menus e rotas por perfil. A autorização definitiva é feita pelas APIs.
- Reservas: criar, listar, editar pendentes (equipe), cancelar, confirmar e concluir conforme datas e perfil.
- Seleção paginada de cliente, quarto, tipo e usuário, com filtro apenas da página atual.
- Calendário por quarto, com dias ocupados, dias livres e seleção de entrada/saída.
- Clientes: cadastrar, editar, ativar/inativar (ADMIN), vincular usuário CLIENTE (ADMIN).
- Quartos: cadastrar e editar (equipe), alterar situação (ADMIN).
- Tipos: consultar (equipe), cadastrar, editar e excluir quando não referenciados (ADMIN).
- Usuários (ADMIN): cadastrar perfil, editar login/e-mail, alterar situação.
- Minha conta: trocar senha.
- Recuperar acesso e redefinir senha com código recebido por e-mail.

## Recuperação de senha com Mailpit

Abra /recuperar-senha e informe o e-mail de recuperação do usuário.
Consulte a mensagem em http://localhost:8025, copie o código e use /redefinir-senha.
O back-end envia um código, não um link. Ele expira em 30 minutos.
A resposta de solicitação é genérica para não revelar se a conta existe.

## Decisões e limites

- Recarregar a página encerra a sessão em memória. Não armazenamos JWT no localStorage.
- O back-end continua responsável por propriedade da reserva, conflitos, versões, preços e transições.
- A consulta de disponibilidade não bloqueia o quarto. O servidor confere de novo ao reservar.
- A diária/total definitivos vêm do servidor; a edição preserva a diária salvo troca de quarto.
- O perfil só pode ser escolhido na criação: o DTO de atualização de usuário não oferece troca de perfil.
- A API de cliente não retorna o vínculo atual do usuário. A tela permite enviar o vínculo, mas não inventa informações sobre o vínculo existente.
- Não há cadastro público: a recepção cadastra clientes; o ADMIN cria e vincula as contas.
- Trocar senha/sair não revoga tokens já emitidos no back-end; mantém-se a política existente.
- Falhas de rede em gravações podem ter resultado incerto. Consulte a listagem antes de repetir.
- Calendário usa intervalos [entrada, saída), no fuso America/Sao_Paulo. Dias anteriores a hoje não são consultados.
- Os arquivos antigos em services e a página Quartos.jsx foram preservados como referência do aprendizado; as telas novas usam lib/api.js e Cadastros.jsx.

## Organização

- src/App.jsx: sessão, navegação e rotas.
- src/lib/api.js: transporte, URLs e erros.
- src/lib/catalogos.js: campos e colunas dos cadastros.
- src/lib/formatacao.js: datas, moeda, senha e ações de reserva.
- src/hooks/usePagina.js: consulta paginada com cancelamento.
- src/components: seletores, formulário de edição, calendário e elementos comuns.
- src/pages: telas de acesso, conta, cadastros, disponibilidade e reservas.
- src/index.css: visual responsivo.

## Verificações

    npm run lint
    npm run build
    npm test

Os testes de regras não substituem o teste integrado com PostgreSQL, APIs, Kafka e Mailpit.
Use contas de teste separadas para verificar permissões, conflitos e mudanças de estado.

## Testes de navegador

Execute npm run test:e2e. Os testes usam Microsoft Edge instalado no Windows, iniciam o Vite na porta 4175 e simulam as APIs; não acessam o banco. O teste visual gera capturas em test-results. Para formatar o código, execute npm run format.

## Validação realizada em 17/11/2026

- npm run lint: aprovado, sem erros.
- npm run build: aprovado.
- npm test: 8 testes de regras aprovados.
- npm run test:e2e: 15 cenários de navegador aprovados, usando Edge e APIs simuladas.
- Capturas de desktop e celular revisadas; disponibilidade testada a 390 pixels sem rolagem horizontal.

Os cenários cobrem os três perfis, criação/edição/cancelamento de reserva, cadastros, vínculo de usuário, recuperação e troca de senha, conflito de versão, sessão expirada, paginação, falha de rede e calendário. Nenhum registro foi criado ou alterado nas APIs reais durante essa validação. A integração completa com os serviços reais permanece a validar.

## Ambiente integrado com Docker

O projeto agora possui Compose completo na raiz. Consulte [o guia Docker](../docker/README.md).
Em 17/11/2026, tambem foram validados no ambiente Docker real: login pelo navegador,
cadastros de demonstracao, disponibilidade, criacao/confirmacao/cancelamento de reserva,
listagem na interface e entrega da notificacao via Kafka ao Mailpit.
Isso complementa os testes simulados descritos acima; nao equivale a testar todas as combinacoes de perfis contra o banco real.
