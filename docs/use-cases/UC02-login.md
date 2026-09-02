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
- 2a. Refresh token não encontrado ou expirado → erro genérico
  "refresh token inválido"
- 2b. Refresh token já foi usado antes (reuso) → tratado como sinal de roubo:
  todos os refresh tokens do atleta são revogados, erro "refresh token
  inválido", atleta precisa logar de novo (ADR-0001)

## Referências
- ADR-0001 (autenticação JWT)

## Definição de pronto
- [x] Teste cobrindo o fluxo principal (login)
- [x] Um teste para CADA fluxo alternativo do login
- [x] Teste cobrindo o fluxo secundário (refresh bem-sucedido, com rotação)
- [x] Um teste para CADA fluxo alternativo do refresh (não encontrado/expirado, reuso)