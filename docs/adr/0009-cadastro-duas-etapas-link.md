# ADR-0009: Cadastro em duas etapas via link de confirmação de email

## Contexto
O cadastro direto (UC01 original: nome + email + senha, tudo de uma vez,
sem verificação) permite que qualquer pessoa crie uma conta usando o email
de outra pessoa, sem nenhuma prova de posse daquele email. O ADR-0002 já
havia identificado esse risco e decidiu adiá-lo; agora ele é resolvido —
mas com um mecanismo diferente do usado na troca de email (ADR-0007,
código de 6 dígitos).

## Decisão
Cadastro passa a ter duas etapas, ligadas por um **link** (não código):

1. Visitante informa **só o email**.
2. Sistema valida que não existe conta ativa com esse email, cria um
   registro de "cadastro pendente" com um token único, e envia um email
   com um link (`.../cadastro/completar?token=...`). O token expira em
   **24 horas**.
3. Visitante clica no link, chega numa tela que pede **nome e senha**.
4. Sistema valida o token (existe, não expirado, não usado), cria a conta
   de verdade, e marca o token como usado.

**Reenvio**: se o visitante solicitar cadastro de novo pro mesmo email
antes de completar, o token anterior é invalidado e um novo é gerado e
enviado — não existe endpoint de reenvio separado, é o mesmo endpoint do
passo 1, idempotente por email.

## Alternativas consideradas
- Código de 6 dígitos, mesmo padrão do UC04/ADR-0007 → rejeitado: nesse
  momento o visitante ainda não tem nada "no app" pra digitar o código de
  volta — link é mais natural pra abrir e continuar de onde parou.
- Cadastro direto + verificação depois (`email_verified = false`, conta
  já existe mas marcada) → rejeitado: não resolve o problema de origem —
  alguém já teria "ocupado" o email de outra pessoa mesmo que a conta
  fique marcada como não verificada.

## Consequências
- **UC01 original é substituído** — cadastro direto (nome+email+senha
  numa chamada só) deixa de existir.
- Precisa de tabela nova pra cadastro pendente (email, token, criado em,
  expira em, usado em).
- Login (UC02) não muda — usuário só existe de verdade após completar
  a etapa 2.
- Testes automatizados que criavam usuário via `POST /api/users` direto
  (inclusive os helpers de teste E2E do frontend) precisam ser
  atualizados pro fluxo de duas chamadas.

## Referências
- ADR-0002 (decisão original adiada, agora resolvida por este ADR)
- ADR-0007 (mecanismo de código por email, mantido só pra troca de email)
- UC01 (revisado), UC23 (completar cadastro)