# UC53 - Exportar meus dados

## Ator
Atleta autenticado.

## Fluxo principal
1. Atleta solicita a exportação informando a **senha atual** —
   `POST /api/users/me/export` `{ "currentPassword": "..." }`
2. Sistema confere a senha (mesma regra da exclusão, UC52), reúne todos os dados do atleta e devolve um arquivo JSON para
   download (`Content-Disposition: attachment`, nome
   `shottrack-dados-AAAAMMDD.json`)

## Conteúdo do arquivo
Perfil (nome, e-mail, nível de experiência, data de cadastro, aceite dos
termos), modalidades praticadas e tipos de resultado escolhidos, armas,
munições, acessórios (com armas associadas), locais de treino, visitas com
treinos, séries e resultados. NUNCA inclui hash de senha nem tokens.

## Observações
- Rota: `POST /api/users/me/export` — resposta JSON propositalmente sem o
  envelope `{"data": ...}` (os erros continuam no envelope `{"error": ...}`). A resposta
  traz `Cache-Control: no-store`.
- Antes era `GET /api/users/me/export` (sem senha). A rota antiga responde
  `405 DATA_EXPORT_REQUIRES_PASSWORD` por uma versão, para o front migrar sem ambiguidade.
- Sem ids internos: referências entre registros (ex: série → arma) vão
  embutidas por nome. Datas em ISO-8601 (UTC).
- Limite: 3 exportações por hora por usuário — senha errada também conta, como na exclusão.

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 1b. Atleta sem nenhum dado além do perfil → arquivo com listas vazias
- 1c. Limite excedido (3/hora por atleta) → `TOO_MANY_REQUESTS`
- 1d. `currentPassword` ausente/em branco → `VALIDATION_ERROR` (400)
- 1e. `currentPassword` incorreta → `INVALID_CURRENT_PASSWORD` (401), sem nenhum dado na resposta
- 1f. `GET /api/users/me/export` (rota antiga) → `DATA_EXPORT_REQUIRES_PASSWORD` (405)

## Definição de pronto
- [x] Teste do fluxo principal (todas as seções preenchidas)
- [x] Teste: não vaza hash de senha nem tokens
- [x] Teste: só dados do próprio atleta (isolamento)
- [x] Teste: atleta sem dados
- [x] Teste: sem token e rate limit
- [x] Teste: cabeçalho de download e nome do arquivo
- [x] Teste: sem senha / senha errada (sem vazar dados) / senha certa com `Cache-Control: no-store`
- [x] Teste: rota GET antiga responde erro claro; senha errada conta no limite

## Referências
- ADR-0019, ADR-0018
