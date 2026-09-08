# UC07 - Listar armas do atleta

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita a lista de armas cadastradas
2. Sistema retorna todas as armas vinculadas ao atleta autenticado (nunca as
   de outro atleta)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Atleta sem nenhuma arma cadastrada → retorna lista vazia (não é erro)

## Definição de pronto
- [x] Teste cobrindo o fluxo principal (com armas cadastradas)
- [x] Teste cobrindo o fluxo alternativo 1b (lista vazia)
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)
- [x] Teste garantindo que a lista nunca inclui arma de outro atleta

## Referências
- ADR-0001 (autenticação JWT)
