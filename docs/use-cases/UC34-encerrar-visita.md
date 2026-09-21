# UC34 - Encerrar visita

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Visita pertence ao atleta
e está `EM_ANDAMENTO`.

## Fluxo principal
1. Atleta solicita o encerramento de uma visita sua
2. Sistema valida que a visita está `EM_ANDAMENTO`
3. Sistema encerra a visita (`status = ENCERRADA`, `encerrada_em` = agora)
   e, na mesma operação, encerra também qualquer treino dela que ainda
   estivesse `EM_ANDAMENTO` (mesmo timestamp) — sem exigir confirmação
   adicional (ADR-0012)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Visita não encontrada ou pertence a outro atleta → erro `VISIT_NOT_FOUND`
- 2a. Visita já encerrada → erro `VISIT_ALREADY_CLOSED`

## Definição de pronto
- [x] Teste cobrindo encerramento sem nenhum treino aberto
- [x] Teste cobrindo encerramento COM treino(s) aberto(s), confirmando que
  eles são encerrados em cascata
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0012 (Visita e Treino — encerramento em cascata)
- UC31 (iniciar visita)