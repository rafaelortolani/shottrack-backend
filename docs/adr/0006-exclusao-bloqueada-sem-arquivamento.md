# ADR-0006: Exclusão bloqueada em vez de arquivamento

## Contexto
O documento de domínio do ShotTrack sugere que itens do acervo com histórico
de uso (armas, e por extensão munições e acessórios) não deveriam ser
excluídos definitivamente, e sim arquivados. O UC08 (excluir arma), já
implementado antes dessa leitura, adota uma abordagem diferente: bloqueia a
exclusão (erro `WEAPON_IN_USE`) quando há histórico, e só permite excluir
de fato quando não há nenhum uso registrado. Isso ficou registrado como
conflito em aberto.

Depois de implementado, um caso não previsto apareceu na prática: excluir
uma arma que tem acessório associado (ADR-0008) causava erro 500 (violação
de constraint no banco), em vez de um comportamento de negócio definido —
o ADR-0008 só havia decidido o lado inverso (excluir acessório remove as
associações), não o lado da arma.

## Decisão
Confirma-se a abordagem já implementada — **bloquear, não arquivar** — como
decisão definitiva, válida para Armas (já implementado, sem mudança de
código necessária), Munições e Acessórios (a implementar já seguindo este
padrão):

- Item nunca usado em nenhuma série/treino → exclusão definitiva permitida.
- Item já usado → exclusão bloqueada com erro específico (ex:
  `WEAPON_IN_USE`, `AMMUNITION_IN_USE`, `ACCESSORY_IN_USE`); nenhuma opção
  de "arquivar" é oferecida.

Para Acessório especificamente: a **associação** a uma ou mais armas não
conta como "uso" para efeito desta regra — remover um acessório remove
junto suas associações com armas, sem bloqueio. O bloqueio só se aplica se
o acessório já tiver sido usado em alguma série (domínio que ainda não
existe).

**Decisão simétrica pro lado da arma**: excluir uma arma que tem acessório(s)
associado(s) também não é bloqueado — a exclusão remove as associações
junto (cascata), pelo mesmo motivo: associação não é "uso".

## Alternativas consideradas
- Arquivamento (sugestão do documento de domínio) → rejeitado: adicionaria
  um estado a mais (`arquivado`) em cada entidade do acervo, telas e filtros
  para itens arquivados, e uma decisão de UX sobre como reativar — custo que
  não se justificou depois de usar o sistema de verdade (ver ADR-0004, seção
  de revisão pós-QA, para o raciocínio equivalente aplicado a Armas).
- Exclusão sempre permitida, mesmo com histórico → rejeitado: destruiria
  dado histórico necessário pra estatísticas e evolução do atleta, indo
  contra o princípio "o treino é a fonte da verdade".

## Consequências
- UC08 (armas): corrigido — excluir uma arma associada a acessório agora
  completa a exclusão em cascata, via `ON DELETE CASCADE` na constraint de
  `weapon_id` em `accessory_weapons` (nenhuma alteração de código no
  usecase/gateway foi necessária).
- UC21 (acessórios): o mesmo tipo de bug apareceu no lado inverso — excluir
  um acessório associado a arma(s) também retornava 500. A causa não era só
  a ausência de `ON DELETE CASCADE` em `accessory_weapons.accessory_id`: o
  código já tentava remover as associações explicitamente antes de excluir
  o acessório, mas a ordem de flush do Hibernate entre as duas entidades
  (sem relação JPA mapeada entre si) não é garantida, e às vezes emitia o
  `DELETE` de `accessories` antes do de `accessory_weapons`. Um teste de
  integração já cobria esse cenário e passava mesmo assim — o `@Transactional`
  do teste mantém tudo numa única transação/flush, o que mascarou o bug que
  só aparecia com transações separadas por requisição, como em produção.
  Corrigido do mesmo jeito que UC08: `ON DELETE CASCADE` na constraint de
  `accessory_id`, removendo a necessidade do código explícito (e do próprio
  método de gateway/repositório que fazia isso).
- UC16 (excluir munição) segue o mesmo padrão de erro `*_IN_USE` pra uso em
  série; não tem uma relação N:N equivalente à de arma/acessório, então não
  tem esse caso de cascata.
- Nenhum dos três domínios precisa de campo `arquivado`/`ativo` nas
  entidades.
- Lição gerada: teste de integração `@Transactional` que encadeia várias
  chamadas HTTP no mesmo método pode não detectar bug de ordem de flush
  entre entidades sem relação JPA mapeada, porque tudo fica na mesma
  transação. Cascata via `ON DELETE CASCADE` no banco evita depender da
  ordem em que o Hibernate decide fazer flush.

## Referências
- ADR-0004 (revisão pós-QA — mesmo raciocínio aplicado ao cadastro rígido de arma)
- UC08 (excluir arma)