# UC67 - Histórico de alterações de planos (administrador)

## Ator
Administrador autenticado.

## Pré-condição
Usuário logado (UC02) com papel ADMIN.

## Rota
`GET /api/admin/plans/audit?limit=50`

## Fluxo principal
1. Administrador solicita o histórico
2. Sistema devolve as alterações mais recentes primeiro: data, tipo (`LIMIT_CHANGED` ou `PLAN_ASSIGNED`), quem fez
   (nome ou e-mail do administrador), alvo (plano, ou atleta), chave, valor anterior e novo

## Regras
- `limit` de 1 a 200 (padrão 50).
- Referências a usuários excluídos aparecem como "conta excluída" (anonimização do UC52).
- Somente leitura; não expõe hash de senha, tokens nem dados além de nome/e-mail necessários para identificar.

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 1b. Sem papel ADMIN → `FORBIDDEN`
- 1c. `limit` fora da faixa → `VALIDATION_ERROR`

## Definição de pronto
- [x] Histórico após editar limite e após trocar plano
- [x] Um teste para CADA fluxo alternativo
- [x] Ator ou alvo com conta excluída aparece anonimizado
- [x] Atleta comum não consegue (403)

## Referências
- ADR-0021, ADR-0027
- UC52
