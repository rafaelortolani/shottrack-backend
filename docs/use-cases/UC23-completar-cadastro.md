# UC23 - Completar cadastro

## Ator
Visitante que clicou no link de confirmação recebido por email.

## Pré-condição
Existe um cadastro pendente com token válido (gerado pelo UC01).

## Fluxo principal
1. Visitante acessa a tela com o token (vindo do link)
2. Visitante informa nome e senha e aceita os Termos de Uso e a Política de
   Privacidade (`acceptedTerms: true` — ADR-0019)
3. Sistema valida o token (existe, não expirado, não usado) e os dados
   informados
4. Sistema cria a conta de verdade, grava o aceite (`terms_accepted_at` = agora,
   `terms_version` = `app.legal.version`), marca o token como usado, e retorna
   os dados do usuário criado (sem logar automaticamente — mesmo
   comportamento do UC01 original: visitante vai pro login depois)

## Fluxos alternativos
- 3a. Token não existe → erro `REGISTRATION_TOKEN_INVALID`
- 3b. Token expirado → erro `REGISTRATION_TOKEN_EXPIRED`
- 3c. Token já usado → erro `REGISTRATION_TOKEN_ALREADY_USED`
- 3d. Nome vazio → erro de validação
- 3e. Senha com menos de 8 caracteres → erro de validação
- 3g. O email foi ocupado por outra conta entre o pedido e a conclusão (inclusive corrida contra a
  restrição única do banco) → `EMAIL_ALREADY_REGISTERED` (409); o link não é consumido
  (checado depois do aceite dos termos e antes de consumir o token)
- 3f. `acceptedTerms` ausente ou falso → erro `TERMS_NOT_ACCEPTED` (o cadastro
  não é concluído e o link continua válido; checado depois do token e antes de
  consumi-lo)

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima
- [x] Teste: conclusão com aceite grava data e versão
- [x] Teste: sem aceite → `TERMS_NOT_ACCEPTED`, link ainda utilizável
- [x] Teste: email ocupado antes de concluir → `EMAIL_ALREADY_REGISTERED` (e corrida no banco), link não consumido

## Limite de tentativas (ADR-0018)
`POST /api/users/registration/completion`: 10 tentativas / hora por IP → `429 TOO_MANY_REQUESTS`.

## Referências
- ADR-0009 (cadastro em duas etapas via link)
- ADR-0019 (aceite de termos)
- UC01 (revisado — gera o token que este use case consome)