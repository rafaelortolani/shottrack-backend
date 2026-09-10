# UC15 - Editar munição

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Munição pertence ao atleta
autenticado.

## Fluxo principal
1. Atleta envia apenas os campos que deseja alterar (edição parcial —
   diferente de arma, aqui não é necessário reenviar tudo)
2. Sistema valida os campos enviados (fabricanteId/calibreId, se enviados,
   precisam existir nos catálogos; identificação — fabricante ou apelido —
   continua obrigatória após a edição)
3. Sistema atualiza apenas os campos enviados e retorna os dados completos

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Munição não existe ou pertence a outro atleta → erro `AMMUNITION_NOT_FOUND`
- 2a. Edição resultaria em nenhum fabricante E nenhum apelido → erro
  `AMMUNITION_IDENTIFICATION_REQUIRED`
- 2b. FabricanteId enviado não existe → erro `AMMUNITION_MANUFACTURER_NOT_FOUND`
- 2c. CalibreId enviado não existe → erro `WEAPON_CALIBER_NOT_FOUND`

## Definição de pronto
- [x] Teste cobrindo edição de um único campo, sem afetar os demais
- [x] Teste cobrindo edição de vários campos de uma vez
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0007 (munição — edição parcial, diferente do padrão de arma)