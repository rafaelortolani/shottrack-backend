# ADR-0021: Papel de administrador

## Contexto
Algumas informações do ShotTrack deixam de ser fixas e passam a ser cadastradas
por quem administra o produto (primeiro caso: regras de habitualidade, depois
talvez aprovação de armas não listadas no catálogo). Hoje o sistema só distingue
"autenticado" de "não autenticado": não existe papel, rota de administração nem
erro de permissão (`FORBIDDEN`).

## Decisão
1. `users.role` com valores `USER` (padrão) e `ADMIN`. A promoção é feita direto no
   banco (`UPDATE users SET role = 'ADMIN' WHERE email = ...`); nenhum endpoint de
   atleta altera o papel e não existe tela de promoção.
2. Rotas de administração ficam em `/api/admin/**` e exigem `ADMIN`.
   Sem token → `UNAUTHORIZED` (401). Autenticado sem ser admin → novo erro
   `FORBIDDEN` (403, mensagem pt/en).
3. O papel é **lido do banco a cada requisição administrativa** (não só do token),
   para que promoção e rebaixamento valham na hora e um admin rebaixado perca o
   acesso sem esperar o token expirar. O token continua sem carregar o papel.
4. `GET /api/users/me` passa a devolver `role`, para o frontend decidir o que exibir.
   Isso é só conveniência de interface: a autorização real é sempre do backend.
5. Frontend: o item "Admin" e as páginas `/admin/**` só para `ADMIN`; o proxy
   redireciona quem não é admin e o BFF das rotas admin também confere.
6. Toda tabela editada por administradores usa as colunas de auditoria que o
   projeto já tem (quem criou e quem alterou).

## Alternativas consideradas
- Papel dentro do JWT → rejeitado: promoção/rebaixamento só valeria no próximo
  refresh (até 1 hora).
- Tela para promover usuários → adiada: há um único administrador (o dono do
  produto) e a promoção por SQL é suficiente e mais segura.
- Sistema de permissões granulares (RBAC completo) → rejeitado por ora: dois
  papéis bastam.

## Consequências
- Nova migration em `users` e nova exceção `FORBIDDEN` no `ApiExceptionHandler`.
- Todas as rotas `/api/admin/**` futuras herdam a proteção.
- Dados pessoais: administrador não ganha acesso a dados de atletas; ele só edita
  catálogos e regras. Qualquer rota admin que exponha dado de atleta exige novo ADR.

## Referências
- ADR-0001 (JWT), ADR-0018 (rate limit), ADR-0019 (LGPD)
