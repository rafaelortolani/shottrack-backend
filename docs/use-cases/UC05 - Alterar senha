# UC05 - Alterar senha

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta envia senha atual e nova senha
2. Sistema valida a senha atual contra o hash salvo
3. Sistema valida a nova senha (mínimo 8 caracteres, mesma regra do UC01)
4. Sistema gera novo hash (BCrypt) e substitui o antigo

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 2a. Senha atual incorreta → erro `INVALID_CURRENT_PASSWORD`
- 3a. Nova senha não atende aos critérios → erro de validação
- 3b. Nova senha igual à atual → erro `PASSWORD_UNCHANGED`

## Definição de pronto
- [ ] Teste cobrindo o fluxo principal
- [ ] Teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)