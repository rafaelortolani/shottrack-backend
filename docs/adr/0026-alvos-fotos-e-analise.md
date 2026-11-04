# ADR-0026: Alvos — foto na série (etapa A) e direção da análise por IA (etapas B e C)

## Contexto
Hoje o alvo da série é texto livre (ADR-0013). O produto quer, em etapas: (A) guardar a foto do alvo,
(B) detectar os furos com confirmação do atleta e calcular grupamento/desvio, (C) pontuar automaticamente.
Este ADR **decide a etapa A** e **registra a direção** de B e C, que serão detalhadas num ADR próprio (0027)
depois de um protótipo com fotos reais.

## Decisão — Etapa A (foto anexada à série)
1. **Várias fotos por série**, no máximo **5** por série e **500** por atleta (configuráveis). O limite protege disco.
2. **Quando anexar**: a qualquer momento, mesmo com o treino encerrado (completar depois é legítimo, como no FUC14).
3. **Recebimento**: upload `multipart/form-data` (campo `file`), até **10 MB**. Tipos aceitos: JPEG, PNG, WebP
   (HEIC é convertido para JPEG no navegador antes do envio; o backend recusa o que não conhecer).
4. **Processamento obrigatório no servidor** (nunca guardar o arquivo original enviado):
   a) conferir o tipo pelo **conteúdo** (assinatura do arquivo), não pelo nome nem pelo Content-Type enviado;
   b) recusar imagem com mais de **40 megapixels** *antes* de decodificar (proteção contra bomba de descompressão e
      uso de memória);
   c) aplicar a orientação EXIF, **remover todos os metadados** (inclusive localização GPS) e **recodificar** como
      JPEG (qualidade ~85, lado maior até 2000 px); gerar também uma **miniatura** (lado maior 400 px).
   A recodificação elimina arquivos "poliglota" e metadados pessoais.
5. **Armazenamento** atrás de uma porta (`StoragePort`: guardar, ler, apagar), padrão Gateway do projeto.
   Implementação inicial: **disco local** (diretório configurável, volume dedicado em produção). Implementação S3
   (Object Storage) entra quando houver necessidade, sem mudar o restante. Chave do arquivo gerada pelo sistema
   (`athletes/{athleteId}/series/{seriesId}/{photoId}.jpg`), nunca derivada de texto do usuário.
6. **Acesso**: bucket/pasta nunca público. A foto só é entregue pelo backend autenticado
   (`GET .../content` e `.../thumbnail`), após conferir que a série é do atleta. Respostas com
   `Content-Type` fixo `image/jpeg`, `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-store`.
7. **Exclusão e LGPD**: excluir série, treino, visita ou conta apaga as fotos (linhas em cascata no banco;
   arquivos apagados **depois do commit**). Uma rotina semanal remove arquivos órfãos (sem linha no banco).
   Exportação de dados (UC53): o JSON inclui os **metadados** das fotos; o arquivo de cada foto é baixado pelo
   endpoint de conteúdo (a Política informa isso). Exportar um ZIP com as imagens fica como evolução.
8. **Limites de taxa**: upload limitado por atleta (ex.: 30 por hora) pela infraestrutura de rate limit existente.
9. **Sem análise nesta etapa**: a foto não é enviada a nenhum serviço externo de IA. O campo `alvo` (texto livre)
   continua existindo.
10. **Catálogo de tipos de alvo** (dimensões, anéis) **não entra na etapa A**: só é necessário quando houver
    análise (B) e depende de quais modalidades/alvos serão atendidos primeiro. Entra no ADR-0027.

## Implementação da etapa A (ajustes em relação ao texto acima)
- **WebP**: o JDK não traz decodificador de WebP e não adicionamos plugin; o backend aceita **JPEG e PNG** e responde
  `PHOTO_UNSUPPORTED_TYPE` (415) para WebP (e qualquer outra assinatura). O navegador já converte para JPEG antes de enviar.
- **Sem dependência nova**: orientação EXIF (leitura da tag 0x0112), redução e recodificação usam só `javax.imageio`/`java.awt`.
  JPEG grande é decodificado já reduzido (subamostragem), o que mantém o consumo de memória baixo mesmo perto de 40 MP.
- **Limite de corpo**: a API só aceita 64 KB por requisição (S10); a rota de upload tem teto próprio
  (`app.request.max-upload-body-bytes`, 11 MB). O Caddy (ADR-0025) deve aceitar 12 MB nessa rota.
- **Configuração**: `app.storage.local-dir` (`APP_STORAGE_LOCAL_DIR`; em prod é obrigatória, o diretório precisa existir e ser
  gravável, senão a aplicação não sobe), `app.photos.*` (limites, megapixels, concorrência, idade mínima de órfão e cron).

## Direção — Etapas B e C (a detalhar no ADR-0027, após o protótipo)
- **B (assistida)**: serviço de visão computacional separado (Python/OpenCV no protótipo), chamado pelo backend por
  fila (RabbitMQ). Devolve candidatos a furo (coordenadas). O **atleta confirma** na tela (adiciona/remove furos).
  Cálculos geométricos (centro do grupamento, dispersão, desvio) feitos por código determinístico; o texto de
  considerações (modelo de linguagem) usa só os números. Guardar correções do atleta para medir taxa de acerto.
- **C (pontuação)**: depende de B provada, do catálogo de alvos com geometria e da regra de toque na linha; sempre com
  revisão obrigatória.
- **Protótipo**: antes de construir B, testar com 10–20 fotos reais (guia em `guia-fotos-prototipo-B.md`).
- **Privacidade**: decidir antes de B se haverá envio de foto a provedor externo (transferência internacional,
  LGPD, Termos/Política) ou processamento só na infraestrutura própria.

## Alternativas consideradas
- Guardar a foto no banco (bytea): simples, mas incha backups e consultas. Rejeitada.
- URL pública temporária (assinada) direto do storage: exigiria Object Storage desde já e expõe link. Rejeitada por ora.
- Guardar o original além da versão processada: mais disco e metadados pessoais. Rejeitada.
- Uma foto por série: não cobre séries com mais de um alvo. Rejeitada.

## Consequências
- Novos UCs: UC60 (anexar), UC61 (consultar/listar), UC62 (remover); ajustes em UC41, UC43, UC44, UC52, UC53.
- Frontend: FUC26; ajuste no registro rápido de série (FUC14).
- Infra (ADR-0025): volume de uploads, limite de corpo no Caddy (12 MB), backup semanal do diretório de fotos
  (criptografado), aviso na Política de Privacidade.
- Memória: processamento de imagem consome memória do backend; mitigado pelo limite de megapixels e pela redução feita
  no navegador antes do envio.
- Nova dependência de imagem (ex.: Thumbnailator): passar pela verificação de dependências.

## Referências
ADR-0001, ADR-0006, ADR-0013, ADR-0019, ADR-0025.
