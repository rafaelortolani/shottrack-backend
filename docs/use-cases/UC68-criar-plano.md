# UC68 - Criar plano (administrador)

## Ator
Administrador autenticado.

## Pré-condição
Usuário logado (UC02) com papel ADMIN (ADR-0021).

## Rota
`POST /api/admin/plans` `{ "code": "PAID", "name": "Pago", "copyFromCode": "FREE" }` (`copyFromCode` opcional)

## Fluxo principal
1. Administrador informa código, nome e, opcionalmente, o plano do qual copiar os limites
2. Sistema valida, cria o plano **ativo** e **não padrão**, copia os limites (se pedido) e registra a auditoria `PLAN_CREATED`
3. Sistema devolve o plano criado (mesmo formato do UC63) com status 201

## Regras
- `code`: `^[A-Z][A-Z0-9_]{1,29}$`, único (sem diferenciar maiúsculas/minúsculas) e **imutável** depois de criado.
- `name`: 2 a 60 caracteres, sem espaços nas pontas; único sem diferenciar maiúsculas/minúsculas.
- Sem `copyFromCode`, o plano nasce sem valores gravados e usa os padrões do enum (ADR-0027); depois se ajusta com o UC64.
- Máximo de 20 planos.

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 1b. Sem papel ADMIN → `FORBIDDEN`
- 2a. Código fora do formato → `PLAN_CODE_INVALID`
- 2b. Código ou nome já existente → `PLAN_CODE_ALREADY_EXISTS` / `PLAN_NAME_ALREADY_EXISTS`
- 2c. Nome inválido → `PLAN_NAME_INVALID`
- 2d. `copyFromCode` inexistente → `PLAN_NOT_FOUND`
- 2e. Já existem 20 planos → `PLAN_COUNT_LIMIT_REACHED`

## Definição de pronto
- [x] Criar com e sem cópia de limites; plano nasce ativo e não padrão
- [x] Um teste para CADA fluxo alternativo (incluindo código duplicado com caixa diferente)
- [x] Auditoria `PLAN_CREATED`; nada gravado em caso de erro
- [x] Atleta comum não consegue (403)

## Referências
- ADR-0021, ADR-0027
- UC63, UC64
