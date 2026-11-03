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

## Limites de entrada (Lote 3, S10/S18)
- Apelido: até 100; tipo de projétil: até 50; lote: até 50; observações: até 2000 caracteres.
- Peso do projétil e carga de pólvora: ≥ 0, até 4 dígitos inteiros e 2 decimais (coluna `NUMERIC(6,2)`). Notação científica com expoente gigante é recusada. Acima/fora → `VALIDATION_ERROR` (400).

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0007 (munição — catálogo híbrido com cadastro parcial)