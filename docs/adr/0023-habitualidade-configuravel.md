# ADR-0023: Habitualidade configurável (regras, versões, níveis e exigências)

## Contexto
O ShotTrack ajuda o atleta a acompanhar a habitualidade exigida pela norma de tiro
desportivo. As exigências (quantidade de treinos, competições, níveis) mudam por
decreto/instrução normativa; não podem ficar fixas no código. O produto precisa de um
cadastro editável pelo administrador (ADR-0021), com histórico de vigência.

## Decisão
1. **Modelo**: Regra → Versão (data de vigência) → Nível → Exigências.
   - Regra: nome, referência legal, ativa/inativa. Não é excluída, só desativada.
   - Versão: `validFrom` (data de vigência) e `windowMonths` (janela de apuração).
     A versão em vigor é a de maior `validFrom` ≤ hoje.
   - Nível (ex.: Iniciante, Intermediário): nome e posição, dentro da versão.
   - Exigência (por nível): tipo de atividade (`TREINO`, `COMPETICAO`, `QUALQUER`),
     quantidade mínima e filtros opcionais: só em clube; eventos distintos; âmbito
     mínimo (`ESTADUAL`, `NACIONAL`, `INTERNACIONAL`). Todas as exigências de um nível
     precisam ser cumpridas (E lógico).
2. **Edição**: versão com `validFrom` futuro é editável/removível. Mudança de norma =
   nova versão com data futura. Versão vigente ou passada só é editável enquanto
   nenhum atleta acompanha a regra (para corrigir o cadastro inicial). Primeira versão
   de uma regra pode ter data passada ou de hoje.
3. **Atleta**: escolhe a regra e o nível que acompanha (um acompanhamento por atleta).
   O nível é guardado pelo **nome** dentro da regra. Quando uma nova versão entra em
   vigor, o nível é mantido se existir com o mesmo nome; senão o atleta precisa
   escolher de novo. Com versão futura cadastrada, o atleta é avisado da mudança.
4. **Meta pessoal**: o atleta define um número de treinos por ano civil. É dado do
   atleta, fora da administração e dos níveis.
5. **Apuração**: janela móvel dos últimos `windowMonths` meses até hoje; conta
   somente visitas **encerradas**; calculada sob demanda (ADR-0016), sem fila.
6. **Dados novos na visita**: tipo (`TREINO` padrão / `COMPETICAO`) e âmbito (só para
   competição, opcional). **Local** de treino ganha a marca "é clube".
   "Eventos distintos" = pares distintos (data, local) entre as visitas de competição.
7. **Sem valores legais no seed**: nenhuma regra é carregada por migration; o
   administrador cadastra após conferir a norma vigente.
8. **Texto obrigatório na interface**: "Controle pessoal. A comprovação oficial é feita
   pelo seu clube."
9. Todas as rotas de gestão ficam em `/api/admin/habituality/**` (ADMIN).

## Alternativas consideradas
- Regras fixas no código → rejeitado: a norma muda.
- Nível deduzido pelo sistema → rejeitado: não conhecemos a graduação oficial do atleta.
- Casar nível por id → rejeitado: ids mudam a cada versão; o nome sobrevive.

## Consequências
- Várias tabelas novas e alterações em visita e local de treino (migrations).
- Visitas antigas ficam `TREINO` e sem âmbito; locais antigos ficam "não é clube".
- Frontend ganha a 5ª aba do dashboard e telas em /admin/habitualidade.
- Responsabilidade: a ferramenta é auxiliar e não substitui o controle oficial.

## Referências
ADR-0011, ADR-0013, ADR-0016, ADR-0019, ADR-0021, ADR-0022.
