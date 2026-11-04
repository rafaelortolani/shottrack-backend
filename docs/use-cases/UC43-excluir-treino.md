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
   externo) e as fotos do alvo dessas séries (registros em cascata; arquivos
   apagados depois do commit — ADR-0026)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Treino não encontrado ou pertence a outro atleta → erro `TRAINING_NOT_FOUND`

## Observação
Se um arquivo de foto não puder ser apagado, a exclusão do treino não falha: a falha é registrada no log (sem dado
pessoal) e a rotina semanal de limpeza remove o arquivo órfão (ADR-0026).

Sem bloqueio por status — pode excluir mesmo um treino `EM_ANDAMENTO`
(ex: corrigir um engano, como abrir treino na modalidade errada).

## Definição de pronto
- [ ] Teste cobrindo exclusão de treino sem séries
- [ ] Teste cobrindo exclusão de treino com séries, confirmando que elas
  somem junto
- [ ] Teste cobrindo exclusão de treino `EM_ANDAMENTO`
- [ ] Um teste para CADA fluxo alternativo listado acima
- [x] Treino com fotos: registros e arquivos (imagem e miniatura) somem junto (`SeriesPhotoCascadeTest`)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0012 (Visita e Treino)
- ADR-0013 (Série)
- ADR-0026 (fotos do alvo)