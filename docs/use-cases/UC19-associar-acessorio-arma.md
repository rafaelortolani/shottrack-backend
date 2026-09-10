# UC19 - Associar/desassociar acessório a armas

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Acessório e arma pertencem
ao atleta autenticado.

## Fluxo principal — associar
1. Atleta informa acessórioId e armaId
2. Sistema valida que ambos existem e pertencem ao atleta, e que a
   associação ainda não existe
3. Sistema cria a associação

## Fluxo principal — desassociar
1. Atleta informa acessórioId e armaId de uma associação existente
2. Sistema remove a associação, sem afetar outras associações do mesmo
   acessório ou da mesma arma

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 2a. Acessório não encontrado ou de outro atleta → erro `ACCESSORY_NOT_FOUND`
- 2b. Arma não encontrada ou de outro atleta → erro `WEAPON_NOT_FOUND`
- 2c. (associar) Associação já existe → erro `ACCESSORY_ALREADY_ASSOCIATED`
- 2d. (desassociar) Associação não existe → erro `ACCESSORY_NOT_ASSOCIATED`

## Definição de pronto
- [x] Teste cobrindo associar um acessório a uma arma
- [x] Teste cobrindo associar o mesmo acessório a uma segunda arma (N:N)
- [x] Teste cobrindo desassociar sem afetar outras associações
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0008 (acessório — associação N:N)