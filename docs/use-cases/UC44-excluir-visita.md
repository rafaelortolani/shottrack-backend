# UC44 - Excluir visita

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Visita pertence ao atleta.

## Fluxo principal
1. Atleta solicita a exclusão de uma visita sua
2. Sistema exclui a visita definitivamente, junto com todos os treinos
   dela e as séries de cada um (cascata)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Visita não encontrada ou pertence a outro atleta → erro `VISIT_NOT_FOUND`

## Observação
Sem bloqueio por status — pode excluir mesmo uma visita `EM_ANDAMENTO`
(ex: corrigir um engano, como iniciar visita no local errado).

## Definição de pronto
- [ ] Teste cobrindo exclusão de visita sem treinos
- [ ] Teste cobrindo exclusão de visita com treinos e séries, confirmando
  que tudo some junto
- [ ] Teste cobrindo exclusão de visita `EM_ANDAMENTO`
- [ ] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0012 (Visita e Treino)
- ADR-0013 (Série)
- UC43 (excluir treino)