# ADR-0022: Dashboard em abas com filtro global de modalidade

## Contexto
O dashboard concentra ação principal, indicadores, últimos treinos, modalidades,
acervo e recordes numa única tela. Em uso real (inclusive no celular) há dados
demais. Queremos separar por intenção, comparar com o período anterior e permitir
olhar uma modalidade por vez.

## Decisão
1. Abas: **Resumo | Evolução | Recordes | Modalidades** (Habitualidade entra como 5ª
   aba em etapa futura, ADR próprio).
2. **Filtro global de modalidade** (Todas / uma modalidade praticada) vale para
   Resumo, Evolução e Recordes. A aba Modalidades ignora o filtro. O filtro vive na
   URL (`?modalidade=<id>`) e persiste ao trocar de aba.
3. **Resumo do acervo sai do dashboard** (o Acervo já tem a tela própria). O backend
   deixa de devolvê-lo no UC42.
4. **Indicadores com comparação**: treinos no mês, disparos no mês e modalidades
   praticadas, cada um com o valor do período anterior. Período = mês corrente até
   hoje × **mesmo número de dias** do mês anterior (evita comparar mês parcial com
   mês cheio).
5. **Evolução** aceita `comparePrevious`: devolve também o período imediatamente
   anterior, de mesma duração, para sobreposição no gráfico.
6. **Pré-cálculo (ADR-0015)**: continua para recordes e estatísticas por modalidade
   (já existem por modalidade, o filtro apenas seleciona). Os indicadores do mês
   dependem da data de hoje (janela móvel), então são calculados sob demanda (são
   contagens indexadas, baratas). O fallback do UC42 permanece.
7. Aba Modalidades vira endpoint próprio (`GET /api/dashboard/modalities`), reaproveitando
   `dashboard_summary_modality_stats`.

## Alternativas consideradas
- Um dashboard por modalidade em páginas separadas → rejeitado: navegação pesada.
- Pré-calcular indicadores por mês fechado → rejeitado: a comparação por janela móvel
  não cabe em mês fechado.

## Consequências
- UC42 v7 muda o contrato (indicadores com `previous`, sem `weaponsSummary`/acervo,
  aceita `modalityId`). O frontend precisa ser atualizado junto.
- UC46 ganha parâmetro opcional `comparePrevious` (compatível com o uso atual).
- Novo UC (Modalidades do dashboard) ou seção do UC42 — ver UC42 v7.

## Referências
ADR-0011, ADR-0013, ADR-0015, ADR-0016.
