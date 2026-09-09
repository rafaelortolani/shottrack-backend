# UC11 - Consultar catálogo de modalidades

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita a lista de modalidades do catálogo
2. Sistema retorna todas as modalidades cadastradas, pra popular a seleção
   de modalidades praticadas (UC12)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0005 (modalidade como catálogo fixo)