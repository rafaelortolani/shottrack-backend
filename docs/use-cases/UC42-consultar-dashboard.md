# UC42 - Consultar dashboard (v7 — substitui a v6)

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Rotas
- `GET /api/dashboard?modalityId=` — abas Resumo e Recordes. `modalityId` é opcional
  (UUID de modalidade praticada pelo atleta).
- `GET /api/dashboard/modalities` — aba Modalidades (sem filtro).

## Fluxo principal (`GET /api/dashboard`)
Devolve apenas dados do atleta logado.
1. Atleta solicita o dashboard, com ou sem filtro de modalidade
2. Sistema valida o filtro (modalidade praticada pelo atleta)
3. Sistema lê recordes e estatísticas por modalidade do resumo pré-calculado em
   `dashboard_summary` (ADR-0015), calcula na hora o que depende da data de hoje
   (indicadores do mês) ou de dados sem evento de recálculo (onboarding, ação
   principal, últimos treinos) e retorna:
    - **Onboarding** (ver seção própria) — só presente se houver pendência
    - **Ação principal** — visita em andamento (local, modalidades dos treinos
      ativos) ou, se não houver, um convite genérico pra iniciar. Com mais de uma
      visita em andamento, vale a mais recente. Não depende do filtro
    - **Indicadores** `trainingsThisMonth`, `shotsThisMonth` e `modalitiesPracticed`,
      cada um `{ value, previous }`:
        - período atual = dia 1 do mês até hoje (inclusive);
        - período anterior = dia 1 do mês anterior até o MESMO dia do mês, limitado
          ao último dia daquele mês (hoje = 31/03 → 01/02 a 28/02);
        - treinos e disparos respeitam `modalityId`; `modalitiesPracticed` (nº de
          modalidades distintas com treino no período) ignora o filtro;
        - o dia vem do início do treino, em UTC (mesmo critério da evolução, UC46);
          os disparos são os das séries de treinos iniciados no período;
        - sem dados no anterior → `previous` = 0 (o front decide como exibir)
    - **Últimos treinos** (3 mais recentes, antes 5), filtrados por modalidade: data,
      local, modalidade e a métrica de destaque daquele treino (se tiver resultado)
    - **Recordes** (lista completa, ordenada pelo nome do tipo), filtrados por
      modalidade quando informada. O Resumo exibe os 3 primeiros; a aba Recordes usa
      a lista inteira — o front corta. Sem filtro: recordes do atleta inteiro
      (ADR-0016); com filtro: melhor valor de cada tipo elegível NAQUELA modalidade
    - **`suggestedEvolution`** `{ modalityId, resultTypeId }` — o tipo de resultado
      com mais registros válidos no filtro atual (sem filtro: o par modalidade+tipo
      mais registrado; desempate pelo nome do tipo). Só sugere tipo ainda configurado
      na modalidade (UC30), porque o UC46 rejeita os demais. `null` se não houver
      dados. O front usa para chamar o UC46 na mini evolução do Resumo

## Fluxo — Modalidades (`GET /api/dashboard/modalities`)
Lista as modalidades em que o atleta já treinou, ordenadas pelo nome: `modalityId`,
nome, quantidade de treinos e melhor valor do tipo de resultado mais registrado
NAQUELA modalidade (`best`, ausente se não houver resultado elegível). Ignora filtro.

## Removido nesta versão
- Resumo de acervo (`weaponCollection`; o Acervo tem tela própria).
- Resumo de modalidades dentro do dashboard (migrou para `/api/dashboard/modalities`).
- Lista `practicedModalities` (substituída pelo indicador `modalitiesPracticed`).

## Onboarding — pendências de configuração inicial
Calculado a partir do estado real do atleta, sem tabela de progresso
própria:
- "Criar perfil": completo se o atleta já editou o perfil ao menos uma
  vez (UC04). `experienceLevel` não serve de critério: tem default
  `BEGINNER` desde o cadastro, então estaria sempre preenchido
- "Configurar modalidades": completo se há pelo menos 1 modalidade
  praticada (UC12)
- "Cadastrar arma": completo se há pelo menos 1 arma no acervo (UC06)

A seção de onboarding só aparece na resposta se **pelo menos uma**
pendência existir. Assim que as três estiverem completas, some da
resposta (não é um campo com "false" pra sempre, é ausência).

## Destaque (por modalidade e por treino)
Entre os tipos de resultado com orientação `MENOR_MELHOR` ou
`MAIOR_MELHOR` (ADR-0011), o que tem mais registros preenchidos; valor é
o melhor já registrado nesse tipo, respeitando a orientação; desempate
pelo mais usado recentemente; ausente se nenhum tipo elegível tiver
registro ainda. Usado na aba Modalidades e nos últimos treinos —
no nível do atleta inteiro, foi substituído pela lista de recordes.

## Últimos treinos — métrica de destaque por treino
Mesma regra de destaque acima, escopada só às séries daquele treino
específico. Se o treino não tem nenhum resultado
registrado, o campo de métrica vem ausente (a tela mostra só data/local/
modalidade, sem inventar métrica).

## Pré-cálculo e fallback (ADR-0015, ADR-0022)
- Pré-calculado em `dashboard_summary` (recalculado por evento): recordes, estatísticas
  por modalidade e recordes por modalidade (alimentam o filtro e `suggestedEvolution`).
- Sob demanda: indicadores do mês. Dependem da data de hoje (janela móvel) e são
  contagens indexadas (visits.user_id → trainings.visit_id → series.training_id).
- 3a. Não existe linha em `dashboard_summary` pra esse atleta ainda → calcula tudo na
  hora (mesma lógica do consumidor), também com filtro; o fallback nunca grava.

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Atleta sem nenhum dado ainda → onboarding completo aparece (3 pendências),
  indicadores `{0, 0}`, listas vazias, `suggestedEvolution` null, sem erro
- 2a. `modalityId` de modalidade não praticada pelo atleta → erro
  `MODALITY_NOT_PRACTICED` (404)
- 2b. `modalityId` que não é UUID → erro de validação

## Observação
Valor de resultado é texto (ADR-0013) — em qualquer cálculo de "melhor valor" (geral,
por modalidade, por treino), valor não numérico é ignorado silenciosamente. Melhor
valor respeita a orientação do tipo (ADR-0011).

## Definição de pronto
- [x] Onboarding com 3 pendências, com 1 restante e ausente (tudo completo)
- [x] Ação principal com visita ativa e sem visita ativa (convite genérico)
- [x] Últimos treinos: 3 mais recentes, treino sem resultado (métrica ausente), filtro
- [x] Indicadores com `previous` corretos (mês anterior no mesmo dia do mês, mês
  anterior mais curto/janeiro em `MonthWindowsTest`, sem dados no anterior)
- [x] Filtro por modalidade em indicadores, últimos treinos e recordes (fallback e
  resumo pré-calculado)
- [x] `modalitiesPracticed` ignora o filtro
- [x] `suggestedEvolution` correto, só com tipo configurado, e `null` sem dados
- [x] `weaponCollection`/`modalitySummaries`/`practicedModalities` removidos
- [x] `GET /api/dashboard/modalities` ignora filtro (inclusive `modalityId` enviado)
- [x] Fallback com filtro (sem gravar); modalidade não praticada; sem token; isolamento
  entre atletas
- [x] Swagger atualizado (`@Operation` nas rotas do dashboard)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0011 (orientação de melhor valor por tipo)
- ADR-0013 (Série — resultado armazenado como texto)
- ADR-0015 (dashboard pré-calculado via fila)
- ADR-0016 (recordes e evolução)
- ADR-0022 (dashboard em abas)
- UC06 (armas), UC12 (modalidades praticadas), UC30 (perfil de modalidade),
  UC31-35 (visita/treino), UC46 (evolução)
