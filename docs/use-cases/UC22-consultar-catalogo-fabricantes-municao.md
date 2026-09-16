# UC22 - Consultar catálogo de fabricantes de munição

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Contexto
Lacuna identificada durante a implementação do frontend: o UC13 (cadastrar
munição) e o ADR-0007 definiram fabricante como catálogo fechado, mas
nenhum use case cobriu a **consulta** desse catálogo — diferente do que foi
feito para Armas, onde o UC09 existe especificamente pra isso. Sem esse
endpoint, o frontend não consegue montar o seletor de fabricante no
cadastro de munição (UC13), já que `manufacturerId` é um UUID de referência,
não texto livre.

## Fluxo principal
1. Atleta solicita a lista de fabricantes do catálogo
2. Sistema retorna todos os fabricantes cadastrados, pra popular o
   cadastro de munição (UC13)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0007 (munição — catálogo híbrido, fabricante fechado)
- UC13 (cadastrar munição, depende deste catálogo)
- UC09 (mesmo padrão já aplicado ao catálogo de armas)