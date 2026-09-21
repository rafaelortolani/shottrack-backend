# UC36 - Registrar série

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Treino pertence ao atleta
e está `EM_ANDAMENTO`. Se informados, armaId/municaoId pertencem ao
atleta.

## Fluxo principal
1. Atleta informa o treino (treinoId) e, opcionalmente, armaId,
   municaoId, distanciaMetros, alvo, quantidadeDisparos, observações
2. Sistema valida os campos informados (arma/munição, se enviadas,
   pertencem ao atleta)
3. Sistema cria a série vinculada ao treino e retorna seus dados

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 2a. Treino não encontrado ou pertence a outro atleta → erro `TRAINING_NOT_FOUND`
- 2b. Treino já encerrado → erro `TRAINING_ALREADY_CLOSED`
- 2c. ArmaId informado não existe ou pertence a outro atleta → erro `WEAPON_NOT_FOUND`
- 2d. MunicaoId informado não existe ou pertence a outro atleta → erro `AMMUNITION_NOT_FOUND`

## Definição de pronto
- [x] Teste cobrindo registro sem nenhum campo opcional preenchido
- [x] Teste cobrindo registro completo (todos os campos)
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0013 (Série — registro incompleto permitido)
- UC32 (abrir treino — pré-condição)