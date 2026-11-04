# UC61 - Consultar fotos da série

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Série pertence ao atleta.

## Rotas
- `GET /api/series/{id}/photos` — lista os metadados das fotos (ordem de `position`)
- `GET /api/series/{id}/photos/{photoId}/content` — imagem processada (JPEG)
- `GET /api/series/{id}/photos/{photoId}/thumbnail` — miniatura (JPEG)

## Fluxo principal
1. Atleta solicita a lista, a imagem ou a miniatura
2. Sistema confere que a série e a foto são do atleta e devolve o dado

## Regras
- Respostas de imagem com `Content-Type: image/jpeg`, `X-Content-Type-Options: nosniff` e
  `Cache-Control: private, no-store`.
- A listagem de séries (UC37) passa a informar `photoCount` por série, sem carregar imagens.
- Nunca existe URL pública; sempre passa pela autenticação.

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 1b. Série não encontrada ou de outro atleta → `SERIES_NOT_FOUND`
- 1c. Foto não encontrada nessa série ou de outro atleta → `PHOTO_NOT_FOUND`
- 1d. Registro existe mas o arquivo sumiu → `PHOTO_NOT_FOUND` e registro de erro no log (sem dado pessoal)

## Definição de pronto
- [x] Lista, imagem e miniatura (inclusive série sem fotos → lista vazia)
- [x] Um teste para CADA fluxo alternativo
- [x] Cabeçalhos de resposta conferidos em teste
- [x] Isolamento: atleta B não vê lista, imagem nem miniatura do atleta A; `photoId` de outra série não funciona
- [x] `photoCount` correto no UC37

## Testes
`SeriesPhotoTest`, `SeriesPhotoIsolationTest`.

## Referências
- ADR-0001, ADR-0026
- UC37 (listar séries), UC60
