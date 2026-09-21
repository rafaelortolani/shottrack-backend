# UC35 - Listar visitas

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta solicita a lista de suas visitas
2. Sistema retorna todas as visitas do atleta, cada uma com seus treinos
   aninhados (id, modalidade, status, horários)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Atleta sem nenhuma visita registrada → retorna lista vazia (não é erro)

## Definição de pronto
- [x] Teste cobrindo listagem com visitas e treinos aninhados
- [x] Teste cobrindo lista vazia
- [x] Teste cobrindo o fluxo alternativo 1a (sem token)
- [x] Teste garantindo que a listagem nunca inclui visita de outro atleta

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0012 (Visita e Treino)