# UC40 - Remover resultado de uma série

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Série pertence ao atleta.
Existe um registro (valor ou "não aplicável") pra esse tipo de resultado
nessa série.

## Fluxo principal
1. Atleta solicita a remoção do registro de um tipo de resultado numa
   série
2. Sistema remove o registro — o tipo volta ao estado "não preenchido"
   (não vira "zero", não vira "não aplicável": deixa de existir)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Série não encontrada ou pertence a outro atleta → erro `SERIES_NOT_FOUND`
- 1c. Não existe registro desse tipo pra essa série → erro `RESULT_NOT_CONFIGURED`

## Definição de pronto
- [x] Teste cobrindo remoção de um valor registrado
- [x] Teste cobrindo remoção de um "não aplicável"
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0013 (Série — resultados com 3 estados)
- UC39 (registrar resultado)