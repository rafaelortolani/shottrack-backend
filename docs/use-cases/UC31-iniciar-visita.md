# UC31 - Iniciar visita

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Local de treino existe e
pertence ao atleta (UC24).

## Fluxo principal
1. Atleta informa o local (localId) e, opcionalmente, observações, o tipo da
   visita (`visitType`: `TREINO`, padrão, ou `COMPETICAO`) e, só para
   competição, o âmbito (`competitionScope`: `ESTADUAL`, `NACIONAL` ou
   `INTERNACIONAL`)
2. Sistema valida que o local existe e pertence ao atleta e que o âmbito só
   foi informado para `COMPETICAO`
3. Sistema cria a visita com status `EM_ANDAMENTO`, `iniciada_em` = agora

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. LocalId não informado → erro de validação
- 1c. `competitionScope` informado com `visitType = TREINO` → erro de validação
- 1d. `visitType` ou `competitionScope` com valor fora dos aceitos → erro de validação
- 2a. Local não encontrado ou pertence a outro atleta → erro `TRAINING_LOCATION_NOT_FOUND`

## Observação
Tipo e âmbito alimentam a apuração de habitualidade (ADR-0023). Visitas
anteriores a este campo são `TREINO`, sem âmbito. A competição pode ser criada
sem âmbito; nesse caso ela não conta em exigências que pedem âmbito mínimo.

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima
- [x] Teste criando visita sem tipo (assume `TREINO`)
- [x] Teste criando visita `COMPETICAO` com e sem âmbito
- [x] Teste do fluxo alternativo 1c (âmbito com `TREINO`) e 1d (valores inválidos)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0012 (Visita e Treino)
- ADR-0023 (habitualidade configurável)
