# UC06 - Cadastrar arma

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Tipo, marca, modelo e calibre
escolhidos existem no catálogo (UC09) e o modelo pertence à marca informada
(ADR-0004).

## Fluxo principal
1. Atleta informa tipoId, marcaId, modeloId, calibreId e, opcionalmente, um
   apelido (texto livre — útil pra diferenciar duas armas com o mesmo tipo/
   marca/modelo/calibre)
2. Sistema valida que tipo, marca, modelo e calibre existem e que o modelo
   pertence à marca informada
3. Sistema cria a arma vinculada ao atleta autenticado e retorna seus dados

## Fluxos alternativos
- 1a. Algum id não informado → erro de validação
- 1b. Token ausente ou inválido → erro `UNAUTHORIZED`
- 2a. Tipo não encontrado → erro `WEAPON_TYPE_NOT_FOUND`
- 2b. Marca não encontrada → erro `WEAPON_BRAND_NOT_FOUND`
- 2c. Modelo não encontrado → erro `WEAPON_MODEL_NOT_FOUND`
- 2d. Calibre não encontrado → erro `WEAPON_CALIBER_NOT_FOUND`
- 2e. Modelo não pertence à marca informada → erro `WEAPON_MODEL_BRAND_MISMATCH`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0003 (não armazenar número de registro da arma)
- ADR-0004 (catálogo de tipo/marca/modelo/calibre via seed fixo)
- UC10 (editar arma — inclui alterar o apelido depois do cadastro)