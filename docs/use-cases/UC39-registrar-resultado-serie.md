# UC39 - Registrar resultado de uma série

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Série pertence ao atleta.
Tipo de resultado está configurado no Perfil de Modalidade (UC30) da
modalidade do treino daquela série.

## Fluxo principal — registrar valor
1. Atleta informa o tipo de resultado (resultTypeId) e um valor
2. Sistema valida que o tipo está configurado pra modalidade do treino, e
   que o formato do valor bate com o tipo (numérico, sim/não, ou texto
   livre, conforme o tipo)
3. Sistema registra o valor pra aquele tipo naquela série (substitui se já
   havia um registro anterior — inclusive se estava marcado "não
   aplicável")
4. Se o tipo for "Acertos" ou "Erros": sistema mantém quantidadeDisparos
   da série em consistência com a soma dos dois (ADR-0014) — se
   quantidadeDisparos ainda não foi informada manualmente pelo atleta
   (UC36/UC38), é recalculada automaticamente pra soma; se já foi, a soma
   não pode ultrapassá-la

## Fluxo principal — marcar como não aplicável
1. Atleta marca um tipo de resultado como não aplicável pra essa série
   (sem valor)
2. Sistema valida que o tipo está configurado pra modalidade do treino, e
   registra o estado "não aplicável" (substitui valor anterior, se havia)
3. Se o tipo for "Acertos" ou "Erros" e quantidadeDisparos ainda não foi
   informada manualmente: sistema recalcula quantidadeDisparos pra soma
   dos dois, agora contando 0 pra esse tipo (ADR-0014)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Série não encontrada ou pertence a outro atleta → erro `SERIES_NOT_FOUND`
- 2a. Tipo de resultado não configurado pra modalidade do treino → erro `RESULT_TYPE_NOT_CONFIGURED_FOR_TRAINING`
- 2b. Valor em formato incompatível com o tipo (ex: texto num tipo
  numérico, ou decimal/negativo em "Acertos"/"Erros") → erro de validação
  `RESULT_VALUE_FORMAT_INVALID`
- 4a. Tipo "Acertos" ou "Erros": nova soma ultrapassa quantidadeDisparos já
  informada manualmente pra série → erro `RESULT_EXCEEDS_SHOT_COUNT`

## Definição de pronto
- [x] Teste cobrindo registrar um valor numérico
- [x] Teste cobrindo registrar valor "0" (distinto de não preenchido)
- [x] Teste cobrindo marcar como não aplicável
- [x] Teste cobrindo substituir um valor já registrado
- [x] Um teste para CADA fluxo alternativo listado acima
- [x] Teste cobrindo auto-preenchimento de quantidadeDisparos a partir de
  acertos+erros quando ainda não informada manualmente
- [x] Teste cobrindo rejeição quando acertos+erros ultrapassa
  quantidadeDisparos já informada manualmente

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0013 (Série — resultados com 3 estados)
- ADR-0014 (consistência entre disparos e acertos/erros)
- UC30 (Perfil de Modalidade — pré-condição)