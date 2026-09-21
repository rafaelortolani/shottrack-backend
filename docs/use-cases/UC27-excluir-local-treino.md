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

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Teste cobrindo 1a (sem token) e 1b (local inexistente/de outro atleta)
- [x] Teste cobrindo 2a (`TRAINING_LOCATION_IN_USE`)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0006 (exclusão bloqueada, sem arquivamento)
- ADR-0010 (local de treino livre)
- ADR-0012 (Visita — domínio que passou a dar uso real ao local de treino)