# UC42 - Consultar dashboard

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita o dashboard
2. Sistema busca o resumo pré-calculado do atleta em `dashboard_summary`
   (ADR-0015), calcula na hora as seções que dependem de dados sem evento
   de recálculo (onboarding, ação principal, últimos treinos, acervo) e
   retorna:
    - **Onboarding** (ver seção própria) — só presente se houver pendência
    - **Ação principal** — visita em andamento (local, modalidades dos
      treinos ativos) ou, se não houver, um convite genérico pra iniciar.
      Com mais de uma visita em andamento, vale a mais recente
    - **Indicadores universais**: treinos esse mês, disparos esse mês,
      modalidades praticadas
    - **Últimos treinos** (últimos 5): data, local, modalidade, e a
      métrica de destaque daquele treino específico (se ele tiver algum
      resultado registrado)
    - **Resumo de modalidades**: cada modalidade em que o atleta já
      treinou, contagem de
      treinos nela, e o melhor valor do tipo de resultado mais registrado
      NAQUELA modalidade (mesma lógica de "destaque", só que escopada por
      modalidade em vez do atleta inteiro)
    - **Resumo de acervo**: contagem de armas cadastradas + até 3 nomes
      (as mais recentes; nome é o apelido, ou marca + modelo sem apelido)
    - **Destaque dinâmico geral** (já existente — melhor valor do tipo
      mais registrado em toda a história do atleta)

## Onboarding — pendências de configuração inicial
Calculado a partir do estado real do atleta, sem tabela de progresso
própria:
- "Criar perfil": completo se o atleta já editou o perfil ao menos uma
  vez (UC04). `experienceLevel` não serve de critério: tem default
  `BEGINNER` desde o cadastro, então estaria sempre preenchido
- "Configurar modalidades": completo se há pelo menos 1 modalidade
  praticada (UC12)
- "Cadastrar arma": completo se há pelo menos 1 arma no acervo (UC06)

A seção de onboarding só aparece na resposta se **pelo menos uma**
pendência existir. Assim que as três estiverem completas, some da
resposta (não é um campo com "false" pra sempre, é ausência).

## Destaque dinâmico
Entre os tipos de resultado com orientação `MENOR_MELHOR` ou
`MAIOR_MELHOR` (ADR-0011), o que tem mais registros preenchidos; valor é
o melhor já registrado nesse tipo, respeitando a orientação; desempate
pelo mais usado recentemente; ausente se nenhum tipo elegível tiver
registro ainda. A mesma regra vale nos três escopos: o atleta inteiro
(destaque geral), uma modalidade e um treino.

## Últimos treinos — métrica de destaque por treino
Mesma lógica do destaque geral (tipo mais registrado, respeitando
orientação MENOR_MELHOR/MAIOR_MELHOR do ADR-0011), mas escopada só às
séries daquele treino específico. Se o treino não tem nenhum resultado
registrado, o campo de métrica vem ausente (a tela mostra só data/local/
modalidade, sem inventar métrica).

## Fluxo alternativo — sem resumo ainda (fallback)
- 2a. Não existe linha em `dashboard_summary` pra esse atleta ainda →
  calcula tudo na hora (mesma lógica do consumidor, ADR-0015)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Atleta sem nenhum dado ainda → onboarding completo aparece (3
  pendências), demais seções vêm vazias/zeradas, sem erro

## Observação
Valor de resultado é texto (ADR-0013) — em qualquer cálculo de "melhor
valor" (geral, por modalidade, por treino), valor não numérico é
ignorado silenciosamente.

## Definição de pronto
- [x] Teste cobrindo onboarding com as 3 pendências
- [x] Teste cobrindo onboarding com 1 pendência restante
- [x] Teste cobrindo onboarding ausente (tudo completo)
- [x] Teste cobrindo ação principal com visita ativa
- [x] Teste cobrindo ação principal sem visita ativa (convite genérico)
- [x] Teste cobrindo últimos treinos, incluindo um treino sem resultado
  nenhum (métrica ausente, sem erro)
- [x] Teste cobrindo resumo de modalidades com o melhor valor certo por
  modalidade
- [x] Teste cobrindo resumo de acervo
- [x] Testes já existentes de destaque dinâmico geral e fallback (mantidos)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0011 (orientação de melhor valor por tipo)
- ADR-0013 (Série — resultado armazenado como texto)
- ADR-0015 (dashboard pré-calculado via fila)
- UC06 (armas), UC12 (modalidades praticadas), UC31-35 (visita/treino)