# UC04 - Editar dados do perfil

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal — editar nome e nível de experiência
1. Atleta envia novo nome e nível de experiência (`BEGINNER`/iniciante,
   `INTERMEDIATE`/intermediário ou `ADVANCED`/avançado) — os dois campos são
   sempre enviados juntos nesse endpoint, mesmo que só um deles tenha mudado
2. Sistema valida que o nome não está vazio e que o nível de experiência é um
   dos três valores permitidos, e atualiza os dois. Na primeira edição,
   marca o perfil como concluído (pendência "Criar perfil" do onboarding,
   UC42)

## Fluxo principal — trocar email
1. Atleta envia o novo email desejado **e a senha atual** (`currentPassword`) —
   `POST /api/users/me/email` `{ "email": "...", "currentPassword": "..." }`
2. Sistema valida a senha atual (mesma regra do UC05) e verifica que o novo email
   não está em uso por outra conta
3. Sistema envia código de verificação (6 dígitos, expira em 15 min) para o
   **novo** email — o email de login continua sendo o antigo até aqui — e um
   **aviso ao email ANTIGO** (sem o código) dizendo que houve um pedido de troca
4. Atleta confirma o código num endpoint separado
5. Sistema revalida que o novo email ainda está livre e efetiva a troca: o novo email
   passa a ser o email de login, **todas as sessões (refresh tokens) são revogadas**
   — o front leva de volta ao login — e o email antigo recebe um aviso da troca

## Fluxos alternativos
- 1a. Nome vazio → erro de validação
- 1b. Nível de experiência vazio ou fora dos valores permitidos → erro de validação
- 1c. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1d. `role` enviado no corpo → ignorado; o papel só muda direto no banco (ADR-0021)
- 2a. Novo email já cadastrado por outra conta → erro `EMAIL_ALREADY_REGISTERED`
- 2b. `currentPassword` ausente/em branco → erro de validação (`VALIDATION_ERROR`)
- 2c. `currentPassword` incorreta → `INVALID_CURRENT_PASSWORD` (401); a senha é conferida
  antes de revelar se o email já existe e nada é gravado nem enviado
- 3a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 4a. Código incorreto → erro `INVALID_VERIFICATION_CODE`
- 4b. Código expirado → erro `VERIFICATION_CODE_EXPIRED`, atleta pode pedir reenvio
- 5a. O novo email foi ocupado entre o pedido e a confirmação (inclusive corrida contra a
  restrição única do banco) → `EMAIL_ALREADY_REGISTERED` (409), nada muda e as sessões continuam

## Definição de pronto
- [x] Teste cobrindo o fluxo principal (editar nome e nível de experiência)
- [x] Teste cobrindo o fluxo alternativo 1a (nome vazio)
- [x] Teste cobrindo o fluxo alternativo 1b (nível de experiência inválido)
- [x] Teste cobrindo o fluxo principal (trocar email, incluindo confirmação do código)
- [x] Troca sem senha / com senha errada / com senha certa (`EmailChangeProtectionTest`)
- [x] Aviso ao email antigo no pedido (sem o código, pt/en) e na confirmação; falha no aviso não derruba o pedido
- [x] Refresh tokens revogados ao confirmar; código errado não revoga nem avisa
- [x] Unicidade revalidada na confirmação (inclusive corrida no banco)
- [x] Teste para CADA fluxo alternativo listado acima
- [x] Teste: `role` no corpo não altera o papel (1d)

## Limite de tentativas (ADR-0018)
`POST /api/users/me/email`: 5 pedidos / hora por usuário; `POST /api/users/me/email/confirmation`: 10 tentativas /
15 min por usuário → `429 TOO_MANY_REQUESTS` com `Retry-After`.

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0002 (verificação de email)
- ADR-0021 (papel de administrador)