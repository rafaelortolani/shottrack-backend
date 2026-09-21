# ADR-0012: Visita e Treino — múltiplos treinos simultâneos, encerramento em cascata

## Contexto
O documento de domínio descreve Visita como a permanência do atleta num
local, dentro da qual ele pode abrir vários treinos (cada um numa
modalidade) e alternar entre eles livremente, sem precisar encerrar um
pra abrir outro. Ao encerrar a visita com treinos ainda abertos, o
sistema deve alertar, mas não impedir.

Este é o primeiro corte desse domínio — Série (a menor unidade de
registro, com arma/munição/resultados) ainda não existe; fica pra um
próximo bloco.

## Decisão
**Visita**: pertence ao atleta e a um Local de Treino (ADR-0010).
- `local_id` (obrigatório, precisa ser um local do próprio atleta)
- `iniciada_em` (automático, no momento da criação)
- `encerrada_em` (nulo enquanto em andamento)
- `status`: `EM_ANDAMENTO` / `ENCERRADA`
- `observacoes` (opcional)

**Treino**: pertence a uma Visita em andamento e a uma Modalidade
praticada pelo atleta (UC12).
- `visita_id`, `modalidade_id` (obrigatórios)
- `iniciado_em` (automático), `encerrado_em` (nulo enquanto em andamento)
- `status`: `EM_ANDAMENTO` / `ENCERRADO`
- Vários treinos podem estar `EM_ANDAMENTO` ao mesmo tempo na mesma
  visita, **inclusive repetindo a mesma modalidade** — não há restrição
  de unicidade.
- Só é possível abrir treino novo se a visita estiver `EM_ANDAMENTO`.

**Encerramento em cascata**: encerrar uma visita sempre encerra, junto,
qualquer treino dela que ainda esteja `EM_ANDAMENTO` (mesmo timestamp de
encerramento da visita) — sem exigir confirmação ou flag adicional na
chamada. O alerta ao atleta ("você tem treinos abertos, confirma?") é
responsabilidade do frontend, antes de chamar o endpoint; o backend nunca
bloqueia.

Encerrar um treino individualmente (sem encerrar a visita) continua
possível a qualquer momento — é uma ação separada.

## Alternativas consideradas
- Restringir um treino aberto por modalidade por visita → rejeitado:
  decisão explícita de permitir repetição (ex: atleta interrompe e
  retoma a mesma modalidade mais tarde na mesma visita).
- Exigir confirmação explícita (flag) pra encerrar visita com treino
  aberto, bloqueando sem ela → rejeitado: mais simples deixar o backend
  sempre aceitar e cascatear; a fricção de confirmar fica só na UI,
  onde faz mais sentido (o atleta já está decidindo ali).

## Consequências
- Nenhum endpoint de "forçar encerramento" separado — é o mesmo endpoint
  de encerrar visita, sempre.
- Local de Treino (ADR-0010) passa a ter uso real — a pendência de
  exclusão bloqueada (`TRAINING_LOCATION_IN_USE`, UC27) passa a valer de
  verdade a partir daqui.
- Série, Resultado e o restante do domínio de registro de disparos ficam
  pra um próximo ADR.

## Referências
- ADR-0010 (Local de Treino)
- UC12 (Modalidades praticadas — pré-condição pra abrir treino)