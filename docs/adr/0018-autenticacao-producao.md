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
