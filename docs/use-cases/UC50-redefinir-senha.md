# UC50 - Redefinir senha

## Ator
Atleta não autenticado, com o link recebido por e-mail (UC49).

## Fluxo principal
1. Atleta envia o token do link e a nova senha
2. Sistema valida o token (existe, não expirou, não foi usado) e a senha
   (mesmas regras do cadastro)
3. Sistema atualiza a senha, marca o token como usado e revoga todos os
   refresh tokens do usuário

## Fluxos alternativos
- 2a. Token inexistente → `PASSWORD_RESET_TOKEN_INVALID`
- 2b. Token expirado → `PASSWORD_RESET_TOKEN_EXPIRED`
- 2c. Token já usado → `PASSWORD_RESET_TOKEN_ALREADY_USED`
- 2d. Token substituído por um pedido mais novo → `PASSWORD_RESET_TOKEN_INVALID`
- 2e. Senha fora das regras → `VALIDATION_ERROR`

## Observações
- Sucesso: `200 OK` com `{"data": null}`. Senha, `used_at` do token e a
  revogação dos refresh tokens acontecem na mesma transação.
- Senha inválida (`VALIDATION_ERROR`) não consome o token.

## Definição de pronto
- [x] Teste do fluxo principal (login com a senha nova funciona, a antiga não)
- [x] Teste: refresh token anterior deixa de funcionar após a redefinição
- [x] Um teste para CADA fluxo alternativo (2a a 2e)
- [x] Teste: o mesmo token não pode ser usado duas vezes

## Limite de tentativas (ADR-0018)
`POST /api/auth/password-reset/confirm`: 10 tentativas / 15 min por IP → `429 TOO_MANY_REQUESTS`.

## Referências
- ADR-0018, ADR-0001
