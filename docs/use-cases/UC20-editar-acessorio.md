# UC20 - Editar acessório

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Acessório pertence ao
atleta autenticado. Se tipoId enviado, existe no catálogo (UC28).

## Fluxo principal
1. Atleta envia nome, tipoId e/ou observações a alterar (edição parcial —
   só os campos enviados mudam, mesmo padrão de Munição)
2. Sistema valida (nome não pode ficar vazio; tipoId, se enviado, precisa
   existir no catálogo) e atualiza os campos enviados

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED`
- 1b. Acessório não existe ou pertence a outro atleta → erro `ACCESSORY_NOT_FOUND`
- 2a. Nome resultaria vazio → erro de validação
- 2b. TipoId enviado não encontrado no catálogo → erro `ACCESSORY_TYPE_NOT_FOUND`

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0008 (acessório — tipo como catálogo fechado, revisão)
- UC28 (consultar catálogo de tipos de acessório)