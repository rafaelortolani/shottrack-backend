# UC60 - Anexar foto do alvo à série

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido. Série pertence ao atleta.

## Rota
`POST /api/series/{id}/photos` (`multipart/form-data`, campo `file`)

## Fluxo principal
1. Atleta envia a imagem
2. Sistema confere os limites do **plano do atleta** (ADR-0027), valida tipo pelo conteúdo, processa a imagem (orientação,
   remoção de metadados, recodificação JPEG, redução, miniatura) e guarda os arquivos
3. Sistema cria o registro da foto vinculado à série e devolve os metadados (`id`, `position`, `widthPx`,
   `heightPx`, `sizeBytes`, `createdAt`)

## Regras
- Limites vêm do plano do atleta (UC66/UC64), não de propriedades: fotos por série, fotos por atleta, espaço total por atleta
  e envios por hora. Padrão do plano Gratuito: 5, 500, 512 MB e 30.
- A verificação dos limites é **atômica** com a gravação: dois envios simultâneos não ultrapassam o limite.
- O espaço usado é a soma de `sizeBytes` das imagens processadas do atleta (miniaturas não contam). A checagem usa o tamanho
  da imagem **já processada**.
- Aceita **JPEG e PNG** (o JDK não decodifica WebP; o navegador converte para JPEG antes de enviar — ADR-0026), até 10 MB e
  40 megapixels. O original nunca é guardado. Um WebP enviado → `PHOTO_UNSUPPORTED_TYPE`.
- O arquivo guardado é um JPEG (qualidade 0,85, lado maior até 2000 px) sem metadados, com a orientação EXIF já aplicada, e
  uma miniatura (lado maior 400 px). JPEG truncado → `PHOTO_INVALID_IMAGE`.
- No máximo 2 imagens são processadas ao mesmo tempo (espera até 30 s; depois `503 PHOTO_PROCESSING_BUSY`).
- Permitido com o treino em andamento ou encerrado.
- Limite de envios por hora (do plano) por atleta → `429 TOO_MANY_REQUESTS` com `Retry-After` (chave: o usuário; tentativas recusadas também contam).
- Falha ao gravar o arquivo não deixa registro no banco; falha ao registrar não deixa arquivo órfão.
- Atleta acima do limite (após redução feita pelo administrador) não perde nada; só não envia novas fotos.

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`
- 1b. Série não encontrada ou de outro atleta → `SERIES_NOT_FOUND`
- 1c. Campo `file` ausente ou vazio → `VALIDATION_ERROR` (400)
- 2a. Arquivo acima de 10 MB → `PHOTO_TOO_LARGE` (413)
- 2b. Tipo não aceito (inclusive arquivo cujo conteúdo não confere com o tipo) → `PHOTO_UNSUPPORTED_TYPE` (415)
- 2c. Imagem corrompida ou ilegível → `PHOTO_INVALID_IMAGE`
- 2d. Mais de 40 megapixels → `PHOTO_DIMENSIONS_TOO_LARGE`
- 2e. Série já no limite do plano → `PHOTO_LIMIT_PER_SERIES_REACHED`
- 2f. Atleta no limite de fotos do plano → `PHOTO_LIMIT_PER_ATHLETE_REACHED`
- 2g. Espaço do plano esgotado → `PHOTO_STORAGE_LIMIT_REACHED`
- 2h. Limite de envios por hora → `TOO_MANY_REQUESTS`

## Definição de pronto
- [x] Teste do fluxo principal (JPEG e PNG; WebP → 415) e verificação de que o arquivo guardado é JPEG sem EXIF/GPS
- [x] Teste com foto girada por EXIF: resultado na orientação correta
- [x] Um teste para CADA fluxo alternativo; teste com arquivo `.jpg` que na verdade é outro formato; teste com
      imagem gigante (megapixels) recusada sem estourar memória
- [x] Teste de falha de gravação (sem registro) e de falha de banco (sem arquivo órfão)
- [x] Isolamento: atleta B não anexa em série do atleta A
- [x] Limites lidos do plano: alterar o limite pelo administrador muda o comportamento no envio seguinte
- [x] Teste de `PHOTO_STORAGE_LIMIT_REACHED` e de limite por hora dinâmico (valor do plano)
- [x] Teste de concorrência: envios simultâneos não ultrapassam o limite
- [x] Atleta acima do novo limite: nada é apagado e novo envio é recusado

## Referências
- ADR-0001, ADR-0013, ADR-0019, ADR-0026, ADR-0027
- UC36 (série), UC41 (excluir série), UC64, UC66
