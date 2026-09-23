# ADR-0016: Evolução sob demanda + recordes plurais (Dashboard Onda 2)

## Contexto
A Onda 2 do dashboard pede duas coisas de natureza diferente: (1)
recordes plurais — melhor valor de cada tipo de resultado com dado; (2)
gráfico de evolução, com modalidade + tipo de resultado + período
escolhidos interativamente pelo atleta.

## Decisão

### Recordes plurais
Estende `dashboard_summary` (ADR-0015): em vez de só o destaque geral (1
tipo), grava o melhor valor de **cada** tipo de resultado com pelo menos
um registro, respeitando a orientação (ADR-0011). Mesma infraestrutura de
fila/recálculo já existente — sem endpoint novo, só payload mais rico no
UC42.

### Evolução — sob demanda, não pré-calculada
Combinação de modalidade × tipo de resultado × período × modo de
agregação é escolhida pelo atleta na hora — pré-calcular todas as
combinações possíveis não compensa (explode rápido, a maioria nunca seria
vista). Endpoint novo, parametrizado, calculado na hora da consulta:

- **Período**: `7d` / `30d` / `3m` / `1a`
- **Agrupamento dos pontos**: sempre por **dia**, independente do
  período — inclusive pro período de 1 ano (365 pontos possíveis; dias
  sem registro simplesmente não geram ponto, não geram zero)
- **Modo de agregação**: `MEDIA` (média dos valores do dia) ou `MELHOR`
  (melhor valor do dia, respeitando orientação) — o atleta alterna entre
  os dois na mesma tela, mesmo endpoint, parâmetro diferente

## Alternativas consideradas
- Agrupar por semana/mês conforme o período (proposta inicial) →
  substituído pela decisão do usuário: sempre por dia, mais granular,
  mesmo que gere mais pontos em períodos longos.
- Pré-calcular evolução também via fila → rejeitado: parametrização
  interativa (modalidade + tipo + período + modo) não combina com
  pré-cálculo — o valor só se paga se for reaproveitado, e aqui quase
  nunca seria.

## Consequências
- UC42 (dashboard) ganha a lista de recordes plurais, sem endpoint novo.
- UC46 (novo) é o endpoint de evolução, sempre calculado na hora — não
  usa `dashboard_summary`.
- Frontend precisa de um seletor (modalidade, tipo de resultado dentro
  dela, período) e um toggle Média/Melhor.

## Referências
- ADR-0011 (orientação de melhor valor)
- ADR-0015 (dashboard pré-calculado — não se aplica à evolução)
- UC42 (dashboard), UC46 (evolução)