# UC14 - Listar munições do atleta

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita a lista de munições cadastradas
2. Sistema retorna todas as munições vinculadas ao atleta autenticado
   (nunca as de outro atleta)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Atleta sem nenhuma munição cadastrada → retorna lista vazia (não é erro)

## Definição de pronto
- [x] Teste cobrindo o fluxo principal (com munições cadastradas)
- [x] Teste cobrindo o fluxo alternativo 1b (lista vazia)
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)
- [x] Teste garantindo que a lista nunca inclui munição de outro atleta

## Referências
- ADR-0001 (autenticação JWT)