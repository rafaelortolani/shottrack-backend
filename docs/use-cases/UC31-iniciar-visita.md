# UC31 - Iniciar visita

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Local de treino existe e
pertence ao atleta (UC24).

## Fluxo principal
1. Atleta informa o local (localId) e, opcionalmente, observações
2. Sistema valida que o local existe e pertence ao atleta
3. Sistema cria a visita com status `EM_ANDAMENTO`, `iniciada_em` = agora

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. LocalId não informado → erro de validação
- 2a. Local não encontrado ou pertence a outro atleta → erro `TRAINING_LOCATION_NOT_FOUND`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0012 (Visita e Treino)