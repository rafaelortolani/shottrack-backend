# UC63 - Listar planos e limites (administrador)

## Ator
Administrador autenticado.

## Pré-condição
Usuário logado (UC02) com papel ADMIN (ADR-0021).

## Rota
`GET /api/admin/plans`

## Fluxo principal
1. Administrador solicita a lista de planos
2. Sistema devolve cada plano (`code`, `name`, `active`, `default`, `athleteCount`) com todos os limites: `key`, `value`
   efetivo, `unit`, `min`, `max`, `defaultValue`, `enforced` (false para chaves reservadas) e `customized` (valor difere do padrão)

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 1b. Usuário sem papel ADMIN → `FORBIDDEN`

## Definição de pronto
- [x] Lista com o plano Gratuito e as 5 chaves (a reservada com `enforced=false`)
- [x] `athleteCount` correto
- [x] Um teste para CADA fluxo alternativo; atleta comum recebe 403

## Referências
- ADR-0021, ADR-0027
