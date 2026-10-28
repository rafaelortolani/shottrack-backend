# UC54 — Gerenciar regras de habitualidade (admin)

## Ator
Administrador.

## Rotas
- `GET /api/admin/habituality/rules`
- `POST /api/admin/habituality/rules` { name, legalReference, active }
- `PUT /api/admin/habituality/rules/{id}` { name, legalReference, active }

## Fluxo principal
Admin lista, cria e edita regras (nome, referência legal, ativa). Regras não são
excluídas; desativar tira a regra da escolha de novos atletas (quem já acompanha vê o
aviso de regra inativa).

## Fluxos alternativos
- Sem token → `UNAUTHORIZED`; atleta comum → `FORBIDDEN`.
- Nome vazio/duplicado → `VALIDATION_ERROR`.
- Regra inexistente → `HABITUALITY_RULE_NOT_FOUND`.

## Definição de pronto
- [x] Criar, listar, editar, desativar/reativar
- [x] 401/403; nome duplicado; regra inexistente
- [x] Auditoria (quem criou/alterou)

## Swagger e mensagens
- Rotas anotadas com `@Operation` em `HabitualityAdminController` (erros e status descritos).
- Mensagens pt/en: `HABITUALITY_RULE_NOT_FOUND`, `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`.
- Auditoria: `createdBy`/`updatedBy` passam a ser preenchidos com o usuário autenticado
  (`AuditorAware` em `JpaAuditingConfig`); fora de requisição autenticada ficam nulos.
