# UC58 — Definir meta pessoal de treinos

## Ator
Atleta autenticado.

## Rotas
- `PUT /api/habituality/personal-goal` { yearlyTrainings }
- `DELETE /api/habituality/personal-goal`
(o valor e o progresso aparecem em `GET /api/habituality/progress`, campo `personalGoal`)

## Fluxo principal
Atleta define quantos treinos quer fazer no ano civil. O progresso conta visitas do tipo
TREINO encerradas entre 1º/jan e 31/dez do ano corrente. Independe de regra e nível.

## Fluxos alternativos
- Valor < 1 ou acima de 1000 → `VALIDATION_ERROR`.
- Sem token → `UNAUTHORIZED`.

## Definição de pronto
- [x] Definir, alterar, remover; validação; sem token; isolamento
- [x] Progresso conta só TREINO encerrado do ano corrente; virada de ano

## Decisões de implementação
- "Treinos" são as visitas TREINO encerradas, contadas pelo dia de início (UTC) no ano civil de hoje.
- O PUT devolve `{ yearlyTrainings, doneThisYear }`; o `DELETE` é idempotente.

## Swagger e mensagens
- Rotas anotadas com `@Operation` em `HabitualityController`; erro `VALIDATION_ERROR` (400) para valor fora de 1–1000.
