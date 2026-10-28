# UC42 v7 — Consultar dashboard (SUBSTITUI a v6)

## Ator
Atleta autenticado.

## Rotas
- `GET /api/dashboard?modalityId=` — Resumo e Recordes (filtro opcional).
- `GET /api/dashboard/modalities` — aba Modalidades (sem filtro).

## Fluxo principal (`GET /api/dashboard`)
Devolve apenas dados do atleta logado.
1. **Ação principal**: como na v6 (iniciar/continuar treino, etc.).
2. **Indicadores** (respeitam `modalityId` quando informado, exceto "modalidades
   praticadas", que sempre considera todas):
   - `trainingsThisMonth` { value, previous }
   - `shotsThisMonth` { value, previous }
   - `modalitiesPracticed` { value, previous }
   Período atual: dia 1 do mês até hoje. Período anterior: dia 1 do mês anterior até o
   mesmo dia do mês (limitado ao último dia daquele mês). Sem dados no anterior →
   `previous` = 0 (o front decide como exibir).
3. **Últimos treinos**: 3 mais recentes (v6 trazia 5), filtrados por modalidade, cada um
   com a métrica por treino já existente.
4. **Recordes**: lista plural já existente, filtrada por modalidade quando informada
   (a partir de `dashboard_summary_modality_stats`). O Resumo exibe os 3 primeiros;
   a aba Recordes usa a lista completa — por isso o endpoint devolve a lista inteira e o
   front corta.
5. **Mini evolução do Resumo**: não é do UC42; o front chama o UC46 (tipo de resultado
   mais registrado). Para saber qual é, o UC42 devolve `suggestedEvolution`
   { modalityId, resultTypeId } (resultado mais registrado no filtro atual; `null` se
   não houver dados).
6. **Onboarding**: como na v6 (só com pendência; no front vive em Boas-vindas).

## Removido nesta versão
- `weaponsSummary` / resumo de acervo.
- Resumo de modalidades dentro do Resumo (migrou para o fluxo abaixo).

## Fluxo — Modalidades (`GET /api/dashboard/modalities`)
Lista as modalidades praticadas: nome, quantidade de treinos e melhor valor do tipo de
resultado mais registrado (como o resumo de modalidades da v6). Ignora filtro.

## Fluxos alternativos
- `modalityId` de modalidade não praticada → `MODALITY_NOT_PRACTICED`.
- Sem token → `UNAUTHORIZED`.
- Sem nenhum treino → 200 com indicadores zerados, listas vazias, `suggestedEvolution`
  null.
- Pré-cálculo ausente/defasado → fallback de cálculo direto (ADR-0015), agora também com
  filtro.

## Observações
- Valores de resultado continuam texto (ADR-0013); melhor valor respeita a orientação
  do tipo (ADR-0011).
- Dias do período anterior são comparados dia a dia no fuso do atleta (usar o mesmo
  critério de fuso que o projeto já usa em "treinos do mês").

## Definição de pronto
- [ ] Indicadores com `previous` corretos (virada de mês, mês anterior mais curto, janeiro)
- [ ] Filtro por modalidade em indicadores, últimos treinos e recordes
- [ ] `modalitiesPracticed` ignora filtro
- [ ] `suggestedEvolution` correto e null sem dados
- [ ] `weaponsSummary` removido; Swagger atualizado
- [ ] `GET /api/dashboard/modalities` ignora filtro
- [ ] Fallback com filtro; modalidade não praticada; sem token; isolamento entre atletas
