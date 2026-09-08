# UC08 - Excluir arma

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Arma pertence ao atleta
autenticado.

## Fluxo principal
1. Atleta solicita a exclusão de uma arma sua
2. Sistema verifica que a arma nunca foi usada em nenhum treino ou resultado
3. Sistema exclui a arma definitivamente

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Arma não existe ou pertence a outro atleta → erro `WEAPON_NOT_FOUND`
- 2a. Arma já foi usada em algum treino/resultado → erro `WEAPON_IN_USE`,
  exclusão bloqueada

## Observação
Os fluxos 2/2a dependem dos domínios de Treino e Resultado, que ainda não
existem no sistema. Até lá, nenhuma arma pode estar "em uso" — a checagem só
passa a ter efeito de verdade quando esses domínios forem implementados.

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)
- [x] Teste cobrindo o fluxo alternativo 1b (arma inexistente ou de outro atleta)
- [ ] Teste cobrindo o fluxo alternativo 2a (arma em uso) — bloqueado até os
      domínios de Treino/Resultado existirem (ver Observação acima)

## Referências
- ADR-0001 (autenticação JWT)
