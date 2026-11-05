# ADR-0027: Planos e limites de uso (administráveis)

## Contexto
O ShotTrack terá planos gratuito e pago. O que cada plano permite (hoje: fotos de alvo; amanhã: análises de IA) precisa
ser ajustável pelo administrador sem alterar código nem reiniciar o backend. Na etapa A dos alvos (ADR-0026) os limites de
fotos ficaram em propriedades do backend (`app.photos.max-per-series`, `app.photos.max-per-athlete`) e o limite de envios por hora
fixo (30). Cobrança e pagamento não existem ainda.

## Decisão
1. **Planos administráveis**: tabela `plans`. A migration cria **um único plano ativo, "Gratuito" (`FREE`), que é o padrão**.
   O **administrador cria outros planos** (ex.: "Pago", "Beta"), edita o nome, ativa/desativa e escolhe qual é o padrão.
   - `code` é informado na criação (`^[A-Z][A-Z0-9_]{1,29}$`), **único e imutável**; `name` de 2 a 60 caracteres, editável.
   - Ao criar, é possível **copiar os limites de outro plano**; sem cópia, o plano nasce usando os valores padrão do enum.
   - **Planos nunca são excluídos** (mesmo raciocínio do ADR-0006): apenas desativados. Plano desativado não pode ser atribuído a
     atletas novos, mas **quem já o tem mantém o plano e os limites**.
   - Sempre existe **exatamente um plano padrão e ativo**: o padrão não pode ser desativado; para trocar, define-se outro plano
     ativo como padrão (o anterior deixa de ser, na mesma transação).
   - Máximo de 20 planos (evita lixo no catálogo).
2. **Limites por plano**: tabela `plan_limits` (plano, chave, valor). As **chaves são um enum no código** (com unidade, mínimo,
   máximo e valor padrão); o banco guarda só os valores. Chave sem valor no banco usa o padrão do enum.
3. **Chaves iniciais**:
   | Chave | Significado | Padrão (Gratuito) | Faixa permitida |
   |---|---|---|---|
   | `PHOTOS_PER_SERIES` | fotos por série | 5 | 0 a 20 |
   | `PHOTOS_PER_ATHLETE` | fotos no total por atleta | 500 | 0 a 100 000 |
   | `STORAGE_BYTES_PER_ATHLETE` | espaço total das fotos por atleta (soma do tamanho das imagens processadas; miniaturas não contam) | 536 870 912 (512 MB) | 0 a 107 374 182 400 (100 GB) |
   | `UPLOADS_PER_HOUR` | envios de foto por hora por atleta | 30 | 1 a 1 000 |
   | `AI_ANALYSES_PER_MONTH` | análises de IA por mês | **reservada, não aplicada** | definir no ADR da etapa B |
   Não existe valor "ilimitado": usa-se um número alto dentro da faixa.
4. **Atleta tem plano**: `users.plan_id` (obrigatório). Cadastros novos recebem o plano padrão; os atleta existentes são migrados para
   o Gratuito. O administrador **altera o plano do atleta manualmente** (sem cobrança por ora).
5. **Aplicação dos limites**: toda verificação consulta o plano do atleta (nunca mais propriedade). A checagem de limite é **atômica**
   com a gravação (sem condição de corrida entre dois envios simultâneos). Valores em cache em memória por até 60 s, invalidados
   quando o administrador altera. Os limites de fotos por série/atleta/espaço e o de envios por hora passam a ser dinâmicos.
6. **Reduzir limite nunca apaga dado**: quem já passou do novo limite mantém o que tem e só fica impedido de enviar mais.
   Vale também para quem mudar de plano.
7. **Administração** (ADR-0021, rotas `/api/admin/**`, só papel ADMIN): listar planos e limites; **criar plano**; **editar plano**
   (nome, ativo, padrão); editar limites de um plano (edição parcial, validada pela faixa); alterar o plano de um atleta;
   consultar o histórico de alterações.
8. **Auditoria**: toda criação/edição de plano, alteração de limite ou troca de plano de atleta registra quem fez, quando, chave, valor anterior e novo. Se já existir auditoria
   de ações de administrador (ADR-0021), reaproveitá-la; senão criar `plan_audit`. Na exclusão de conta (UC52), a referência
   ao usuário (ator ou alvo) nos registros de auditoria é **anonimizada** (fica nula); o registro permanece.
9. **Atleta vê seu plano e uso**: `GET /api/users/me/plan` devolve plano, limites efetivos e uso atual (fotos e espaço).
   O frontend usa isso para mostrar "12 de 500 fotos" e avisar ao chegar perto do limite.
10. **Propriedades `app.photos.*` deixam de existir** (o Gratuito é semeado com os mesmos valores). Revisa o item de limites do ADR-0026.

## Alternativas consideradas
- Limites globais soltos numa tabela de configuração: mais simples, mas teria de ser refeito quando houver planos. Rejeitada.
- Chaves de limite como linhas livres criadas pelo admin: permite erro de digitação e limite que o código ignora. Rejeitada
  (enum no código é a fonte das chaves).
- Valor "ilimitado" (-1): abre exceções em todo cálculo. Rejeitada.
- Excluir plano: rejeitado (atletas e auditoria referenciam o plano); só desativar.
- Cobrar/vender plano agora: fora do escopo.

## Consequências
- Nova migration (planos, limites, `users.plan_id`, auditoria se necessário); ajuste no UC60 (limites vêm do plano; novo erro
  `PHOTO_STORAGE_LIMIT_REACHED`), UC23 (plano padrão), UC52 (anonimizar auditoria) e UC53 (exportação traz o nome do plano).
- Novos UCs: UC63 a UC69 (UC68 criar plano, UC69 editar plano). Novos FUCs: FUC27 (admin) e FUC28 (atleta).
- Mudar o plano de um atleta tem efeito imediato (respeitando o cache de até 60 s).
- Quando a análise de IA existir (etapa B), `AI_ANALYSES_PER_MONTH` passa a ser aplicada, com ADR próprio.
- Dependência: o limite de envios por hora (rate limit) precisa aceitar valor por atleta em vez de constante.

## Referências
ADR-0006, ADR-0019, ADR-0021, ADR-0026.
