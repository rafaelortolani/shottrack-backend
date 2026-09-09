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
   dos três valores permitidos, e atualiza os dois

## Fluxo principal — trocar email
1. Atleta envia o novo email desejado
2. Sistema verifica que o novo email não está em uso por outra conta
3. Sistema envia código de verificação (6 dígitos, expira em 15 min) para o
   **novo** email — o email de login continua sendo o antigo até aqui
4. Atleta confirma o código num endpoint separado
5. Sistema efetiva a troca: o novo email passa a ser o email de login

## Fluxos alternativos
- 1a. Nome vazio → erro de validação
- 1b. Nível de experiência vazio ou fora dos valores permitidos → erro de validação
- 1c. Token ausente ou inválido → erro `UNAUTHORIZED`
- 2a. Novo email já cadastrado por outra conta → erro `EMAIL_ALREADY_REGISTERED`
- 3a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 4a. Código incorreto → erro `INVALID_VERIFICATION_CODE`
- 4b. Código expirado → erro `VERIFICATION_CODE_EXPIRED`, atleta pode pedir reenvio

## Definição de pronto
- [x] Teste cobrindo o fluxo principal (editar nome e nível de experiência)
- [x] Teste cobrindo o fluxo alternativo 1a (nome vazio)
- [x] Teste cobrindo o fluxo alternativo 1b (nível de experiência inválido)
- [x] Teste cobrindo o fluxo principal (trocar email, incluindo confirmação do código)
- [x] Teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0002 (verificação de email)