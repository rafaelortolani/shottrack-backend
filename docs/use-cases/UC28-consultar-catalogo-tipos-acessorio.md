# UC28 - Consultar catálogo de tipos de acessório

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita a lista de tipos de acessório do catálogo
2. Sistema retorna todos os tipos cadastrados, pra popular o cadastro de
   acessório (UC17/UC20)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0008 (acessório — tipo como catálogo fechado, revisão)
- ADR-0004 (mesmo padrão de catálogo fechado)