# UC04 - Editar dados do perfil

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal — editar nome
1. Atleta envia novo nome
2. Sistema valida (não vazio) e atualiza

## Fluxo principal — trocar email
1. Atleta envia o novo email desejado
2. Sistema verifica que o novo email não está em uso por outra conta
3. Sistema envia código de verificação (6 dígitos, expira em 15 min) para o
   **novo** email — o email de login continua sendo o antigo até aqui
4. Atleta confirma o código num endpoint separado
5. Sistema efetiva a troca: o novo email passa a ser o email de login

## Fluxos alternativos
- 1a. Nome vazio → erro de validação
- 2a. Novo email já cadastrado por outra conta → erro `EMAIL_ALREADY_REGISTERED`
- 3a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 4a. Código incorreto → erro `INVALID_VERIFICATION_CODE`
- 4b. Código expirado → erro `VERIFICATION_CODE_EXPIRED`, atleta pode pedir reenvio

## Definição de pronto
- [ ] Teste cobrindo o fluxo principal (editar nome)
- [ ] Teste cobrindo o fluxo principal (trocar email, incluindo confirmação do código)
- [ ] Teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0002 (verificação de email)