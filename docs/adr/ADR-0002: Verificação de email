# ADR-0002: Verificação de email

## Contexto
Sem verificação, qualquer pessoa pode informar o email de outra pessoa. Esse
risco existe em dois pontos: no cadastro (UC01) e na troca de email (UC04).

## Decisão
O mecanismo de verificação (código por email + confirmação) é implementado
agora, mas usado **apenas na troca de email (UC04)** por enquanto:

1. Usuário pede troca de email em UC04
2. Sistema envia um código de 6 dígitos para o **novo** email, com expiração
   (ex: 15 minutos)
3. Usuário confirma o código num endpoint de verificação
4. Email é efetivamente alterado só após confirmação — até lá, o email antigo
   continua sendo o email de login

Em desenvolvimento, o envio roda contra o **Mailpit** (SMTP fake local, ver
`docker-compose.yml`) — nenhum email real é enviado, o código aparece na
interface web do Mailpit (`localhost:8025`).

## Adiado — verificação no cadastro (UC01)
Continua fora de escopo por agora: o UC01 não exige verificação de email no
momento do cadastro. Como o mecanismo já vai existir (por causa do UC04), a
diferença pra habilitar isso no cadastro no futuro é só de orquestração, não
de infraestrutura nova.

## Alternativas consideradas
- Código enviado antes do cadastro, usuário só preenche o formulário depois
  de já ter o código em mãos → rejeitado: mais fricção, sem ganho de
  segurança sobre o padrão "pendente até confirmar".
- Adiar o mecanismo inteiro (não implementar nada agora) → rejeitado: a troca
  de email exige verificação por decisão explícita do produto, e isso não é
  possível sem o mecanismo existir.

## Consequências
- Precisa de configuração SMTP na aplicação (Mailpit em dev; provedor real
  como SES/SendGrid/Resend em produção — decisão adiada pra quando o sistema
  for pra produção).
- Tabela nova para os códigos de verificação (código, email de destino,
  expiração, usado/não usado).
- UC04 (editar dados) depende deste ADR para o fluxo de troca de email.
- Antes de qualquer lançamento público: decidir se o cadastro (UC01) também
  passa a exigir verificação, e configurar o provedor SMTP real.