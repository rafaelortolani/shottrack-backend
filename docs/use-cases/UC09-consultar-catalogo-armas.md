# UC09 - Consultar catálogo de armas

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita a lista de tipos do catálogo
2. Atleta solicita a lista de marcas do catálogo
3. Atleta solicita a lista de modelos de uma marca específica
4. Atleta solicita a lista de calibres do catálogo
5. Sistema retorna os itens correspondentes, pra popular o cadastro de arma
   (UC06)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 3a. Marca informada não existe → erro `WEAPON_BRAND_NOT_FOUND`

## Definição de pronto
- [x] Teste cobrindo a listagem de tipos
- [x] Teste cobrindo a listagem de marcas
- [x] Teste cobrindo a listagem de modelos de uma marca válida
- [x] Teste cobrindo o fluxo alternativo 3a (marca inexistente)
- [x] Teste cobrindo a listagem de calibres
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0004 (catálogo de tipo/marca/modelo/calibre via seed fixo)