# UC35 - Listar visitas

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita a lista de suas visitas
2. Sistema retorna todas as visitas do atleta, cada uma com seus treinos
   aninhados (id, modalidade, status, horários)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Atleta sem nenhuma visita registrada → retorna lista vazia (não é erro)

## Observação
Assim como em UC08/UC27, o conteúdo real dos treinos aninhados depende do
domínio de Treino (UC32/UC33), que ainda não existe. Até lá, toda visita é
retornada com `trainings: []` — a forma da resposta já está pronta, só falta
ter o que popular.

## Definição de pronto
- [x] Teste cobrindo listagem com visitas e treinos aninhados (lista vazia —
      ver Observação abaixo)
- [x] Teste cobrindo lista vazia
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)
- [x] Teste garantindo que a listagem nunca inclui visita de outro atleta

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0012 (Visita e Treino)