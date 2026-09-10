# UC16 - Excluir munição

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Munição pertence ao atleta
autenticado.

## Fluxo principal
1. Atleta solicita a exclusão de uma munição sua
2. Sistema verifica que a munição nunca foi usada em nenhuma série
3. Sistema exclui a munição definitivamente

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Munição não existe ou pertence a outro atleta → erro `AMMUNITION_NOT_FOUND`
- 2a. Munição já foi usada em alguma série → erro `AMMUNITION_IN_USE`,
  exclusão bloqueada (sem opção de arquivar — ver ADR-0006)

## Observação
Assim como no UC08 (excluir arma), o fluxo 2/2a depende do domínio de
Série, que ainda não existe. Até lá, nenhuma munição pode estar "em uso" —
a checagem só passa a ter efeito de verdade quando esse domínio for
implementado.

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Teste cobrindo 1a (sem token) e 1b (munição inexistente/de outro atleta)
- [ ] Teste cobrindo 2a (`AMMUNITION_IN_USE`) — não testável ainda: como o
      domínio de Série não existe, não há como fazer uma munição chegar a
      esse estado (ver Observação acima); a checagem está estruturada
      (`AmmunitionService.isUsedInAnySeries`) pra virar teste real assim que
      Série existir

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0006 (exclusão bloqueada, sem arquivamento)