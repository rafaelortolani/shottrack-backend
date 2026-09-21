# ADR-0011: Tipo de Resultado como catálogo fixo, com sugestão padrão por modalidade

## Contexto
O documento de domínio descreve o "Perfil da Modalidade": o atleta escolhe
quais tipos de resultado quer acompanhar em cada modalidade que pratica
(ex: alguém de Precisão pode querer pontuação+tempo+agrupamento; outro só
agrupamento). O documento também sugere: *"O sistema pode fornecer uma
configuração inicial para facilitar o primeiro uso."*

## Decisão
**Tipo de Resultado** é catálogo fixo, mesmo padrão já usado em outros
domínios (seed via migration, sem endpoint de criação pelo app):
pontuação, tempo, agrupamento, acertos, erros, penalidades, fator de
desempenho, exercício concluído, anotação livre.

**Sugestão padrão por modalidade**: uma tabela de seed separada mapeia
cada modalidade do catálogo (ADR-0005) a um conjunto sugerido de tipos de
resultado:
- Precisão: pontuação, agrupamento
- IPSC: tempo, pontuação, fator de desempenho
- Steel Challenge: tempo
- Trap: acertos, erros
- Skeet: acertos, erros
- Saque e tiro: tempo, acertos

Quando o atleta adiciona uma modalidade praticada (UC12), o sistema aplica
automaticamente essa sugestão como ponto de partida — gravada como
seleção **do atleta** (não uma referência à sugestão), editável livremente
depois (UC30), sem nenhuma obrigatoriedade de manter os tipos sugeridos.

## Alternativas consideradas
- Começar vazio, sem sugestão nenhuma → rejeitado: o próprio documento
  pede configuração inicial pra facilitar o primeiro uso; começar vazio
  empurra decisão de configuração pro atleta antes dele ter usado o
  produto o suficiente pra saber o que quer.
- Sugestão calculada dinamicamente (ex: por popularidade entre outros
  atletas) → rejeitado: não há dado nenhum ainda pra calcular isso;
  tabela fixa de seed é suficiente e muito mais simples.

## Consequências
- Precisa de duas tabelas de seed: tipos de resultado, e o mapeamento
  modalidade → tipos sugeridos.
- UC12 (adicionar modalidade praticada) passa a, internamente, também
  criar as seleções de tipo de resultado a partir da sugestão — sem mudar
  a interface pública do UC12 (o atleta só vê o resultado, não pede a
  sugestão explicitamente).
- Selecionar/remover tipos de resultado de uma modalidade já praticada é
  ação separada (UC30) — mesma lógica de UC12 ser separado de UC04
  (relação, não campo simples).

## Referências
- ADR-0004, ADR-0005 (mesmo padrão de catálogo fechado)
- UC12 (adicionar modalidade praticada — gatilho da sugestão padrão)
- UC29 (catálogo de tipos de resultado), UC30 (ajustar perfil de modalidade)