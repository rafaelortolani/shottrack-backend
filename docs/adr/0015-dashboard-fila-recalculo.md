# ADR-0015: Dashboard pré-calculado via fila, recálculo completo por evento

## Contexto
O UC42 (consultar dashboard) calcula as estatísticas na hora da consulta.
Decisão: mover esse cálculo pra background, disparado por eventos, pra
não impactar o tempo de resposta das ações que geram os dados (registrar
série, editar série, registrar/remover resultado, encerrar treino,
encerrar visita).

## Decisão
- **RabbitMQ** como fila, com retry e dead-letter queue (DLQ) — falha no
  processamento não é silenciosa, fica visível e tenta de novo.
- Os 9 pontos de escrita relevantes (UC33, UC34, UC36, UC38, UC39, UC40,
  UC41, UC43, UC44) publicam um evento leve `DashboardRecalculationRequested`
  contendo só `atletaId`, depois que a transação principal já commitou —
  nunca antes, pra não publicar evento de uma escrita que pode dar rollback.
- Um consumidor escuta esse evento e **recalcula do zero** todas as
  estatísticas daquele atleta (mesma lógica já desenhada no UC42 — nenhuma
  lógica nova, só muda o destino: grava numa tabela `dashboard_summary`
  em vez de responder direto numa requisição HTTP).
- Recalcular do zero é naturalmente idempotente: processar o mesmo evento
  duas vezes (reentrega da fila, por exemplo) produz o mesmo resultado,
  sem necessidade de deduplicação.
- `GET /api/dashboard` (UC42, revisado) passa a **ler** de
  `dashboard_summary`. Se não existir linha ainda pra aquele atleta
  (primeiro acesso, antes de qualquer evento ter sido processado), calcula
  na hora como fallback (mesma lógica), sem esperar o evento.

## Revisão — Onda 1 do dashboard (UC42)
Nem tudo do UC42 entra no resumo pré-calculado. O critério é se as
escritas que alimentam a seção publicam o evento de recálculo:
- **No resumo** (`dashboard_summary`): indicadores do mês, modalidades
  praticadas, resumo de modalidades (`dashboard_summary_modality_stats`)
  e recordes (`dashboard_summary_records`, ADR-0016). Dependem do histórico inteiro de séries/resultados,
  que só muda pelos pontos de escrita acima.
- **Calculado na hora, nos dois caminhos de leitura**: onboarding, ação
  principal, últimos treinos e acervo. Dependem de escritas que não
  publicam evento (perfil, modalidades praticadas, armas, iniciar
  visita/abrir treino, editar local), e são baratos: no máximo 5 treinos,
  nunca o histórico inteiro.

A apuração de melhor valor (ADR-0011) é uma só, parametrizada pelo
conjunto de resultados: recordes do atleta inteiro, destaque de uma
modalidade ou de um treino.

## Alternativas consideradas
- Atualização incremental do resumo a cada escrita (somar/comparar em vez
  de recalcular tudo) → rejeitado: mais rápido de processar, mas cada
  ponto de escrita precisaria de lógica própria e correta pra manter o
  resumo consistente — superfície de bug maior. Recalcular tudo é mais
  simples de manter correto, e o volume de dado por atleta é pequeno o
  bastante pra isso não pesar.
- `@Async` do Spring sem fila de verdade → rejeitado: sem retry nem DLQ,
  falha vira inconsistência silenciosa e permanente.
- Publicar o evento antes do commit da transação principal → rejeitado:
  arriscaria disparar recálculo de uma escrita que não vingou (rollback).

## Consequências
- Precisa de RabbitMQ no `docker-compose.yml` (dev e teste) e da
  dependência `spring-boot-starter-amqp`.
- Nova tabela `dashboard_summary` (um registro por atleta).
- UC42 passa a ter dois caminhos de leitura: tabela de resumo (caminho
  normal) e cálculo direto como fallback (primeiro acesso).
- Os 6 use cases de escrita ganham um efeito colateral (publicar evento)
  — precisa de teste garantindo que o evento é publicado, além dos testes
  que já existiam.
- Testes do consumidor podem rodar a lógica de recálculo diretamente
  (sem precisar de fila de verdade rodando), já que ela é a mesma do
  fallback do UC42 — só o teste de publicação/consumo de evento em si
  precisa da fila.

## Referências
- UC42 (consultar dashboard — revisado)
- ADR-0013 (Série — mesma lógica de cálculo reaproveitada)