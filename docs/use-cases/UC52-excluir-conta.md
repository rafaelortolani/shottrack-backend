# UC52 - Excluir conta

## Ator
Atleta autenticado.

## Fluxo principal
1. Atleta envia a senha atual para confirmar a exclusão
2. Sistema valida a senha
3. Sistema apaga todos os dados do atleta (ver ADR-0019), numa única
   transação, e revoga as sessões
4. Sistema apaga os arquivos das fotos de alvos do atleta (depois do commit)
5. Sistema envia e-mail informando que a conta foi excluída

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 2a. Senha incorreta → `INVALID_CURRENT_PASSWORD` (nada é apagado)
- 2b. Senha ausente → `VALIDATION_ERROR`
- 3a. Falha no meio da exclusão → rollback completo (nada é apagado)
- 4a. Falha ao apagar algum arquivo de foto → a exclusão permanece (só registra o
  erro, sem dado pessoal); a rotina semanal de limpeza remove o órfão (ADR-0026)
- 5a. Falha no envio do e-mail → a exclusão permanece (só registra o erro)
- 1b. Limite de tentativas excedido → `TOO_MANY_REQUESTS`

## Observações
- Rota: `POST /api/users/me/deletion` com `{"password": "..."}` → `200` e
  `{"data": null}`.
- Tabelas apagadas, nesta ordem, numa única transação: `series_photos`, `dashboard_summary`
  (com `dashboard_summary_modality_records`, `dashboard_summary_modality_stats` e
  `dashboard_summary_records`), `athlete_habituality_tracking`, `athlete_personal_goal`
  (ADR-0023), `series_results`, `series`,
  `trainings`, `visits`, `training_locations`, `accessory_weapons`, `accessories`,
  `ammunitions`, `weapons`, `modality_result_type_selections`,
  `practiced_modalities`, `refresh_tokens`, `email_verification_codes`,
  `password_reset_tokens`, `pending_registrations` (pelo e-mail) e `users`.
- As chaves dos arquivos de foto são coletadas antes de apagar as linhas; os
  arquivos (imagem e miniatura) são removidos depois do commit, para que um
  rollback nunca deixe registros sem arquivo.
- A linha do usuário é travada no início da transação, para que um evento de
  recálculo do dashboard em voo não regrave o resumo no meio da exclusão. O
  consumidor da fila ignora, em silêncio, evento de usuário inexistente.
- O e-mail de aviso (pt/en) sai depois do commit; se falhar, só se registra o
  tipo do erro (nunca o endereço).
- Limite: 5 tentativas por hora por usuário (por IP quando não há usuário),
  contando também as de senha errada.
- Auditoria de planos (ADR-0027): as linhas de auditoria em que o usuário aparece como ator ou alvo permanecem, com essa
  referência anonimizada (nula), na mesma transação. O plano do usuário some junto com a linha de `users`.
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
- [x] Teste: atleta com fotos de alvos — nenhuma linha em `series_photos` e nenhum arquivo (imagem
      ou miniatura) restante do atleta; arquivos de OUTRO atleta permanecem intactos
- [x] Teste: falha ao apagar arquivo de foto não desfaz a exclusão da conta (`SeriesPhotoCascadeTest`)
- [x] Teste: conta que foi ator ou alvo de alteração de plano/limite é excluída; o registro de auditoria permanece com a referência anonimizada

## Referências
- ADR-0019, ADR-0018, ADR-0006, ADR-0026, ADR-0027
