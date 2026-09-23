# UC42 - Consultar dashboard

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita o dashboard
2. Sistema busca o resumo pré-calculado do atleta em `dashboard_summary`
3. Sistema retorna: treinos esse mês, disparos esse mês, modalidades
   praticadas, 3 visitas mais recentes, e o destaque dinâmico (se houver)

## Fluxo alternativo — sem resumo ainda (fallback)
- 2a. Não existe linha em `dashboard_summary` pra esse atleta ainda
  (primeiro acesso, antes de qualquer evento ter sido processado pela
  fila) → sistema calcula tudo na hora, com a mesma lógica do
  recalculador (ADR-0015), sem esperar o evento

## Destaque dinâmico (mesma regra de antes)
Entre os tipos de resultado com orientação `MENOR_MELHOR` ou
`MAIOR_MELHOR` (ADR-0011), o que tem mais registros preenchidos nas
séries do atleta; valor é o melhor já registrado nesse tipo, respeitando
a orientação; desempate pelo mais usado recentemente; ausente se nenhum
tipo elegível tiver registro ainda.

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Atleta sem nenhum dado ainda → retorna zeros/vazio, sem erro

## Observação
Valor de resultado é texto (ADR-0013) — no cálculo, valor não numérico
pro tipo em questão é ignorado silenciosamente.

## Definição de pronto
- [x] Teste cobrindo leitura normal (resumo já existe em dashboard_summary)
- [x] Teste cobrindo o fallback (resumo não existe ainda, calcula na hora)
- [x] Teste cobrindo atleta sem nenhum dado (zeros/vazio, sem erro)
- [x] Teste cobrindo o destaque dinâmico (tipo certo, valor certo,
  desempate, ausência quando não há registro elegível)
- [x] Teste cobrindo valor não numérico sendo ignorado
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0011 (orientação de melhor valor por tipo)
- ADR-0013 (Série — resultado armazenado como texto)
- ADR-0015 (dashboard pré-calculado via fila)