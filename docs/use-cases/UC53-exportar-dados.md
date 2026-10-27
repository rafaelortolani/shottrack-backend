# UC53 - Exportar meus dados

## Ator
Atleta autenticado.

## Fluxo principal
1. Atleta solicita a exportação
2. Sistema reúne todos os dados do atleta e devolve um arquivo JSON para
   download (`Content-Disposition: attachment`, nome
   `shottrack-dados-AAAAMMDD.json`)

## Conteúdo do arquivo
Perfil (nome, e-mail, nível de experiência, data de cadastro, aceite dos
termos), modalidades praticadas e tipos de resultado escolhidos, armas,
munições, acessórios (com armas associadas), locais de treino, visitas com
treinos, séries e resultados. NUNCA inclui hash de senha nem tokens.

## Observações
- Rota: `GET /api/users/me/export` — resposta JSON propositalmente sem o
  envelope `{"data": ...}`.
- Sem ids internos: referências entre registros (ex: série → arma) vão
  embutidas por nome. Datas em ISO-8601 (UTC).
- Limite: 3 exportações por hora por usuário.

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 1b. Atleta sem nenhum dado além do perfil → arquivo com listas vazias
- 1c. Limite excedido (3/hora por atleta) → `TOO_MANY_REQUESTS`

## Definição de pronto
- [x] Teste do fluxo principal (todas as seções preenchidas)
- [x] Teste: não vaza hash de senha nem tokens
- [x] Teste: só dados do próprio atleta (isolamento)
- [x] Teste: atleta sem dados
- [x] Teste: sem token e rate limit
- [x] Teste: cabeçalho de download e nome do arquivo

## Referências
- ADR-0019, ADR-0018
