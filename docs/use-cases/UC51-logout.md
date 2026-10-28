# UC51 - Sair (logout)

## Ator
Atleta, com ou sem access token válido.

## Fluxo principal
1. Cliente envia o refresh token da sessão
2. Sistema revoga esse refresh token
3. Sistema responde sucesso

## Fluxos alternativos
- 2a. Refresh token desconhecido, já revogado ou expirado → mesma resposta de
  sucesso (idempotente). Reenviar um token já revogado aqui NÃO derruba as
  outras sessões (diferente do `/refresh`, onde o reuso indica roubo).
- 1a. Refresh token ausente ou em branco → `VALIDATION_ERROR`

## Observações
- Rota pública (`POST /api/auth/logout`): não exige access token, para
  funcionar com o access token já expirado. O access token em si continua
  válido até expirar (JWT stateless, ADR-0001) — só a renovação é cortada.
- Resposta: `200 OK` com `{"data": null}`.

## Definição de pronto
- [x] Teste: logout revoga o refresh token (refresh seguinte falha)
- [x] Teste: funciona sem access token / com access token inválido
- [x] Teste: idempotente (token já revogado, desconhecido e expirado)
- [x] Teste: reenviar token já revogado não derruba as outras sessões
- [x] Teste: refresh token ausente e em branco → `VALIDATION_ERROR`

## Referências
- ADR-0018, ADR-0001
