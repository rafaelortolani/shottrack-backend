# UC01 (revisado) - Solicitar cadastro

## Ator
Visitante (ainda não possui conta).

## Pré-condição
Nenhuma.

## Fluxo principal
1. Visitante informa o email
2. Sistema valida o formato do email e verifica que não existe conta
   ativa com esse email
3. Sistema cria (ou substitui, se já havia um pendente pro mesmo email —
   ver observação) um registro de cadastro pendente com token único,
   expirando em 24 horas
4. Sistema envia email com o link de confirmação
   (`.../cadastro/completar?token=...`)

## Fluxos alternativos
- 2a. Email inválido → erro de validação
- 2b. Já existe conta ativa com esse email → **resposta neutra** (S05): a mesma `202 {"data":null,"error":null}` de um
  e-mail novo. Nenhum cadastro pendente é criado e o dono da conta recebe o aviso "alguém tentou cadastrar seu
  e-mail" (pt/en, sem link nem token). Quem consulta a rota não descobre se o e-mail tem conta.
- 2c. O e-mail é normalizado (sem espaços nas pontas, minúsculas — S15): `Foo@X.com` e `foo@x.com` são a mesma conta.

- 4a. Falha ao enviar o e-mail (SMTP fora do ar) → a resposta continua `202` igual à de qualquer outro caso (N1/S05: senão
  só o e-mail livre daria erro e revelaria quem tem conta); o pedido fica registrado, só o tipo da exceção vai para o log e o
  visitante pede de novo (dentro do limite de taxa). Vale também para o aviso ao dono da conta. A falha é logada em
  WARN (não ERROR, para não virar um evento no Sentry por requisição); `AccountEnumerationTest` cobre o nível e a ausência do e-mail.
- Diferença de custo que sobra (aceita): o e-mail livre faz duas gravações a mais (invalidar o pendente anterior e gravar o novo)
  que o e-mail já cadastrado não faz. Igualar exigiria gravações falsas; com o envio SMTP síncrono (dezenas a centenas de ms)
  dominando o tempo e o limite de taxa por e-mail/IP, a diferença não foi tratada.

## Observação — reenvio
Se o visitante solicitar cadastro de novo pro mesmo email antes de
completar a etapa 2, o token anterior (ainda não usado) é invalidado e um
novo é gerado e enviado — mesmo endpoint, sem rota separada de reenvio.

## Definição de pronto
- [ ] Teste cobrindo o fluxo principal
- [ ] Teste cobrindo o reenvio (token anterior invalidado)
- [ ] Um teste para CADA fluxo alternativo listado acima
- [x] Teste: conta existente responde igual a e-mail novo, sem pendente e com aviso ao dono (`AccountEnumerationTest`)
- [x] Teste: e-mail com maiúsculas/espaços é a mesma conta (`AccountEnumerationTest`)

## Limites de entrada (Lote 3, S10/S18)
- E-mail: até 254 caracteres. Corpo da requisição: até 64 KB (`413 PAYLOAD_TOO_LARGE`).

## Referências
- ADR-0009 (cadastro em duas etapas via link)
- UC23 (completar cadastro)