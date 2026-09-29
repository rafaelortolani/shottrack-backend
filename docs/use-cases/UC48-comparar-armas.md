    # UC48 - Comparar desempenho entre armas

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Modalidade é praticada
pelo atleta. Tipo de resultado está configurado no Perfil de Modalidade
dessa modalidade (UC30). Armas informadas pertencem ao atleta.

## Fluxo principal
1. Atleta informa modalidadeId, resultTypeId, uma lista de 2 ou mais
   armas (armaIds), e opcionalmente um período
2. Sistema valida modalidade, tipo de resultado e as armas informadas
3. Para cada arma, sistema calcula: quantidade de séries registradas com
   essa arma (nessa modalidade, no período), média do tipo de resultado,
   e melhor valor (respeitando a orientação do tipo, ADR-0011)
4. Sistema retorna os dados lado a lado, uma entrada por arma

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Menos de 2 armas informadas → erro de validação (comparação exige
  pelo menos 2 armas diferentes — a mesma arma repetida conta uma vez)
- 1c. Filtro de data com início depois do fim → erro de validação
- 2a. Modalidade não é praticada pelo atleta → erro `MODALITY_NOT_PRACTICED`
- 2b. Tipo de resultado não configurado pra essa modalidade → erro `RESULT_TYPE_NOT_CONFIGURED_FOR_TRAINING`
- 2c. Alguma arma informada não existe ou pertence a outro atleta → erro `WEAPON_NOT_FOUND`
- 3a. Arma sem nenhuma série registrada nessa modalidade/período → aparece
  na resposta com contagem zero e médias/melhores ausentes (não é erro,
  não trava a comparação das demais)

## Observação
Calculado sob demanda (mesmo raciocínio do ADR-0016) — combinação livre
de armas e tipo de resultado não compensa pré-cálculo.

## Definição de pronto
- [x] Teste cobrindo comparação entre 2 armas com dados
- [x] Teste cobrindo comparação entre 3+ armas
- [x] Teste cobrindo uma arma sem dado nenhum aparecendo com zero, sem
  quebrar a comparação
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0011 (orientação de melhor valor por tipo)
- ADR-0016 (mesmo raciocínio de cálculo sob demanda)
- UC30 (Perfil de Modalidade — pré-condição)