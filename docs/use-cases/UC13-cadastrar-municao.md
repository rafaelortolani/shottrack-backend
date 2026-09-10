# UC13 - Cadastrar munição

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Se informados, fabricanteId
e calibreId existem nos respectivos catálogos.

## Fluxo principal
1. Atleta informa os dados que souber: fabricanteId, calibreId, apelido,
   pesoProjetilGraos, quantidadePolvora, tipoProjetil, lote, observações —
   todos opcionais individualmente
2. Sistema valida que pelo menos fabricanteId ou apelido foi informado, e
   que fabricanteId/calibreId (quando enviados) existem nos catálogos
3. Sistema cria a munição vinculada ao atleta autenticado e retorna seus dados

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 2a. Nenhum entre fabricanteId e apelido foi informado → erro
  `AMMUNITION_IDENTIFICATION_REQUIRED`
- 2b. FabricanteId informado não existe no catálogo → erro `AMMUNITION_MANUFACTURER_NOT_FOUND`
- 2c. CalibreId informado não existe no catálogo → erro `WEAPON_CALIBER_NOT_FOUND`
  (reaproveita o erro do catálogo de armas, já que é o mesmo catálogo)

## Definição de pronto
- [x] Teste cobrindo cadastro só com fabricante
- [x] Teste cobrindo cadastro só com apelido
- [x] Teste cobrindo cadastro completo (todos os campos)
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0007 (munição — catálogo híbrido com cadastro parcial)