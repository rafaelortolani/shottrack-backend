# ADR-0019: LGPD — direitos do titular, consentimento e retenção de dados

## Contexto
Antes de abrir o ShotTrack para usuários reais, o sistema precisa atender o
básico da LGPD: o atleta deve poder **ver/levar** seus dados, **excluir** a
conta, e ter dado **ciência e aceite** dos Termos e da Política de
Privacidade. Os dados que guardamos são: e-mail, nome, senha (hash), nível de
experiência, armas (tipo/marca/modelo/calibre/apelido — sem número de série
ou registro, ADR-0003), munições, acessórios, locais de treino, visitas,
treinos, séries e resultados.

## Decisão

### 1. Excluir conta (UC52)
- Confirmação com a **senha atual**. Exclusão **imediata e definitiva** (sem
  carência, sem soft delete).
- Apaga TODOS os dados do atleta, em código, na ordem das dependências e numa
  única transação (sem depender de `ON DELETE CASCADE`): resumo do dashboard,
  resultados, séries, treinos, visitas, locais, associações acessório-arma,
  acessórios, munições, armas, modalidades praticadas e suas seleções de
  tipo de resultado, tokens (refresh, redefinição, verificação de e-mail),
  cadastros pendentes do mesmo e-mail e, por fim, o usuário.
- A regra de bloqueio de exclusão de itens usados (ADR-0006) não se aplica,
  pois é a conta inteira que sai.
- Revoga todas as sessões e envia e-mail avisando da exclusão (falha no envio
  não desfaz a exclusão).
- Backups do banco gerenciado podem reter dados por um período; isso consta na
  Política de Privacidade.

### 2. Exportar dados (UC53)
- `POST /api/users/me/export` `{ "currentPassword": "..." }` devolve um arquivo JSON com tudo o
  que o sistema guarda sobre o atleta (sem hash de senha e sem tokens). Síncrono (volume por
  atleta é pequeno), com rate limit. **Exige a senha atual**, como a exclusão (UC52): só o token
  de acesso (60 min) não basta para levar embora todos os dados pessoais. A rota `GET` antiga
  responde `405 DATA_EXPORT_REQUIRES_PASSWORD` por uma versão.
- **Troca de e-mail (UC04):** também exige a senha atual; o e-mail antigo é avisado do pedido e da
  troca (sem o código), e a confirmação revoga todas as sessões. A unicidade do e-mail é
  revalidada no cadastro (UC23) e na confirmação, e a violação no banco vira `EMAIL_ALREADY_REGISTERED`.

### 3. Consentimento (alteração no UC23)
- Completar o cadastro exige aceitar Termos de Uso e Política de Privacidade.
- Guardamos `terms_accepted_at` e `terms_version` no usuário. A versão vigente
  vem de configuração (`app.legal.version`). Erro: `TERMS_NOT_ACCEPTED`.
- Páginas públicas `/termos` e `/privacidade` no frontend, com minuta
  fornecida em `docs/legal/` (precisa de revisão jurídica antes de produção).

### 4. Boas práticas
- **Logs e Sentry sem dados pessoais:** nada de e-mail, senha, tokens ou corpo
  de requisição; Sentry com `sendDefaultPii=false`. Revisar os dois lados.
- **Limpeza agendada** (diária) de dados expirados: cadastros pendentes,
  tokens de redefinição, refresh tokens e códigos de verificação vencidos.
- **Minimização:** não coletar CPF, endereço, telefone ou nº de série/registro
  de arma.

- **Mesma máscara em todos os destinos de log (Lote 4, S09):** o console aplica a
  mesma máscara de e-mail e JWT do arquivo JSON (`logback-spring.xml`), inclusive no
  stack trace — mensagens de exceção de terceiros (ex: `Key (email)=(...)` do Postgres)
  não saem em claro.
- **DLQ sem dado pessoal (Lote 4, S17):** o recoverer da fila de recálculo
  (ADR-0015) só leva a classe da exceção e o stack trace pra DLQ — nunca a
  mensagem da exceção nem das causas. As mensagens da DLQ expiram sozinhas
  (`app.rabbit.dlq-ttl-days`, padrão 7), então as de um usuário excluído somem
  no máximo nesse prazo; pra descartar antes, ver o ADR-0015.

### 5. Pendências de infraestrutura antes de produção (Lote 5 da auditoria)
Nada abaixo é código do backend; precisa estar resolvido (ou aceito conscientemente) antes de dados reais:
- **Backup, retenção e expurgo:** definir política de backup do Postgres (frequência, onde, por quanto tempo) e
  como a exclusão de conta (UC52) chega aos backups — o dado apagado continua nos backups até eles expirarem;
  documentar esse prazo na Política de Privacidade ou criptografar/rotacionar de modo que a retenção seja curta.
- **Criptografia em repouso:** disco/volume do Postgres, dos backups e do RabbitMQ (a DLQ guarda o id do atleta).
- **TLS e HSTS:** terminar TLS no proxy (backend e frontend), `Strict-Transport-Security`, e SMTP com STARTTLS
  (o perfil `prod` já exige); Postgres e RabbitMQ só em rede privada.
- **Rotação do `JWT_SECRET`:** hoje trocar a chave invalida todos os access tokens e não há suporte a duas chaves
  ao mesmo tempo; definir a rotina (janela de manutenção ou suporte a chave anterior) e quem guarda o segredo.

## Alternativas consideradas
- Carência de 7 dias para desfazer a exclusão → rejeitado por ora: mais
  complexidade (estado "em exclusão", job, e-mail de reversão) sem demanda real.
- Anonimizar em vez de apagar → rejeitado: os dados de treino são pessoais e
  não há finalidade estatística que justifique retê-los.
- Exportação assíncrona por e-mail → rejeitada: volume pequeno.

## Consequências
- Novas colunas em `users` (`terms_accepted_at`, `terms_version`); usuários
  anteriores ficam com nulo (só existem contas de teste hoje).
- Novos erros: `TERMS_NOT_ACCEPTED`. Reusa `INVALID_CURRENT_PASSWORD`.
- Frontend: aba Privacidade em Usuário, checkbox de aceite no completar
  cadastro, páginas `/termos` e `/privacidade`.
- Texto jurídico definitivo e contato do encarregado são pendências de
  negócio (não de código).

## Referências
- ADR-0003, ADR-0006, ADR-0009, ADR-0017, ADR-0018
