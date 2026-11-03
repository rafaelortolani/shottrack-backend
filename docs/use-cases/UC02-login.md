# UC02 - Login

## Ator
Atleta cadastrado (UC01)

## Pré-condição
Usuário já possui cadastro (email + senha) no sistema.

## Fluxo principal
1. Atleta envia email e senha
2. Sistema valida credenciais contra o hash salvo
3. Sistema gera access token (JWT, 1h) e refresh token (7 dias)
4. Sistema retorna os dois tokens

## Fluxos alternativos
- 2a. Email não encontrado ou senha incorreta → erro genérico
  "credenciais inválidas" (nunca revelar qual dos dois está errado)

## Fluxo secundário: renovação de token (refresh)
1. Atleta envia o refresh token recebido no login (ou na renovação anterior)
2. Sistema localiza o refresh token e confere que ainda é válido (existe,
   não expirou, ainda não foi usado)
3. Sistema invalida o refresh token recebido e gera um novo par (access +
   refresh) — rotação a cada uso (ADR-0001)
4. Sistema retorna os dois tokens novos

## Fluxos alternativos (refresh)
- Os refresh tokens são guardados só como hash SHA-256 (S06): o valor entregue ao cliente nunca é gravado e a
  busca é pelo hash. A rotação é atômica (S20): a revogação é um `UPDATE ... WHERE revoked = false`; de duas
  chamadas simultâneas com o mesmo token só uma vence e a outra é tratada como reuso (2b).
- Login com e-mail inexistente executa uma comparação BCrypt com hash falso (S05), igualando o tempo da resposta
  ao de "senha errada"; o e-mail é normalizado (minúsculas, S15).
- 2a. Refresh token não encontrado ou expirado → erro genérico
  "refresh token inválido"
- 2b. Refresh token já foi usado antes (reuso) → tratado como sinal de roubo:
  todos os refresh tokens do atleta são revogados, erro "refresh token
  inválido", atleta precisa logar de novo (ADR-0001)

## Limite de tentativas (ADR-0018)
- Login: 10 tentativas / 15 min por IP + e-mail e 20 / hora por e-mail (qualquer IP) → `429 TOO_MANY_REQUESTS`
  com `Retry-After`. Renovação de sessão (`POST /api/auth/refresh`): 30 / 15 min por IP.
- Variações do caminho (`/api/auth/log%69n`, barra final) contam no mesmo limite e respondem
  `400 INVALID_REQUEST_PATH`.

## Limites de entrada (Lote 3, S10/S18)
- E-mail: até 254 caracteres; `refreshToken`: até 255. Acima → `VALIDATION_ERROR` (400).
- Corpo da requisição de qualquer rota: até 64 KB → `413 PAYLOAD_TOO_LARGE` (`app.request.max-body-bytes`).

## Referências
- ADR-0001 (autenticação JWT)

## Definição de pronto
- [x] Teste cobrindo o fluxo principal (login)
- [x] Um teste para CADA fluxo alternativo do login
- [x] Teste cobrindo o fluxo secundário (refresh bem-sucedido, com rotação)
- [x] Um teste para CADA fluxo alternativo do refresh (não encontrado/expirado, reuso)