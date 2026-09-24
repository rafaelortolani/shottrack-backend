# UC06 - Cadastrar arma

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Modelo existe no catálogo
(UC09) e o calibre escolhido está entre os válidos pra esse modelo
(ADR-0004, Revisão 2).

## Fluxo principal
1. Atleta escolhe a marca (consulta UC09)
2. Sistema retorna os modelos daquela marca — cada um já com seu tipo
3. Atleta escolhe o modelo
4. Sistema retorna os calibres válidos pra esse modelo
5. Atleta escolhe o calibre
6. Sistema cria a arma vinculada ao atleta, com tipo e marca **derivados
   do modelo escolhido** (não enviados pelo cliente), e retorna os dados

## Fluxos alternativos
- 1a. ModelId ou caliberId não informado → erro de validação
- 1b. Token ausente ou inválido → erro `UNAUTHORIZED`
- 2a. Modelo não encontrado → erro `WEAPON_MODEL_NOT_FOUND`
- 2b. Calibre não está entre os permitidos pra esse modelo → erro
  `WEAPON_CALIBER_NOT_ALLOWED_FOR_MODEL`

## Observação
O apelido da arma não é definido aqui — intencionalmente. O cadastro é
apenas modelo/calibre; apelido só existe a partir da edição (UC10).

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Um teste para CADA fluxo alternativo listado acima
- [x] Teste garantindo que o tipo/marca salvos batem com os do modelo
  escolhido, mesmo sem o cliente enviar esses campos

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0003 (não armazenar número de registro da arma)
- ADR-0004 (catálogo de armas, Revisão 2 — tipo e calibre amarrados ao modelo)