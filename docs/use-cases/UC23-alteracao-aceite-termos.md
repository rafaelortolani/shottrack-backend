# UC23 - Completar cadastro — ALTERAÇÃO (ADR-0019)

> Acrescentar ao docs/use-cases/UC23-completar-cadastro.md.

## Fluxo principal (passo adicional)
O atleta envia, junto com nome e senha, a confirmação de aceite dos Termos de
Uso e da Política de Privacidade (`acceptedTerms: true`). O sistema grava
`terms_accepted_at` (agora) e `terms_version` (valor de `app.legal.version`).

## Fluxo alternativo novo
- `acceptedTerms` ausente ou falso → erro `TERMS_NOT_ACCEPTED` (o cadastro
  não é concluído e o link continua válido)

## Definição de pronto (acrescentar)
- [ ] Teste: conclusão com aceite grava data e versão
- [ ] Teste: sem aceite → `TERMS_NOT_ACCEPTED`, link ainda utilizável
