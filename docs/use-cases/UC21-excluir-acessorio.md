# UC21 - Excluir acessório

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Acessório pertence ao
atleta autenticado.

## Fluxo principal
1. Atleta solicita a exclusão de um acessório seu
2. Sistema verifica que o acessório nunca foi usado em nenhuma série
3. Sistema exclui o acessório definitivamente, junto com suas associações
   a armas (se houver — associação não é "uso", ver ADR-0006)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Acessório não existe ou pertence a outro atleta → erro `ACCESSORY_NOT_FOUND`
- 2a. Acessório já foi usado em alguma série → erro `ACCESSORY_IN_USE`,
  exclusão bloqueada

## Observação
Como no UC08 e UC16, o fluxo 2/2a depende do domínio de Série, que ainda
não existe. Até lá, nenhum acessório pode estar "em uso".

## Definição de pronto
- [x] Teste cobrindo o fluxo principal, incluindo remoção das associações
- [x] Teste cobrindo 1a (sem token) e 1b (acessório inexistente/de outro atleta)
- [ ] Teste cobrindo 2a (`ACCESSORY_IN_USE`) — não testável ainda: como no
      UC16/UC08, o domínio de Série não existe, então não há como fazer um
      acessório chegar a esse estado; a checagem está estruturada
      (`AccessoryService.isUsedInAnySeries`) pra virar teste real assim que
      Série existir

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0006 (exclusão bloqueada, sem arquivamento)
- ADR-0008 (acessório — associação N:N)