    # UC29 - Consultar catálogo de tipos de resultado

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita a lista de tipos de resultado do catálogo
2. Sistema retorna todos os tipos cadastrados (pontuação, tempo,
   agrupamento, acertos, erros, penalidades, fator de desempenho,
   exercício concluído, anotação livre), pra popular o ajuste de perfil
   de modalidade (UC30)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0011 (tipo de resultado como catálogo fixo)