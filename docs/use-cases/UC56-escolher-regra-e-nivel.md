# UC56 — Escolher regra e nível de habitualidade

## Ator
Atleta autenticado.

## Rotas
- `GET /api/habituality/rules` — regras ativas com os níveis e exigências da versão vigente
  (e, se houver, `upcomingVersion` { validFrom }).
- `PUT /api/habituality/tracking` { ruleId, levelName }
- `DELETE /api/habituality/tracking`

## Fluxo principal
Atleta escolhe uma regra ativa e um dos níveis da versão vigente. Há um acompanhamento
por atleta; um novo PUT substitui o anterior.

## Fluxos alternativos
- Regra inexistente ou inativa → `HABITUALITY_RULE_NOT_FOUND` / `HABITUALITY_RULE_INACTIVE`.
- Nível inexistente na versão vigente → `HABITUALITY_LEVEL_NOT_FOUND`.
- Regra sem versão vigente → não aparece na listagem.
- Sem token → `UNAUTHORIZED`.

## Mudança de versão
O nível é guardado pelo nome. Ao entrar em vigor uma nova versão: nível existente com o
mesmo nome → mantido; inexistente → o progresso sinaliza `levelRequired=true` até o atleta
escolher de novo. Com versão futura cadastrada, o progresso traz `upcomingChange`.

## Definição de pronto
- [x] Escolher, trocar, remover
- [x] Regra inativa/inexistente; nível inexistente; sem token
- [x] Nível mantido e nível perdido na troca de versão (teste com data de vigência)
- [x] Isolamento entre atletas

## Decisões de implementação
- Nível casado pelo nome sem diferenciar maiúsculas; o nome gravado é o da versão vigente.
- `HABITUALITY_RULE_INACTIVE` → 409; regra sem versão vigente no PUT → `HABITUALITY_VERSION_NOT_FOUND` (404).
- `DELETE /api/habituality/tracking` é idempotente.

## Swagger e mensagens
- Rotas anotadas com `@Operation` em `HabitualityController`.
- Mensagens pt/en: `HABITUALITY_RULE_NOT_FOUND`, `HABITUALITY_RULE_INACTIVE`,
  `HABITUALITY_VERSION_NOT_FOUND`, `HABITUALITY_LEVEL_NOT_FOUND`.
