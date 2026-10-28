# ADR-0024: Visita retroativa e edição de datas da visita

## Contexto
Hoje a visita sempre nasce com `iniciada_em` = agora e encerra com `encerrada_em` =
agora (ADR-0012). O atleta não consegue registrar um treino que já aconteceu (por
exemplo, de ontem ou do mês passado), o que prejudica dashboard, evolução e
habitualidade (ADR-0023).

## Decisão
1. **Iniciar visita com data passada**: ao iniciar, o atleta pode informar a data e o
   horário em que a visita começou (`startedAt`). Sem informar, vale agora, como hoje.
   Aceita passado até **12 meses** atrás; **futuro nunca**.
2. **Encerrar continua igual**: `encerrada_em` = agora (data atual), sem pedir nada.
3. **Editar datas depois**: nova operação permite ajustar `startedAt` e, em visita
   encerrada, `endedAt`. Regras: início ≤ fim; fim não pode ser futuro; início dentro de
   12 meses atrás; visita em andamento só altera o início.
4. **Data de referência**: toda apuração por data (dashboard, evolução, recordes,
   habitualidade, meta pessoal, histórico) usa a **data de início da visita**
   (`startedAt`), não o horário em que o registro foi digitado. Os horários próprios
   dos treinos e séries continuam sendo o registro operacional e não mudam.
5. **Consequências de cálculo**: criar ou editar visita com data passada dispara o
   recálculo do dashboard (ADR-0015); habitualidade e evolução já são sob demanda.
6. Sem marca de "retroativa" na interface por ora. Auditoria de criação/alteração
   existente registra quando foi lançada.

## Alternativas consideradas
- Só editar a data de visitas existentes → fluxo desajeitado (iniciar hoje para depois
  corrigir).
- Sem limite no passado → rejeitado: datas absurdas e recálculo de histórico enorme.
- Criar visita já encerrada em uma única chamada → rejeitado: o fluxo atual (iniciar,
  adicionar treinos/séries, encerrar) já cobre e mantém o padrão do ADR-0012.

## Consequências
- Alteração no UC31 (startedAt), novo UC59 (editar datas da visita) e observação no UC34.
- Todo cálculo por data precisa usar a data da visita; o Claude Code deve auditar os
  pontos que hoje usam o timestamp de criação.
- Um treino aberto em visita retroativa tem horário próprio "de agora"; a data
  de referência continua sendo a da visita.

## Referências
ADR-0012, ADR-0015, ADR-0016, ADR-0023.
