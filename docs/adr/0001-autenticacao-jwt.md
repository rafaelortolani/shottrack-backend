# ADR-0001: Autenticação via JWT com refresh token

## Contexto
O sistema precisa autenticar o atleta para proteger endpoints pessoais (treinos,
resultados, armas). É a primeira decisão de segurança do projeto.

## Decisão
Autenticação stateless via JWT.
- Access token: expira em 1 hora, carrega o `id` do usuário (claim `sub`).
- Refresh token: expira em 7 dias, usado só pra gerar novo access token —
  nunca autentica um endpoint de negócio diretamente.
- Refresh token é persistido no banco (tabela própria), permitindo revogação — **só como hash SHA-256**
  (Lote 6, S06; a V55 apagou os tokens em texto puro e todos relogaram). A rotação é atômica (S20): a revogação
  é um UPDATE condicional (`WHERE revoked = false`) e só quem altera a linha segue; a chamada concorrente que
  perde é tratada como reuso e derruba as sessões.
- **Rotação a cada uso**: toda vez que um refresh token é trocado por um novo
  access token, o refresh token antigo é invalidado e um novo é emitido junto.
  Um refresh token já usado que aparece de novo é tratado como sinal de roubo —
  todos os tokens daquele usuário são revogados e ele precisa logar de novo.

## Alternativas consideradas
- Sessão tradicional (cookie + estado no servidor) → rejeitado: adiciona
  necessidade de sticky session ou store compartilhado se o backend escalar
  horizontalmente; JWT evita essa dependência desde já.
- Access token de vida longa sem refresh → rejeitado: token comprometido
  fica válido por muito tempo sem forma de revogar.
- Refresh token sem rotação → rejeitado: um refresh token vazado ficaria
  válido pelos 7 dias inteiros sem nenhum sinal de comprometimento.

## Consequências
- Endpoints protegidos exigem header `Authorization: Bearer <token>`.
- Tabela de refresh tokens precisa de coluna indicando se já foi usado/revogado.
- Precisa de rotina de limpeza dos refresh tokens expirados/revogados.
- `AuditorAware` (mencionado na skill de convenções) passa a poder ser
  implementado de verdade, usando o usuário autenticado no token.

## Pendente
Onde o cliente armazena os tokens (cookie httpOnly no navegador vs. keychain/
keystore no mobile) é decisão do front-end — fica em aberto até esse repositório existir.