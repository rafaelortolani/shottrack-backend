# UC17 - Cadastrar acessório

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Fluxo principal
1. Atleta informa nome (obrigatório), tipo (opcional, texto livre) e
   observações (opcional)
2. Sistema cria o acessório vinculado ao atleta autenticado e retorna seus dados

## Fluxos alternativos
- 1a. Nome vazio → erro de validação
- 1b. Token ausente ou inválido → erro `UNAUTHORIZED`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0008 (acessório — cadastro livre, associação N:N)