# UC30 - Consultar e ajustar perfil de modalidade

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Modalidade é uma das
modalidades praticadas pelo atleta (UC12).

## Fluxo principal — consultar
1. Atleta solicita os tipos de resultado configurados pra uma modalidade
   praticada
2. Sistema retorna a lista (inicialmente a sugestão padrão aplicada quando
   a modalidade foi adicionada, ver ADR-0011 — mas o atleta pode já ter
   alterado)

## Fluxo principal — adicionar tipo de resultado
1. Atleta informa um tipo de resultado do catálogo (UC29) pra adicionar à
   modalidade
2. Sistema valida que o tipo existe e ainda não está configurado pra essa
   modalidade, e adiciona

## Fluxo principal — remover tipo de resultado
1. Atleta informa um tipo de resultado já configurado pra remover
2. Sistema remove, sem afetar os demais tipos configurados

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Modalidade não é praticada pelo atleta → erro `MODALITY_NOT_PRACTICED`
- 2a. Tipo de resultado não encontrado no catálogo → erro `RESULT_TYPE_NOT_FOUND`
- 2b. (adicionar) Tipo já configurado pra essa modalidade → erro `RESULT_TYPE_ALREADY_CONFIGURED`
- 2c. (remover) Tipo não está configurado pra essa modalidade → erro `RESULT_TYPE_NOT_CONFIGURED`

## Definição de pronto
- [x] Teste cobrindo consulta (com a sugestão padrão já aplicada)
- [x] Teste cobrindo adicionar um tipo
- [x] Teste cobrindo remover um tipo sem afetar os demais
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0011 (tipo de resultado, sugestão padrão)
- UC12 (modalidades praticadas — pré-condição e gatilho da sugestão inicial)
- UC29 (catálogo de tipos de resultado)