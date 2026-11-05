# UC65 - Alterar plano do atleta (administrador)

## Ator
Administrador autenticado.

## Pré-condição
Usuário logado (UC02) com papel ADMIN.

## Rota
`PUT /api/admin/users/{userId}/plan` `{ "planCode": "FREE" }`
(a localização do atleta por e-mail usa a busca de usuários do painel de admin já existente, ADR-0021; se não houver
busca por e-mail, criar `GET /api/admin/users?email=` somente leitura, sem devolver hash de senha nem tokens)

**Resultado da busca** (mín. 3 caracteres, até 10 itens): `id`, `name`, `email`, `planCode`, `planName`, `photoCount` (total de fotos do
atleta) e `storageBytes` (soma do tamanho das imagens processadas, sem miniaturas — a mesma fonte do limite
`STORAGE_BYTES_PER_ATHLETE`), para o administrador decidir a troca com informação. Atleta sem fotos → `0` e `0`. Nunca
devolve hash, tokens nem ids de fotos.

## Fluxo principal
1. Administrador informa o atleta e o novo plano
2. Sistema valida, troca o plano, registra a auditoria (quem, quando, plano anterior e novo) e invalida o cache
3. Sistema devolve o atleta com o plano atual

## Regras
- Só plano **ativo**. Atleta com mais dados que o novo plano permite mantém tudo e fica bloqueado para novos envios.
- Trocar para o mesmo plano não gera auditoria nem erro.
- Efeito imediato nos próximos envios.

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 1b. Sem papel ADMIN → `FORBIDDEN`
- 1c. Atleta inexistente → `USER_NOT_FOUND`
- 2a. Plano inexistente → `PLAN_NOT_FOUND`
- 2b. Plano inativo → `PLAN_NOT_ACTIVE`

## Definição de pronto
- [x] Trocar plano com auditoria; mesmo plano sem auditoria
- [x] Um teste para CADA fluxo alternativo
- [x] Atleta com uso acima do novo plano: dados preservados, novo envio recusado
- [x] Busca por e-mail não expõe hash de senha, tokens nem dados além do necessário
- [x] Atleta comum não consegue (403)

## Referências
- ADR-0021, ADR-0027
- [x] Busca devolve `photoCount` e `storageBytes` corretos (0 e 0 sem fotos; fotos de outro atleta não entram), numa única consulta agregada (sem N+1)
