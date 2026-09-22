# UC38 - Editar série

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Série pertence ao atleta
(via treino).

## Fluxo principal
1. Atleta envia os campos que deseja alterar/completar (edição parcial —
   é assim que "completar depois" funciona na prática)
2. Sistema valida os campos enviados e atualiza só esses

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Série não encontrada ou pertence a outro atleta → erro `SERIES_NOT_FOUND`
- 2a. ArmaId enviado não existe ou pertence a outro atleta → erro `WEAPON_NOT_FOUND`
- 2b. MunicaoId enviado não existe ou pertence a outro atleta → erro `AMMUNITION_NOT_FOUND`
- 2c. quantidadeDisparos enviada é menor que a soma de acertos+erros já
  registrados pra essa série → erro `SHOT_COUNT_LESS_THAN_REGISTERED_RESULTS`
  (ADR-0014) — atleta precisa ajustar acertos/erros primeiro (UC39)

## Definição de pronto
- [x] Teste cobrindo edição de um único campo, completando um dado que
  faltava (ex: adicionar a arma depois)
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0013 (Série — edição parcial é como "completar depois" funciona)
- ADR-0014 (consistência entre disparos e acertos/erros)