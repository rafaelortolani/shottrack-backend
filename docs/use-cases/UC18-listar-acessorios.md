# UC18 - Listar acessórios do atleta

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita a lista de acessórios cadastrados
2. Sistema retorna todos os acessórios vinculados ao atleta autenticado,
   incluindo, para cada um, as armas às quais está associado (se houver)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Atleta sem nenhum acessório cadastrado → retorna lista vazia (não é erro)

## Definição de pronto
- [x] Teste cobrindo o fluxo principal (com acessórios; associações sempre
      vazias por enquanto — UC19 ainda não existe)
- [x] Teste cobrindo o fluxo alternativo 1b (lista vazia)
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)
- [x] Teste garantindo que a lista nunca inclui acessório de outro atleta

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0008 (acessório — associação N:N)