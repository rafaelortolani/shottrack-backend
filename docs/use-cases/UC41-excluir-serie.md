# UC41 - Excluir série

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Série pertence ao atleta.

## Fluxo principal
1. Atleta solicita a exclusão de uma série sua
2. Sistema exclui a série definitivamente, junto com seus resultados e as
   fotos do alvo (registros no banco; arquivos de imagem e miniatura apagados
   depois do commit, ADR-0026)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Série não encontrada ou pertence a outro atleta → erro `SERIES_NOT_FOUND`

## Observação
Diferente de Arma/Munição/Acessório/Local (ADR-0006), não há bloqueio de
exclusão aqui — nada no sistema ainda referencia uma série como "em uso"
por outra coisa. Se isso mudar no futuro (ex: competições referenciando
séries específicas), revisitar com um ADR próprio.

Se um arquivo de foto não puder ser apagado, a exclusão da série não falha:
a falha é registrada no log (sem dado pessoal) e a rotina semanal de limpeza
remove o arquivo órfão (ADR-0026).

## Definição de pronto
- [x] Teste cobrindo o fluxo principal, confirmando que os resultados
  somem junto
- [x] Um teste para CADA fluxo alternativo listado acima
- [x] Série com fotos: registros e arquivos (imagem e miniatura) somem junto (`SeriesPhotoCascadeTest`)
- [x] Falha ao apagar arquivo não impede a exclusão da série (`SeriesPhotoCascadeTest`)

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0013 (Série)
- ADR-0026 (fotos do alvo)
