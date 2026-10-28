# UC24 - Cadastrar local de treino

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta informa nome, cidade, estado e, opcionalmente, se o local é um clube
   (`club`, boolean, padrão `false`)
2. Sistema valida os campos e cria o local vinculado ao atleta autenticado

## Fluxos alternativos
- 1a. Algum campo obrigatório vazio → erro de validação
- 1b. Token ausente ou inválido → erro `UNAUTHORIZED`

## Observação
`club` indica que o local é um clube e é usado na apuração de habitualidade
(ADR-0023). Locais anteriores a este campo ficam com `club = false`.

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima
- [x] Teste cadastrando local com `club = true` e conferindo na resposta
- [x] Teste cadastrando sem informar `club` (assume `false`)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0010 (local de treino livre, estrutura pronta pra clubes futuros)
- ADR-0023 (habitualidade configurável)
