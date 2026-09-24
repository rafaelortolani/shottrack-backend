# UC09 - Consultar catálogo de armas

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita a lista de marcas do catálogo
2. Atleta solicita a lista de modelos de uma marca específica (cada
   modelo já retorna seu tipo)
3. Atleta solicita a lista de calibres válidos pra um modelo específico
   (ADR-0004, Revisão 2 — não mais o catálogo geral de calibres solto)
4. Sistema retorna os itens correspondentes, pra popular o cadastro de
   arma (UC06)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 2a. Marca informada não existe → erro `WEAPON_BRAND_NOT_FOUND`
- 3a. Modelo informado não existe → erro `WEAPON_MODEL_NOT_FOUND`

## Observação
O catálogo geral de tipos (`GET /api/weapon-catalog/types`) e o catálogo
geral de calibres (`GET /api/weapon-catalog/calibers`) continuam
existindo como estão — usados pra exibição/referência e reaproveitados
por outros domínios (ex: Munição, ADR-0007). O que muda é que o
**cadastro de arma** não usa mais o catálogo geral de calibres solto,
usa a lista filtrada por modelo (fluxo principal 3).

## Definição de pronto
- [x] Teste cobrindo a listagem de marcas
- [x] Teste cobrindo a listagem de modelos de uma marca válida (com tipo
  incluído em cada modelo)
- [x] Teste cobrindo a listagem de calibres válidos de um modelo válido
- [x] Teste cobrindo os fluxos alternativos 2a e 3a
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0004 (catálogo de armas, Revisão 2)