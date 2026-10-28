# UC31 - Iniciar visita

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Local de treino existe e
pertence ao atleta (UC24).

## Fluxo principal
1. Atleta informa o local (localId) e, opcionalmente, observações, o tipo da
   visita (`visitType`: `TREINO`, padrão, ou `COMPETICAO`), só para competição o
   âmbito (`competitionScope`: `ESTADUAL`, `NACIONAL` ou `INTERNACIONAL`) e a data e
   o horário em que a visita começou (`startedAt`; padrão = agora)
2. Sistema valida que o local existe e pertence ao atleta, que o âmbito só foi
   informado para `COMPETICAO` e que `startedAt` não é futuro nem anterior a 12 meses
3. Sistema cria a visita com status `EM_ANDAMENTO`, `iniciada_em` = `startedAt`
   (ou agora, se não informado)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. LocalId não informado → erro de validação
- 1c. `competitionScope` informado com `visitType = TREINO` → erro de validação
- 1d. `visitType` ou `competitionScope` com valor fora dos aceitos → erro de validação
- 1e. `startedAt` no futuro → erro de validação (`VISIT_DATE_IN_FUTURE`)
- 1f. `startedAt` anterior a 12 meses → erro de validação (`VISIT_DATE_TOO_OLD`)
- 2a. Local não encontrado ou pertence a outro atleta → erro `TRAINING_LOCATION_NOT_FOUND`

## Observação
Tipo e âmbito alimentam a apuração de habitualidade (ADR-0023). Visitas
anteriores a esse campo são `TREINO`, sem âmbito. A competição pode ser criada
sem âmbito; nesse caso ela não conta em exigências que pedem âmbito mínimo.
A data da visita (`iniciada_em`) é a data de referência de dashboard, evolução,
recordes, habitualidade e meta pessoal (ADR-0024). Criar visita com data passada
dispara o recálculo do dashboard (ADR-0015).

## Decisões de implementação
- `startedAt` é ISO-8601 com fuso. Mesmas regras de 12 meses (por dia, UTC) e tolerância de relógio
  (5 minutos, ajustado para "agora") do UC59.
- A data de início é a coluna `visits.started_at` (V53), separada do `created_at` de auditoria.
  Apurações que passaram a usar a data da visita: indicadores do mês (treinos, disparos), evolução (UC46),
  histórico (UC47), comparação de armas, últimos treinos e ação principal do dashboard, habitualidade (UC57),
  meta pessoal (UC58) e exportação (UC53).
- Swagger: `@Operation` em `VisitController.start`. Erros: `VISIT_DATE_IN_FUTURE`, `VISIT_DATE_TOO_OLD`.

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima
- [x] Teste criando visita sem tipo (assume `TREINO`)
- [x] Teste criando visita `COMPETICAO` com e sem âmbito
- [x] Teste do fluxo alternativo 1c (âmbito com `TREINO`) e 1d (valores inválidos)
- [x] Teste criando visita com `startedAt` passado e conferindo `iniciada_em`
- [x] Teste de `startedAt` no limite de 12 meses (aceita) e além (1f)
- [x] Teste de `startedAt` futuro (1e)
- [x] Teste confirmando que a visita retroativa entra no dashboard/habitualidade na data informada

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0012 (Visita e Treino)
- ADR-0023 (habitualidade configurável)
- ADR-0024 (visita retroativa e edição de datas)
