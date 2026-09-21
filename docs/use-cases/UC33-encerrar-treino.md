# UC33 - Encerrar treino

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Treino pertence ao atleta
(via visita) e está `EM_ANDAMENTO`.

## Fluxo principal
1. Atleta solicita o encerramento de um treino seu
2. Sistema valida que o treino está `EM_ANDAMENTO`
3. Sistema atualiza o status pra `ENCERRADO`, `encerrado_em` = agora

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Treino não encontrado ou pertence a outro atleta → erro `TRAINING_NOT_FOUND`
- 2a. Treino já encerrado → erro `TRAINING_ALREADY_CLOSED`

## Definição de pronto
- [ ] Teste cobrindo o fluxo principal
- [ ] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0012 (Visita e Treino)
- UC32 (abrir treino — pré-condição)