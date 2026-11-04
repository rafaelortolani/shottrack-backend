# UC62 - Remover foto da série

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Série e foto pertencem ao atleta.

## Rota
`DELETE /api/series/{id}/photos/{photoId}`

## Fluxo principal
1. Atleta solicita a remoção da foto
2. Sistema remove o registro e, após o commit, apaga a imagem e a miniatura do armazenamento
3. Sistema responde sem conteúdo (204)

## Regras
- Remoção definitiva; sem lixeira.
- Se o arquivo não puder ser apagado, o registro já foi removido; o arquivo órfão é limpo pela rotina semanal e a
  falha é registrada no log (sem dado pessoal).

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 1b. Série não encontrada ou de outro atleta → `SERIES_NOT_FOUND`
- 1c. Foto não encontrada nessa série → `PHOTO_NOT_FOUND`

## Definição de pronto
- [x] Remover e conferir que registro e arquivos sumiram
- [x] Um teste para CADA fluxo alternativo
- [x] Falha ao apagar arquivo não faz a requisição falhar
- [x] Isolamento: atleta B não remove foto do atleta A
- [x] `photoCount` atualizado

## Testes
`SeriesPhotoTest`, `SeriesPhotoIsolationTest`, `SeriesPhotoConsistencyTest`; arquivos apagados após o commit em
`SeriesPhotoCascadeTest`.

## Referências
- ADR-0001, ADR-0006, ADR-0026
- UC60, UC61
