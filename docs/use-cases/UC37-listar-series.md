# UC37 - Listar séries de um treino

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Treino pertence ao atleta.

## Fluxo principal
1. Atleta solicita a lista de séries de um treino
2. Sistema retorna todas as séries daquele treino, cada uma com seus
   resultados (os três estados — não preenchido é ausência de registro,
   então só aparecem os preenchidos/não aplicáveis)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Treino não encontrado ou pertence a outro atleta → erro `TRAINING_NOT_FOUND`
- 1c. Treino sem nenhuma série registrada → retorna lista vazia (não é erro)

## Definição de pronto
- [x] Teste cobrindo listagem com séries e resultados
- [x] Teste cobrindo lista vazia
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0013 (Série)