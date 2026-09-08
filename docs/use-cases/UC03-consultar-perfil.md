# UC03 - Consultar perfil

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita seus próprios dados
2. Sistema retorna nome, email e data de cadastro (nunca senha/hash)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Teste cobrindo o fluxo alternativo (1a)

## Referências
- ADR-0001 (autenticação JWT)