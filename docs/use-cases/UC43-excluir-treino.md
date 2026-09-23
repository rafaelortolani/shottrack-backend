# UC43 - Excluir treino

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Treino pertence ao atleta
(via visita).

## Fluxo principal
1. Atleta solicita a exclusão de um treino seu
2. Sistema exclui o treino definitivamente, junto com todas as séries
   dele (cascata — série só existe no contexto do treino, não é "uso"
   externo)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Treino não encontrado ou pertence a outro atleta → erro `TRAINING_NOT_FOUND`

## Observação
Sem bloqueio por status — pode excluir mesmo um treino `EM_ANDAMENTO`
(ex: corrigir um engano, como abrir treino na modalidade errada).

## Definição de pronto
- [ ] Teste cobrindo exclusão de treino sem séries
- [ ] Teste cobrindo exclusão de treino com séries, confirmando que elas
  somem junto
- [ ] Teste cobrindo exclusão de treino `EM_ANDAMENTO`
- [ ] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0012 (Visita e Treino)
- ADR-0013 (Série)