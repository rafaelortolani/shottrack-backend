# UC57 — Consultar progresso de habitualidade

## Ator
Atleta autenticado.

## Rota
`GET /api/habituality/progress`

## Fluxo principal
Devolve o acompanhamento do atleta:
- `rule` { id, name, legalReference }, `level` (nome), `windowMonths`, `periodStart`,
  `periodEnd` (hoje), `compliant` (todas as exigências cumpridas).
- `requirements[]`: descrição, `required`, `done`, `missing` e `met`.
- `upcomingChange` { validFrom } quando existe versão futura.
- `levelRequired` quando o nível escolhido não existe na versão vigente.
- `personalGoal` { yearlyTrainings, doneThisYear } (UC58), ou null.
- `disclaimer` não é devolvido; o texto fixo fica no front.

## Apuração
- Janela móvel: do dia equivalente a `windowMonths` meses atrás até hoje.
- Conta só visitas **encerradas** do atleta.
- `TREINO` conta visitas do tipo TREINO; `COMPETICAO` as de COMPETICAO; `QUALQUER` ambas.
- `onlyInClub`: só visitas em local marcado "é clube".
- `distinctEvents`: conta pares distintos (data, local).
- `minScope`: visita de competição com âmbito ≥ mínimo (ESTADUAL < NACIONAL <
  INTERNACIONAL); competição sem âmbito não conta.
- Cálculo sob demanda (ADR-0016).

## Fluxos alternativos
- Sem acompanhamento → 200 com `rule` null (o front convida a escolher).
- Regra inativa → 200 com `ruleInactive=true`, sem apuração.
- Sem token → `UNAUTHORIZED`.

## Definição de pronto
- [x] Cada tipo de exigência e cada filtro isolado e combinados
- [x] Janela móvel (bordas), só visitas encerradas, isolamento entre atletas
- [x] Sem acompanhamento; regra inativa; nível perdido; mudança futura

## Decisões de implementação
- A visita pertence à janela pelo dia de início (UTC); a janela inclui `periodStart` e hoje.
- `description` é montada no idioma da requisição (`Accept-Language`, padrão pt); os campos
  estruturados (`activityType`, `onlyInClub`, `distinctEvents`, `minScope`) vão junto.
- `minScope` em exigência `QUALQUER` só conta competições com âmbito suficiente (treino não tem âmbito).
- `ruleInactive` e `levelRequired` devolvem `compliant` e `requirements` nulos.

## Swagger e mensagens
- Rota anotada com `@Operation` em `HabitualityController`.
- Mensagens pt/en das descrições: `HABITUALITY_REQUIREMENT`, `HABITUALITY_ACTIVITY_*`,
  `HABITUALITY_FILTER_*`, `HABITUALITY_SCOPE_*`.
