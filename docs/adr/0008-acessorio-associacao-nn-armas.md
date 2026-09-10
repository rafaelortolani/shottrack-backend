# ADR-0008: Acessório — cadastro livre com associação N:N a armas

## Contexto
O documento de domínio descreve acessório como item usado junto aos
equipamentos (mira, red dot, luneta, empunhadura, carregador, apoio,
equipamento de proteção), que "pode ser associado a uma ou mais armas
quando essa relação for útil". Não há menção a catálogo fechado nem a
cadastro parcial específico para este domínio.

## Decisão
- Cadastro livre: `nome` (obrigatório), `tipo` (texto livre, opcional — ex:
  "mira", "red dot", sem catálogo fechado, já que a lista de tipos possíveis
  é aberta e não crítica pra nenhuma agregação hoje), `observações` (opcional).
- Associação **N:N** com armas: um acessório pode estar associado a várias
  armas, e uma arma pode ter vários acessórios — tabela de associação
  própria, com endpoints dedicados pra associar/desassociar.
- Remover um acessório remove suas associações junto (não é bloqueado por
  estar associado a uma arma — ver ADR-0006).

## Alternativas consideradas
- `tipo` como catálogo fechado (igual armas) → rejeitado por ora: o
  documento não sugere isso, e não há motivo de agregação/relatório que
  justifique o custo agora; pode ser revisitado se essa necessidade surgir.
- Associação 1:N (acessório pertence a uma única arma) → rejeitado: o
  documento é explícito em permitir "uma ou mais armas", e isso é realista
  (uma luneta pode ser trocada entre duas armas do mesmo calibre/rosca).

## Consequências
- Precisa de tabela de associação `weapon_accessory` (ou nome equivalente),
  além da tabela de acessório em si.
- Endpoints de associar/desassociar são separados dos de CRUD do acessório
  em si — mesmo raciocínio já aplicado a Modalidades praticadas (ADR-0005 +
  UC12): é uma relação, não um campo simples.

## Referências
- ADR-0006 (exclusão bloqueada, não arquivamento — aplica-se aqui)
- ADR-0005 / UC12 (mesmo padrão de relação N:N já usado em modalidades praticadas)