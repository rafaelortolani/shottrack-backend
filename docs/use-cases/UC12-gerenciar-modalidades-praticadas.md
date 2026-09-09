# UC12 - Gerenciar modalidades praticadas

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Modalidade existe no
catálogo (UC11/ADR-0005).

## Fluxo principal — adicionar
1. Atleta informa o id de uma modalidade
2. Sistema valida que a modalidade existe e que o atleta ainda não a possui
   na lista de praticadas
3. Sistema associa a modalidade ao atleta

## Fluxo principal — remover
1. Atleta informa o id de uma modalidade já associada
2. Sistema remove a associação, sem afetar as demais modalidades praticadas

## Fluxo principal — listar
1. Atleta solicita suas modalidades praticadas
2. Sistema retorna a lista (pode ser vazia — atleta pode não ter selecionado
   nenhuma ainda, isso não é erro)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 2a. Modalidade não encontrada no catálogo → erro `MODALITY_NOT_FOUND`
- 2b. Modalidade já está na lista de praticadas do atleta → erro `MODALITY_ALREADY_ADDED`
- 2c. (remoção) Modalidade não está associada ao atleta → erro `MODALITY_NOT_ASSOCIATED`

## Definição de pronto
- [x] Teste cobrindo adicionar uma modalidade
- [x] Teste cobrindo remover uma modalidade (sem afetar as outras)
- [x] Teste cobrindo listar (com itens e lista vazia)
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0005 (modalidade como catálogo fixo)
- UC11 (consultar catálogo de modalidades)