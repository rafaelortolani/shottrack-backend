# UC46 - Consultar evolução

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Modalidade é praticada
pelo atleta (UC12). Tipo de resultado está configurado no Perfil de
Modalidade (UC30) daquela modalidade.

## Fluxo principal
1. Atleta informa modalidadeId, resultTypeId, período (`7d`/`30d`/`3m`/`1a`)
   e modo (`MEDIA`/`MELHOR`)
2. Sistema valida que a modalidade é praticada e o tipo está configurado
   nela
3. Sistema calcula um ponto por dia dentro do período, a partir das
   séries de treinos daquela modalidade: `MEDIA` = média dos valores do
   tipo registrados naquele dia; `MELHOR` = melhor valor do dia
   (respeitando a orientação do tipo, ADR-0011). O dia de um valor é o
   dia em que o treino começou (UTC, mesmo critério dos indicadores do
   mês no UC42) — completar o resultado depois não muda o dia do ponto.
   O período termina hoje, inclusive (`7d` = hoje e os 6 dias
   anteriores)
4. Sistema retorna a lista de pontos (data + valor) — dias sem nenhum
   registro daquele tipo simplesmente não geram ponto

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Período ou modo fora dos valores aceitos → erro de validação
- 2a. Modalidade não é praticada pelo atleta → erro `MODALITY_NOT_PRACTICED`
- 2b. Tipo de resultado não configurado pra essa modalidade → erro `RESULT_TYPE_NOT_CONFIGURED_FOR_TRAINING`
- 3a. Nenhum dado no período → retorna lista vazia (não é erro)

## Observação
Valor de resultado é texto (ADR-0013) — valor não numérico é ignorado no
cálculo, tanto pra média quanto pra melhor. Tipo sem orientação
(`NAO_APLICAVEL`, ADR-0011) não tem "melhor valor": no modo `MELHOR`, a
lista vem vazia.

## Definição de pronto
- [x] Teste cobrindo modo MEDIA com dados reais
- [x] Teste cobrindo modo MELHOR com dados reais
- [x] Teste cobrindo os 4 períodos
- [x] Teste cobrindo período sem nenhum dado (lista vazia)
- [x] Teste cobrindo dia sem registro não gerando ponto (não gerando zero)
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0011 (orientação de melhor valor)
- ADR-0016 (evolução sob demanda)
- UC12 (modalidades praticadas), UC30 (perfil de modalidade)