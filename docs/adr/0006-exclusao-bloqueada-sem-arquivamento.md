# ADR-0006: Exclusão bloqueada em vez de arquivamento

## Contexto
O documento de domínio do ShotTrack sugere que itens do acervo com histórico
de uso (armas, e por extensão munições e acessórios) não deveriam ser
excluídos definitivamente, e sim arquivados. O UC08 (excluir arma), já
implementado antes dessa leitura, adota uma abordagem diferente: bloqueia a
exclusão (erro `WEAPON_IN_USE`) quando há histórico, e só permite excluir
de fato quando não há nenhum uso registrado. Isso ficou registrado como
conflito em aberto.

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
- UC08 (armas) permanece como está, sem alteração de código.
- UC16 (excluir munição) e UC21 (excluir acessório) seguem o mesmo padrão
  de erro `*_IN_USE`.
- Nenhum dos três domínios precisa de campo `arquivado`/`ativo` nas
  entidades.

## Referências
- ADR-0004 (revisão pós-QA — mesmo raciocínio aplicado ao cadastro rígido de arma)
- UC08 (excluir arma)