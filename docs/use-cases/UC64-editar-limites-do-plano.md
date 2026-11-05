# UC64 - Editar limites do plano (administrador)

## Ator
Administrador autenticado.

## Pré-condição
Usuário logado (UC02) com papel ADMIN.

## Rota
`PATCH /api/admin/plans/{code}/limits` `{ "limits": { "PHOTOS_PER_SERIES": 8, "STORAGE_BYTES_PER_ATHLETE": 1073741824 } }`

## Fluxo principal
1. Administrador envia os limites que quer alterar (edição parcial)
2. Sistema valida chaves e faixas, grava os valores, registra a auditoria (quem, quando, chave, valor anterior e novo)
   e invalida o cache de limites
3. Sistema devolve o plano atualizado (mesmo formato do UC63)

## Regras
- Só chaves do enum e só valores inteiros dentro da faixa (ADR-0027). Chave reservada (`enforced=false`) é recusada.
- Valor igual ao atual não gera auditoria. Valor igual ao padrão do enum pode ser gravado (passa a ser explícito).
- Reduzir limite não apaga nem altera dados de atletas (ADR-0027, item 6).
- Efeito imediato nos próximos envios (cache de no máximo 60 s; invalidado nesta operação).

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 1b. Sem papel ADMIN → `FORBIDDEN`
- 1c. Plano inexistente → `PLAN_NOT_FOUND`
- 2a. Chave desconhecida ou reservada → `PLAN_LIMIT_INVALID_KEY`
- 2b. Valor fora da faixa ou não inteiro → `PLAN_LIMIT_OUT_OF_RANGE`
- 2c. Nenhum limite enviado → `VALIDATION_ERROR`

## Definição de pronto
- [x] Editar um e vários limites; auditoria com valor anterior/novo; sem auditoria quando o valor não muda
- [x] Um teste para CADA fluxo alternativo
- [x] Reduzir abaixo do uso atual: nada é apagado, novo envio é recusado, remoção de fotos continua funcionando
- [x] Aumentar libera novos envios logo em seguida (cache invalidado)
- [x] Atleta comum não consegue (403)

## Referências
- ADR-0021, ADR-0027
- UC60 (aplica os limites)
