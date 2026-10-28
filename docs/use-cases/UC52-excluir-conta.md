# UC52 - Excluir conta

## Ator
Atleta autenticado.

## Fluxo principal
1. Atleta envia a senha atual para confirmar a exclusão
2. Sistema valida a senha
3. Sistema apaga todos os dados do atleta (ver ADR-0019), numa única
   transação, e revoga as sessões
4. Sistema envia e-mail informando que a conta foi excluída

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 2a. Senha incorreta → `INVALID_CURRENT_PASSWORD` (nada é apagado)
- 2b. Senha ausente → `VALIDATION_ERROR`
- 3a. Falha no meio da exclusão → rollback completo (nada é apagado)
- 4a. Falha no envio do e-mail → a exclusão permanece (só registra o erro)
- 1b. Limite de tentativas excedido → `TOO_MANY_REQUESTS`

## Observações
- Rota: `POST /api/users/me/deletion` com `{"password": "..."}` → `200` e
  `{"data": null}`.
- Tabelas apagadas, nesta ordem, numa única transação: `dashboard_summary`
  (com `dashboard_summary_modality_records`, `dashboard_summary_modality_stats` e
  `dashboard_summary_records`), `series_results`, `series`, `trainings`,
  `visits`, `training_locations`, `accessory_weapons`, `accessories`,
  `ammunitions`, `weapons`, `modality_result_type_selections`,
  `practiced_modalities`, `refresh_tokens`, `email_verification_codes`,
  `password_reset_tokens`, `pending_registrations` (pelo e-mail) e `users`.
- A linha do usuário é travada no início da transação, para que um evento de
  recálculo do dashboard em voo não regrave o resumo no meio da exclusão. O
  consumidor da fila ignora, em silêncio, evento de usuário inexistente.
- O e-mail de aviso (pt/en) sai depois do commit; se falhar, só se registra o
  tipo do erro (nunca o endereço).
- Limite: 5 tentativas por hora por usuário (por IP quando não há usuário),
  contando também as de senha errada.
- Um access token já emitido continua válido até expirar (JWT stateless,
  ADR-0001): as rotas `/me` respondem `UNAUTHORIZED`, e criar registros com ele
  falha pela FK do usuário.

## Definição de pronto
- [x] Teste do fluxo principal com dados em TODAS as tabelas do atleta
      (armas, munições, acessórios com associação, locais, visitas, treinos,
      séries, resultados, modalidades e seleções, resumo do dashboard, tokens)
- [x] Teste: login e refresh depois da exclusão falham
- [x] Teste: dados de OUTRO atleta permanecem intactos
- [x] Um teste para CADA fluxo alternativo
- [x] Teste: cadastro pendente do mesmo e-mail também é removido

## Referências
- ADR-0019, ADR-0018, ADR-0006
