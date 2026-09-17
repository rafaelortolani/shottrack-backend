# UC26 - Editar local de treino

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Local pertence ao atleta
autenticado.

## Fluxo principal
1. Atleta envia nome, cidade e/ou estado a alterar
2. Sistema valida (campos não podem ficar vazios) e atualiza os campos
   enviados (edição parcial — mesmo padrão de Munição, ADR-0007)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Local não existe ou pertence a outro atleta → erro `TRAINING_LOCATION_NOT_FOUND`
- 2a. Algum campo enviado ficaria vazio → erro de validação

## Definição de pronto
- [x] Teste cobrindo edição de um único campo, sem afetar os demais
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0010 (local de treino livre)