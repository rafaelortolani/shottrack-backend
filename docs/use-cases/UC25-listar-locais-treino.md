# UC25 - Listar locais de treino

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita a lista de locais cadastrados
2. Sistema retorna todos os locais vinculados ao atleta autenticado, cada um
   com a marca `club`

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Atleta sem nenhum local cadastrado → retorna lista vazia (não é erro)

## Definição de pronto
- [x] Teste cobrindo o fluxo principal (com locais cadastrados)
- [x] Teste cobrindo o fluxo alternativo 1b (lista vazia)
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)
- [x] Teste garantindo que a lista nunca inclui local de outro atleta
- [x] Teste confirmando que cada local traz `club` (verdadeiro e falso)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0023 (habitualidade configurável)
