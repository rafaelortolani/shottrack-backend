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

## Fluxo principal — marcar como não aplicável
1. Atleta marca um tipo de resultado como não aplicável pra essa série
   (sem valor)
2. Sistema valida que o tipo está configurado pra modalidade do treino, e
   registra o estado "não aplicável" (substitui valor anterior, se havia)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Série não encontrada ou pertence a outro atleta → erro `SERIES_NOT_FOUND`
- 2a. Tipo de resultado não configurado pra modalidade do treino → erro `RESULT_TYPE_NOT_CONFIGURED_FOR_TRAINING`
- 2b. Valor em formato incompatível com o tipo (ex: texto num tipo
  numérico) → erro de validação

## Definição de pronto
- [ ] Teste cobrindo registrar um valor numérico
- [ ] Teste cobrindo registrar valor "0" (distinto de não preenchido)
- [ ] Teste cobrindo marcar como não aplicável
- [ ] Teste cobrindo substituir um valor já registrado
- [ ] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0013 (Série — resultados com 3 estados)
- UC30 (Perfil de Modalidade — pré-condição)