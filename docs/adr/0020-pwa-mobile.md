# ADR-0020: Celular via PWA instalável (sem app nativo por ora)

## Contexto
O atleta usa o ShotTrack no estande, pelo celular. O frontend já é responsivo
(Next.js, BFF com cookies httpOnly). Precisamos que ele seja instalável na
tela inicial, abra em tela cheia, tenha usabilidade boa no celular e, no
futuro, tire foto do alvo para análise por IA — sem manter um segundo código
(app nativo).

## Decisão
1. **PWA instalável**: manifest (`app/manifest.ts`), ícones (192, 512, maskable
   e apple-touch-icon) gerados a partir do LogoIcon, `display: standalone`,
   cores de tema do design (#232323), atalhos para Treinos e Dashboard.
2. **Service worker mínimo, escrito à mão** (sem biblioteca de PWA):
   - cache de arquivos estáticos (cache-first);
   - navegação em rede com página de **fallback offline** genérica;
   - **nunca** guarda respostas da API/BFF nem páginas autenticadas (dados
     pessoais não ficam em cache do dispositivo — alinhado ao ADR-0019 — e
     não há risco de mostrar dado velho);
   - registrado apenas em produção.
3. **Sem escrita offline**: sem conexão o app avisa e não registra. Registro
   offline com fila e sincronização fica fora do escopo e só será considerado
   se o uso real no estande mostrar essa necessidade (ou levar a um app nativo).
4. **Convite de instalação**: Android/Chrome usa o prompt nativo (botão
   "Instalar app"); iPhone mostra instruções ("Compartilhar > Adicionar à Tela
   de Início"), pois o iOS não tem prompt. Cartão dispensável em Boas-vindas e
   seção "Aplicativo" em Usuário > Perfil.
5. **Revisão de usabilidade mobile** dos fluxos principais (login, boas-vindas,
   treinos/visitas e registro de série, dashboard, acervo) em 390x844 e
   360x640: alvos de toque ≥ 44px, safe areas (notch), `100dvh`, campos sem
   zoom automático no iOS, ações principais ao alcance do polegar.
6. **Câmera**: fora deste ADR. Quando o alvo com IA for desenhado, a foto virá
   do app de câmera do celular via campo de arquivo com captura, que funciona
   no PWA.

## Alternativas consideradas
- App nativo (React Native/Expo) → adiado: segundo produto para manter, lojas,
  custo contínuo; só se aparecer necessidade que o PWA não atenda (offline-first,
  câmera avançada).
- Biblioteca de PWA (next-pwa/serwist) → rejeitada por ora: o escopo do worker
  é pequeno e a escrita à mão evita acoplamento com o bundler.
- Cache offline de dados (listas, séries) → rejeitado: risco de dado velho e de
  dado pessoal em cache.

## Consequências
- Requer HTTPS em produção (exigência do service worker); em desenvolvimento
  localhost funciona. Teste em aparelho real exige HTTPS (túnel ou ambiente de
  beta).
- No iPhone o PWA instalado tem armazenamento separado do Safari: o atleta
  entra de novo no app instalado (esperado).
- Mudanças só no frontend; a skill `convencoes-frontend` ganha seção
  "PWA e mobile".

## Referências
- ADR-0019 (dados pessoais), ADR-0017 (Sentry)
