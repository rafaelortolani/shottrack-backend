# ADR-0018: Autenticação para produção — recuperação de senha, logout com revogação e rate limit

## Contexto
Antes de abrir o ShotTrack para usuários reais, três lacunas de autenticação
apareceram no inventário: não existe "esqueci minha senha"; o logout só limpa
cookies no BFF (o refresh token continua válido no servidor até expirar,
apesar da coluna `revoked` já existir); e nada limita tentativas em login,
cadastro e pedidos de e-mail.

## Decisão

### 1. Recuperação de senha (UC49, UC50)
- Pedido por e-mail (`POST /api/auth/password-reset/request`). A resposta é
  **sempre sucesso**, exista a conta ou não, pra não revelar quem está
  cadastrado. Se existir, envia e-mail com link de uso único.
- Token aleatório, **validade de 1 hora**, guardado só como **hash**. Um novo
  pedido invalida o anterior (mesmo padrão do cadastro, ADR-0009).
- Confirmação (`POST /api/auth/password-reset/confirm`) define a nova senha,
  marca o token como usado e **revoga todos os refresh tokens** do usuário.
- Erros: `PASSWORD_RESET_TOKEN_INVALID`, `PASSWORD_RESET_TOKEN_EXPIRED`,
  `PASSWORD_RESET_TOKEN_ALREADY_USED`.

### 2. Logout com revogação (UC51)
- `POST /api/auth/logout` recebe o refresh token e o revoga. Idempotente:
  token desconhecido ou já revogado também responde sucesso. Não exige access
  token válido (o logout precisa funcionar com o access expirado).
- Alterar senha (UC05) também revoga todos os refresh tokens do usuário. Efeito
  no front: depois de trocar a senha, o atleta volta ao login.

### 3. Rate limit
- No backend, em memória (Bucket4j), configurável em `application.yml` (`app.rate-limit.*`).
- A regra é escolhida pelo caminho **normalizado** (decodificado, sem `;param` nem `//`, sem barra
  final, em minúsculas) — não pela URL bruta. `/api/auth/log%69n` e `/api/auth/login/` caem no mesmo
  bucket de `/api/auth/login` e respondem `400 INVALID_REQUEST_PATH` sem chegar ao controller; `//`,
  `;jsessionid` e maiúsculas já são barrados antes pelo Spring Security.
- Aplica em (limite padrão; chave):
  - login — 10 / 15 min por IP + e-mail **e** 20 / hora por e-mail (minúsculas), qualquer IP;
  - cadastro (`POST /api/users/registration`) e pedido de redefinição — 5/hora por e-mail e 20/hora por IP;
  - conclusão do cadastro (`/registration/completion`) — 10 / hora por IP;
  - renovação de sessão (`POST /api/auth/refresh`) — 30 / 15 min por IP;
  - confirmação da redefinição (`/password-reset/confirm`) — 10 / 15 min por IP;
  - troca de senha (`PATCH /api/users/me/password`) — 5 / 15 min por usuário;
  - troca de e-mail (`POST /api/users/me/email`) — 5 / hora por usuário; confirmação do código
    (`/me/email/confirmation`) — 10 / 15 min por usuário;
  - exclusão da conta (5/h) e exportação de dados (3/h), por usuário (ADR-0019).
  Rotas por usuário usam o id do token; sem usuário válido, o IP.
- Ao exceder: HTTP 429, código `TOO_MANY_REQUESTS`, header `Retry-After`.
- O IP real só é lido de `X-Forwarded-For` quando
  `app.rate-limit.trust-forwarded-header=true` (atrás do proxy em produção); caso contrário usa o IP da
  conexão, pra ninguém burlar o limite forjando o header. Com o header confiável vale a entrada a
  `app.rate-limit.trusted-proxy-hops` posições do **fim** da lista (padrão 1 = a última, a que o proxy
  acrescentou); as anteriores são escritas pelo cliente e ignoradas. Lista menor que isso → IP da conexão.
- **Limitação conhecida:** os contadores ficam na memória de cada instância. Com mais de uma instância
  do backend, o limite efetivo se multiplica pelo número de instâncias (e reiniciar zera os contadores).
  Antes de escalar horizontalmente é preciso mover os buckets para um armazenamento compartilhado
  (Redis ou o banco) — não implementado agora.
- Testes usam limites configuráveis baixos.

### 4. Limites de entrada (abuso — Lote 3 da auditoria)
- O corpo de qualquer requisição tem teto de 64 KB (`app.request.max-body-bytes`): acima disso, `413 PAYLOAD_TOO_LARGE`
  (recusado antes do Spring Security quando há `Content-Length`; sem ele, o stream interrompe a leitura ao passar do teto).
- Todo texto de entrada tem `@Size(max=…)` coerente com a coluna (nome 100, e-mail 254, apelido 100, observações 2000 etc.;
  a lista fica nos UCs). Senhas novas: mínimo de 8 caracteres e máximo de 72 **bytes UTF-8** (limite do BCrypt, que recusa mais que isso; letra acentuada ocupa 2 bytes e emoji 4 — `@MaxUtf8Bytes`). Todo `encode()` passa por `PasswordBytes.encode`, que devolve `VALIDATION_ERROR` em vez de deixar o BCrypt lançar (defesa em profundidade).
- Número vindo como texto (valor de resultado) só entra como número simples (`SafeDecimal`: sem notação científica, ≤ 32
  caracteres, ≤ 18 dígitos, ≤ 6 casas); o que estiver fora disso é ignorado pelos cálculos.

### 5. Perfil de produção e rotas públicas (Lote 5 da auditoria)
- `application-prod.yml` (`SPRING_PROFILES_ACTIVE=prod`): Swagger/OpenAPI desligados (as rotas dão 404),
  actuator só com `health` (sem detalhes) e **nenhuma credencial com valor padrão** — `DB_URL`, `DB_USERNAME`,
  `DB_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `SMTP_HOST`, `SMTP_PORT`,
  `SMTP_USERNAME`, `SMTP_PASSWORD`, `FRONTEND_URL` e `JWT_SECRET` são obrigatórios: faltando uma, a aplicação
  não sobe. Opcionais: `RABBITMQ_PORT` (5672), `RABBITMQ_VHOST` (`/`), `SENTRY_DSN`. SMTP exige STARTTLS.
  O console de log usa a mesma máscara de e-mail/JWT do arquivo em todos os perfis (ADR-0019).
- `SecurityConfig` lista as rotas públicas de `/api/auth` uma a uma (login, refresh, logout,
  password-reset/request, password-reset/confirm) em vez de `/api/auth/**`: rota nova nasce protegida, e uma rota
  inexistente sob `/api/auth` sem token responde 401 (com token, 404).
- `docker-compose.yml` é só de desenvolvimento: portas em `127.0.0.1`, imagens com versão fixa
  (`postgres:16.15`, `rabbitmq:3.9.10-management`, `axllent/mailpit:v1.31.0`) e credenciais `shottrack` padrão.

### 6. Identidade e sessão (Lote 6 da auditoria)
- **E-mail único sem diferenciar caixa (S15):** o e-mail entra sem espaços nas pontas e em minúsculas (cadastro,
  login, troca de e-mail, redefinição — `EmailNormalizer`) e há índice único em `lower(email)` (V54). A V54 **aborta**
  se houver e-mails duplicados por caixa e passa os existentes para minúsculas; checagem prévia em
  `docs/seguranca/checar-emails-duplicados.sql`.
- **Conta não descobrível (S05):** o cadastro responde `202` igual exista a conta ou não (se existir, o dono recebe
  "alguém tentou cadastrar seu e-mail", sem link); a troca de e-mail para um endereço de outra conta também responde
  igual e não gera código; o login com e-mail inexistente faz uma comparação BCrypt com hash falso. O
  `EMAIL_ALREADY_REGISTERED` só sobrevive onde quem chama já provou ser dono do endereço (concluir o cadastro e
  confirmar o código da troca). Os limites por e-mail (seção 3) seguem valendo.
- **Refresh token como hash (S06) e rotação atômica (S20):** ver ADR-0001. A V55 apagou os tokens em texto puro: **todas
  as sessões caíram** no deploy.

## Alternativas consideradas
- Redis para o rate limit já agora → adiado: com uma VM só, memória basta;
  migra quando houver mais de uma instância.
- CAPTCHA no cadastro → adiado para o bloco de produção (depende de nuvem e
  domínio); o rate limit cobre o mais urgente.
- Resposta diferente quando o e-mail não existe no "esqueci minha senha" →
  rejeitado: permite enumerar contas.
- Manter as sessões ativas após redefinir a senha → rejeitado: quem redefine
  costuma suspeitar de acesso indevido.

## Consequências
- Nova tabela `password_reset_tokens` (id, user_id, token_hash, expires_at,
  used_at, created_at), sem ON DELETE CASCADE.
- Nova dependência de rate limit e nova exceção `TOO_MANY_REQUESTS` (429) no
  `ApiExceptionHandler`, com mensagem pt/en.
- Front: telas "Esqueci minha senha" e "Redefinir senha", logout chamando o
  backend e tratamento do 429. O FUC05 (alterar senha) muda: ao concluir,
  encerra a sessão e leva ao login.
- Provedor de e-mail real continua pendente (bloco de produção); em
  desenvolvimento os e-mails vão pro Mailpit.

## Referências
- ADR-0001 (JWT), ADR-0009 (cadastro em duas etapas — padrão de token por link)
