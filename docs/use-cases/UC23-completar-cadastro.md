# UC23 - Completar cadastro

## Ator
Visitante que clicou no link de confirmação recebido por email.

## Pré-condição
Existe um cadastro pendente com token válido (gerado pelo UC01).

## Fluxo principal
1. Visitante acessa a tela com o token (vindo do link)
2. Visitante informa nome e senha
3. Sistema valida o token (existe, não expirado, não usado) e os dados
   informados
4. Sistema cria a conta de verdade, marca o token como usado, e retorna
   os dados do usuário criado (sem logar automaticamente — mesmo
   comportamento do UC01 original: visitante vai pro login depois)

## Fluxos alternativos
- 3a. Token não existe → erro `REGISTRATION_TOKEN_INVALID`
- 3b. Token expirado → erro `REGISTRATION_TOKEN_EXPIRED`
- 3c. Token já usado → erro `REGISTRATION_TOKEN_ALREADY_USED`
- 3d. Nome vazio → erro de validação
- 3e. Senha com menos de 8 caracteres → erro de validação

## Definição de pronto
- [ ] Teste cobrindo o fluxo principal
- [ ] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0009 (cadastro em duas etapas via link)
- UC01 (revisado — gera o token que este use case consome)