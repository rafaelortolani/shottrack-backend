# UC24 - Cadastrar local de treino

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta informa nome, cidade e estado
2. Sistema valida os campos e cria o local vinculado ao atleta autenticado

## Fluxos alternativos
- 1a. Algum campo obrigatório vazio → erro de validação
- 1b. Token ausente ou inválido → erro `UNAUTHORIZED`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0010 (local de treino livre, estrutura pronta pra clubes futuros)