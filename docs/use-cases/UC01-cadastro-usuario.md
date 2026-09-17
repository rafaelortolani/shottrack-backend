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
- 2b. Já existe conta ativa com esse email → erro `EMAIL_ALREADY_REGISTERED`

## Observação — reenvio
Se o visitante solicitar cadastro de novo pro mesmo email antes de
completar a etapa 2, o token anterior (ainda não usado) é invalidado e um
novo é gerado e enviado — mesmo endpoint, sem rota separada de reenvio.

## Definição de pronto
- [ ] Teste cobrindo o fluxo principal
- [ ] Teste cobrindo o reenvio (token anterior invalidado)
- [ ] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0009 (cadastro em duas etapas via link)
- UC23 (completar cadastro)