# ADR-0014: Consistência entre disparos e acertos/erros

## Contexto
Bug relatado: atleta registrou `quantidadeDisparos = 50` numa série e, em
seguida, `Acertos = 45` e `Erros = 10` — soma 55, maior que os disparos
informados. O sistema aceitou, porque `quantidadeDisparos` (campo da
Série) e os resultados `Acertos`/`Erros` (linhas separadas em
`series_results`, ADR-0013) nunca são comparados entre si; cada um só
valida o próprio formato.

`quantidadeDisparos` é opcional (ADR-0013, "registrar primeiro, organizar
depois"), então a regra não pode simplesmente exigi-lo antes de aceitar
acertos/erros — isso contrariaria o princípio central do produto.

## Decisão
`Acertos` e `Erros` são os únicos tipos de resultado do catálogo
(ADR-0011) que contam disparos da série — tratados como caso especial,
hardcoded, no mesmo lugar onde o formato de cada tipo já é interpretado
(`SeriesResultService`).

**Formato**: `Acertos`/`Erros` precisam ser inteiro não-negativo (antes
aceitavam qualquer `BigDecimal`, inclusive decimal/negativo, igual aos
demais tipos numéricos) — decimal não faz sentido pra contagem de
disparos.

**Série** ganha uma segunda informação sobre `quantidadeDisparos`, além do
valor: se foi **informada manualmente** pelo atleta (na criação, UC36, ou
edição, UC38) ou não (`Series.shotCountSetManually`, coluna
`shot_count_set_manually`).

- **Enquanto não informada manualmente**: a cada mudança em
  `Acertos`/`Erros` (registrar, marcar não aplicável, ou remover — UC39/
  UC40), `quantidadeDisparos` é **recalculada automaticamente** pra soma
  dos dois (ausente/não aplicável conta como 0). Nunca há erro possível
  nesse modo — o valor sempre acompanha a soma.
- **Depois de informada manualmente**: passa a ser um teto. Um novo
  registro de `Acertos`/`Erros` cuja soma ultrapasse
  `quantidadeDisparos` é rejeitado (`RESULT_EXCEEDS_SHOT_COUNT`). A trava
  é permanente — não existe caminho de volta pro modo automático (ver
  Alternativas).
- **Editar `quantidadeDisparos` (UC38) pra um valor menor que a soma já
  registrada de `Acertos`+`Erros`**: rejeitado
  (`SHOT_COUNT_LESS_THAN_REGISTERED_RESULTS`) — o atleta precisa ajustar
  acertos/erros primeiro, o sistema nunca muta esses valores sozinho.

## Alternativas consideradas
- Só bloquear quando `quantidadeDisparos` já estiver preenchida, sem
  auto-preencher no caso contrário → rejeitado: deixaria o caso mais comum
  do bug relatado (nenhum dos três preenchido na ordem "certa") sem
  proteção nenhuma, já que a maioria dos atletas de Trap/Skeet (ADR-0011)
  provavelmente preenche acertos/erros antes de pensar em "quantidade de
  disparos" como campo separado.
- Ajustar `Acertos`/`Erros` automaticamente ao reduzir `quantidadeDisparos`
  (UC38) → rejeitado: mutação silenciosa de valores que o atleta digitou
  explicitamente é mais surpreendente do que um erro pedindo confirmação;
  quebraria o princípio de nunca alterar um registro sem ação explícita do
  atleta (mesmo espírito de ADR-0013 sobre os 3 estados de resultado).
  Bloquear é reversível (o atleta tenta de novo); auto-ajustar não é.
- Permitir "destravar" `shotCountSetManually` (voltar pro modo automático)
  → não implementado por ora: nenhum fluxo do produto pede isso; se surgir
  necessidade real, é extensão aditiva (não quebra o que já existe).

## Consequências
- Nova coluna `series.shot_count_set_manually` (migration V35, boolean not
  null default false, backfill `true` onde `shot_count` já não era nulo —
  aproximação razoável pra dados existentes, já que a migration não tem
  como saber retroativamente se o valor veio do atleta ou de um cálculo).
- `Series.shotCount` deixa de ter setter direto — só muda via
  `setShotCountManually` (UC36/UC38, trava o modo automático) ou
  `autoFillShotCount` (UC39/UC40, nunca muda a trava), mesmo padrão já
  usado em `SeriesResult.value`/`notApplicable` (ADR-0013).
- `SeriesResultService` ganha uma dependência implícita entre os tipos
  "Acertos" e "Erros" — primeira vez que um tipo de resultado depende de
  outro; aceitável enquanto for só esse par (catálogo pequeno e fechado,
  ADR-0011), mas se mais relações desse tipo surgirem vale revisitar como
  modelar (hoje é condicional direto por nome, sem tabela de metadados).

## Referências
- ADR-0011 (catálogo fixo de tipos de resultado)
- ADR-0013 (Série — registro incompleto permitido, resultados com 3 estados)
- UC36 (registrar série), UC38 (editar série), UC39 (registrar resultado),
  UC40 (remover resultado)
