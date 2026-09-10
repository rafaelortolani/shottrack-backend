# UC20 - Editar acessório

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Acessório pertence ao
atleta autenticado.

## Fluxo principal
1. Atleta envia nome, tipo e/ou observações a alterar
2. Sistema valida (nome não pode ficar vazio) e atualiza os campos enviados

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Acessório não existe ou pertence a outro atleta → erro `ACCESSORY_NOT_FOUND`
- 2a. Nome resultaria vazio → erro de validação

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)