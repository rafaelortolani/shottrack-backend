# UC47 - Consultar histórico de séries

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita o histórico de séries, opcionalmente com filtros:
   período (data início/fim), modalidadeId, armaId, distância (mín/máx
   em metros)
2. Sistema retorna as séries que atendem aos filtros informados
   (nenhum filtro informado = retorna tudo), cada uma com o contexto
   completo: data, local (via visita), modalidade (via treino), arma,
   munição, distância, alvo, quantidade de disparos, e os resultados
   registrados
3. Resultado ordenado do mais recente pro mais antigo

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Nenhuma série atende aos filtros → retorna lista vazia (não é erro)
- 1c. Filtro de data com início depois do fim → erro de validação
- 1d. ArmaId informado não existe ou pertence a outro atleta → erro `WEAPON_NOT_FOUND`
- 1e. ModalidadeId informado não é praticada pelo atleta → erro `MODALITY_NOT_PRACTICED`

## Observação
Calculado sob demanda (mesmo raciocínio do ADR-0016) — filtros
combináveis livremente não compensam pré-cálculo, e o volume de série
por atleta é pequeno o bastante pra isso não pesar.

## Definição de pronto
- [x] Teste cobrindo sem filtro nenhum (retorna tudo)
- [x] Teste cobrindo cada filtro isoladamente (período, modalidade, arma,
  distância)
- [x] Teste cobrindo combinação de 2+ filtros ao mesmo tempo
- [x] Teste cobrindo nenhum resultado (lista vazia)
- [x] Um teste para CADA fluxo alternativo listado acima
- [x] Teste garantindo isolamento entre atletas (nunca retorna série de
  outro atleta)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0013 (Série)
- ADR-0016 (mesmo raciocínio de cálculo sob demanda)