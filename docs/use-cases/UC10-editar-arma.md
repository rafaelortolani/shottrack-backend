# UC10 - Editar arma

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Arma pertence ao atleta
autenticado. Modelo existe no catálogo e o calibre é válido pra esse
modelo (ADR-0004, Revisão 2).

## Fluxo principal
1. Atleta envia modelId, caliberId (sempre completos — reenvia os dois,
   mesmo se só um mudou) e, opcionalmente, um apelido
2. Sistema valida que o modelo existe e que o calibre está entre os
   permitidos pra esse modelo
3. Sistema atualiza a arma (tipo e marca voltam a ser derivados do
   modelo escolhido) e retorna seus dados

## Fluxos alternativos
- 1a. ModelId ou caliberId não informado → erro de validação
- 1b. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1c. Arma não existe ou pertence a outro atleta → erro `WEAPON_NOT_FOUND`
- 2a. Modelo não encontrado → erro `WEAPON_MODEL_NOT_FOUND`
- 2b. Calibre não está entre os permitidos pra esse modelo → erro
  `WEAPON_CALIBER_NOT_ALLOWED_FOR_MODEL`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal (incluindo alterar só o apelido,
  reenviando modelo/calibre sem mudança)
- [x] Teste cobrindo troca de modelo pra um de calibre diferente (a
  arma passa a refletir o tipo/marca do novo modelo)
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0003 (não armazenar número de registro da arma)
- ADR-0004 (catálogo de armas, Revisão 2)
- UC06 (cadastrar arma)
- UC08 (excluir arma — mesma pendência de checagem de uso em treino/resultado)agu