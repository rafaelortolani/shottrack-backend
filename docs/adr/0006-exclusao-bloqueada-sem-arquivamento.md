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
- UC08 (armas) **precisa de correção**: hoje excluir uma arma associada a
  acessório retorna 500 em vez de completar a exclusão em cascata. Corrigir
  removendo as linhas de `weapon_accessory` (ou tabela equivalente) antes
  de excluir a arma — via `ON DELETE CASCADE` na constraint, ou explicitamente
  no usecase/gateway, antes do delete da arma.
- UC16 (excluir munição) e UC21 (excluir acessório) seguem o mesmo padrão
  de erro `*_IN_USE` pra uso em série; nenhum dos dois tem uma relação
  N:N equivalente à de arma/acessório, então não têm esse caso de cascata.
- Nenhum dos três domínios precisa de campo `arquivado`/`ativo` nas
  entidades.

## Referências
- ADR-0004 (revisão pós-QA — mesmo raciocínio aplicado ao cadastro rígido de arma)
- UC08 (excluir arma)