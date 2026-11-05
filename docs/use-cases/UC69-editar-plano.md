# UC69 - Editar plano (administrador)

## Ator
Administrador autenticado.

## Pré-condição
Usuário logado (UC02) com papel ADMIN.

## Rota
`PATCH /api/admin/plans/{code}` `{ "name": "Pago mensal", "active": true, "default": false }` (todos opcionais, edição parcial)

## Fluxo principal
1. Administrador envia o que quer alterar: nome, ativo e/ou padrão
2. Sistema valida, aplica na mesma transação e registra a auditoria `PLAN_UPDATED` (campo, valor anterior e novo)
3. Sistema devolve o plano atualizado

## Regras
- O `code` não muda. Planos não são excluídos, só desativados.
- Desativar plano com atletas: permitido; os atletas **mantêm** o plano e os limites; o plano só deixa de poder ser atribuído (UC65).
- O **plano padrão** precisa ficar sempre ativo: não pode ser desativado. Para trocar o padrão, defina outro plano **ativo** como
  padrão (`default: true`); o anterior deixa de ser padrão na mesma transação.
- `default: false` no plano que é o padrão atual é recusado (sempre existe um padrão).
- Valores iguais aos atuais não geram auditoria.

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 1b. Sem papel ADMIN → `FORBIDDEN`
- 1c. Plano inexistente → `PLAN_NOT_FOUND`
- 2a. Nome inválido ou já existente → `PLAN_NAME_INVALID` / `PLAN_NAME_ALREADY_EXISTS`
- 2b. Desativar o plano padrão → `PLAN_DEFAULT_CANNOT_BE_DEACTIVATED`
- 2c. Definir como padrão um plano inativo → `PLAN_NOT_ACTIVE`
- 2d. Retirar o padrão sem indicar outro → `PLAN_DEFAULT_REQUIRED`
- 2e. Nenhum campo enviado → `VALIDATION_ERROR`

## Definição de pronto
- [x] Renomear; desativar e reativar; trocar o padrão (o anterior deixa de ser)
- [x] Um teste para CADA fluxo alternativo
- [x] Atletas de plano desativado mantêm plano e limites; plano desativado não aparece para atribuição
- [x] Sempre exatamente 1 plano padrão e ativo, mesmo com requisições simultâneas
- [x] Auditoria registrada e ausente quando nada muda; atleta comum não consegue (403)

## Referências
- ADR-0006, ADR-0021, ADR-0027
- UC63, UC65, UC68
