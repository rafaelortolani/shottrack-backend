# ADR-0008: Acessório — cadastro livre com associação N:N a armas

## Contexto
O documento de domínio descreve acessório como item usado junto aos
equipamentos (mira, red dot, luneta, empunhadura, carregador, apoio,
equipamento de proteção), que "pode ser associado a uma ou mais armas
quando essa relação for útil". Não há menção a catálogo fechado nem a
cadastro parcial específico para este domínio.

## Decisão
- Cadastro: `nome` (obrigatório), `tipoId` (catálogo fechado — ver revisão
  abaixo), `observações` (opcional).
- Associação **N:N** com armas: um acessório pode estar associado a várias
  armas, e uma arma pode ter vários acessórios — tabela de associação
  própria, com endpoints dedicados pra associar/desassociar.
- Remover um acessório remove suas associações junto (não é bloqueado por
  estar associado a uma arma — ver ADR-0006).

## Revisão — tipo passa a ser catálogo fechado
Decisão original (abaixo) tratava `tipo` como texto livre. Revisado: tipo
de acessório passa a ser **catálogo fechado**, mesmo padrão de
tipo/marca/modelo/calibre de arma (ADR-0004) — seed via migration, sem
endpoint de criação pelo app, consultável via UC28. Motivo: mesmo risco de
duplicidade/inconsistência já visto em outros catálogos ("mira" vs "Mira"
vs "MIRA"), e a lista de tipos de acessório de tiro esportivo é
razoavelmente estável (mira, red dot, luneta, empunhadura, carregador,
apoio, equipamento de proteção), não justificando texto livre.

## Alternativas consideradas
- `tipo` como texto livre (decisão original) → revertido: sem
  normalização, permite erro de digitação e duplicidade — mesmo problema
  que catálogo fechado já resolve em Arma e Munição.
- Associação 1:N (acessório pertence a uma única arma) → rejeitado: o
  documento é explícito em permitir "uma ou mais armas", e isso é realista
  (uma luneta pode ser trocada entre duas armas do mesmo calibre/rosca).

## Consequências
- Precisa de tabela de associação `weapon_accessory` (ou nome equivalente),
  além da tabela de acessório em si.
- Endpoints de associar/desassociar são separados dos de CRUD do acessório
  em si — mesmo raciocínio já aplicado a Modalidades praticadas (ADR-0005 +
  UC12): é uma relação, não um campo simples.
- UC17 (cadastrar) e UC20 (editar) passam a exigir `tipoId` válido em vez
  de `tipo` texto livre — mudança incompatível (breaking change) na API,
  já em produção de desenvolvimento; UC28 (novo) expõe o catálogo pra
  popular o seletor no frontend.
- Precisa de migration com seed inicial de tipos de acessório.

## Referências
- ADR-0006 (exclusão bloqueada, não arquivamento — aplica-se aqui)
- ADR-0005 / UC12 (mesmo padrão de relação N:N já usado em modalidades praticadas)
- ADR-0004 (mesmo padrão de catálogo fechado, aplicado aqui)
- UC28 (consultar catálogo de tipos de acessório)