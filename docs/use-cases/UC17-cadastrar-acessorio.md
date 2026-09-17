# UC17 - Cadastrar acessório

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Se informado, tipoId
existe no catálogo (UC28).

## Fluxo principal
1. Atleta informa nome (obrigatório), tipoId (obrigatório, catálogo
   fechado) e observações (opcional)
2. Sistema valida que o tipoId existe no catálogo
3. Sistema cria o acessório vinculado ao atleta autenticado e retorna seus dados

## Fluxos alternativos
- 1a. Nome vazio → erro de validação
- 1b. TipoId não informado → erro de validação
- 1c. Token ausente ou inválido → erro `UNAUTHORIZED`
- 2a. TipoId não encontrado no catálogo → erro `ACCESSORY_TYPE_NOT_FOUND`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0008 (acessório — tipo como catálogo fechado, revisão)
- UC28 (consultar catálogo de tipos de acessório)