# UC10 - Editar arma

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Arma pertence ao atleta
autenticado.

## Fluxo principal
1. Atleta envia tipoId, marcaId, modeloId, calibreId (sempre completos — não
   dá pra editar só um campo do catálogo sem reenviar os outros) e,
   opcionalmente, um apelido
2. Sistema valida que tipo, marca, modelo e calibre existem e que o modelo
   pertence à marca informada
3. Sistema atualiza a arma e retorna seus dados

## Fluxos alternativos
- 1a. Algum id obrigatório (tipoId/marcaId/modeloId/calibreId) não informado
  → erro de validação
- 1b. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1c. Arma não existe ou pertence a outro atleta → erro `WEAPON_NOT_FOUND`
- 2a. Tipo não encontrado → erro `WEAPON_TYPE_NOT_FOUND`
- 2b. Marca não encontrada → erro `WEAPON_BRAND_NOT_FOUND`
- 2c. Modelo não encontrado → erro `WEAPON_MODEL_NOT_FOUND`
- 2d. Calibre não encontrado → erro `WEAPON_CALIBER_NOT_FOUND`
- 2e. Modelo não pertence à marca informada → erro `WEAPON_MODEL_BRAND_MISMATCH`

## Observação
O apelido pode ser alterado a qualquer momento, mesmo que a arma já tenha
sido usada em algum treino/resultado. Já a troca de tipo/marca/modelo/
calibre de uma arma já usada deveria ser bloqueada pela mesma razão do UC08
(preservar a integridade do histórico) — mas, como os domínios de Treino e
Resultado ainda não existem, essa checagem ainda não tem o que checar; fica
pendente, igual ao UC08.

## Definição de pronto
- [x] Teste cobrindo o fluxo principal (incluindo alterar só o apelido,
      reenviando o resto sem mudança)
- [x] Teste cobrindo o fluxo alternativo 1a (campo obrigatório ausente)
- [x] Teste cobrindo o fluxo alternativo 1b (sem token)
- [x] Teste cobrindo o fluxo alternativo 1c (arma inexistente ou de outro
      atleta)
- [x] Teste cobrindo o fluxo alternativo 2a (tipo não encontrado)
- [x] Teste cobrindo o fluxo alternativo 2b (marca não encontrada)
- [x] Teste cobrindo o fluxo alternativo 2c (modelo não encontrado)
- [x] Teste cobrindo o fluxo alternativo 2d (calibre não encontrado)
- [x] Teste cobrindo o fluxo alternativo 2e (modelo não pertence à marca)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0003 (não armazenar número de registro da arma)
- ADR-0004 (catálogo de tipo/marca/modelo/calibre via seed fixo)
- UC06 (cadastrar arma)
- UC08 (excluir arma — mesma pendência de checagem de uso em treino/resultado)
