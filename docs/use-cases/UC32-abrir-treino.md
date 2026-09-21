# UC32 - Abrir treino

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Visita pertence ao atleta
e está `EM_ANDAMENTO`. Modalidade é praticada pelo atleta (UC12).

## Fluxo principal
1. Atleta informa a visita (visitaId) e a modalidade (modalidadeId)
2. Sistema valida que a visita pertence ao atleta e está `EM_ANDAMENTO`,
   e que a modalidade é praticada pelo atleta
3. Sistema cria o treino com status `EM_ANDAMENTO`, `iniciado_em` = agora,
   vinculado à visita

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 2a. Visita não encontrada ou pertence a outro atleta → erro `VISIT_NOT_FOUND`
- 2b. Visita já encerrada → erro `VISIT_ALREADY_CLOSED`
- 2c. Modalidade não é praticada pelo atleta → erro `MODALITY_NOT_PRACTICED`

## Observação
Não há restrição de unicidade — o atleta pode abrir mais de um treino da
mesma modalidade na mesma visita (ADR-0012).

## Definição de pronto
- [ ] Teste cobrindo o fluxo principal
- [ ] Teste cobrindo abrir dois treinos da mesma modalidade na mesma visita
- [ ] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0012 (Visita e Treino)
- UC31 (iniciar visita — pré-condição)