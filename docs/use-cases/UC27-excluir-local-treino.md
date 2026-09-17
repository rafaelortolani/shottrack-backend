# UC27 - Excluir local de treino

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Local pertence ao atleta
autenticado.

## Fluxo principal
1. Atleta solicita a exclusão de um local seu
2. Sistema verifica que o local nunca foi usado em nenhuma visita
3. Sistema exclui o local definitivamente

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Local não existe ou pertence a outro atleta → erro `TRAINING_LOCATION_NOT_FOUND`
- 2a. Local já foi usado em alguma visita → erro `TRAINING_LOCATION_IN_USE`,
  exclusão bloqueada (sem opção de arquivar — ADR-0006)

## Observação
Assim como em UC08/UC16/UC21, o fluxo 2/2a depende do domínio de Visita,
que ainda não existe. Até lá, nenhum local pode estar "em uso".

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Teste cobrindo 1a (sem token) e 1b (local inexistente/de outro atleta)
- [ ] Teste cobrindo 2a (`TRAINING_LOCATION_IN_USE`) — não testável ainda: como
      o domínio de Visita não existe, não há como fazer um local chegar a
      esse estado (ver Observação acima); a checagem está estruturada
      (`TrainingLocationService.isUsedInAnyVisit`) pra virar teste real assim
      que Visita existir

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0006 (exclusão bloqueada, sem arquivamento)
- ADR-0010 (local de treino livre)