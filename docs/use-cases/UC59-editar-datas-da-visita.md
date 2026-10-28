# UC59 - Editar datas da visita

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Visita pertence ao atleta.

## Rota
`PATCH /api/visits/{id}/dates` { startedAt, endedAt } (campos opcionais, edição parcial)

## Fluxo principal
1. Atleta envia a nova data/horário de início e/ou de fim
2. Sistema valida e atualiza apenas os campos enviados
3. Sistema dispara o recálculo do dashboard (ADR-0015) e devolve a visita atualizada

## Regras
- Início ≤ fim; fim não pode ser futuro; início não pode ser futuro nem anterior a
  12 meses.
- Visita `EM_ANDAMENTO` só aceita `startedAt`; `endedAt` → erro de validação.
- Visita `ENCERRADA` aceita os dois.
- Os horários dos treinos e séries não são alterados; a data de referência de todas as
  apurações é a data de início da visita (ADR-0024).
- Apuração por data muda na hora (habitualidade e evolução são sob demanda).

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Visita não encontrada ou de outro atleta → erro `VISIT_NOT_FOUND`
- 2a. Data no futuro → `VISIT_DATE_IN_FUTURE`
- 2b. Início anterior a 12 meses → `VISIT_DATE_TOO_OLD`
- 2c. Início depois do fim → `VISIT_DATE_RANGE_INVALID`
- 2d. `endedAt` em visita em andamento → erro de validação
- 2e. Nenhum campo enviado → erro de validação

## Definição de pronto
- [x] Editar só início; só fim; os dois
- [x] Um teste para CADA fluxo alternativo
- [x] Limite de 12 meses (aceita/recusa)
- [x] Recálculo do dashboard disparado e resultado correto após a edição
- [x] Habitualidade, evolução e meta pessoal refletem a nova data (movendo uma visita entre janelas/anos)
- [x] Isolamento entre atletas

## Decisões de implementação
- Corpo: ISO-8601 **com fuso** (`Z` ou offset, ex.: `2026-10-01T10:00:00-03:00`); sem fuso → `VALIDATION_ERROR`.
- Ordem das validações: visita (`VISIT_NOT_FOUND`) → corpo vazio → `endedAt` em visita em andamento →
  futuro → 12 meses → ordem início ≤ fim.
- "12 meses" é por dia (UTC): vale a partir das 00:00 UTC de hoje menos 12 meses.
- Tolerância de relógio: data até **5 minutos** à frente do servidor é aceita e ajustada para "agora"
  (cliente com relógio adiantado); acima disso → `VISIT_DATE_IN_FUTURE`.
- Resposta: a visita completa (mesmo formato do UC35). `PATCH /api/visits/{id}/dates` publica o evento de
  recálculo do dashboard (entregue após o commit).

## Swagger e erros
`@Operation` em `VisitController.editDates`. Erros: `UNAUTHORIZED` (401), `VISIT_NOT_FOUND` (404),
`VISIT_DATE_IN_FUTURE`, `VISIT_DATE_TOO_OLD`, `VISIT_DATE_RANGE_INVALID`, `VALIDATION_ERROR` (400).
Mensagens pt/en em `messages*.properties`.

## Referências
- ADR-0001, ADR-0012, ADR-0015, ADR-0023, ADR-0024
- UC31 (iniciar visita), UC34 (encerrar visita)
