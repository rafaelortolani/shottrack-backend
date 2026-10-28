## Fluxo principal
1. Atleta solicita seus próprios dados
2. Sistema retorna nome, email, data de cadastro e papel (`role`: `USER` ou `ADMIN`)
   (nunca senha/hash)

## Fluxos alternativos
- 1a. Token ausente ou inválido → erro `UNAUTHORIZED` (401)

## Regras
- O papel só é alterado diretamente no banco; nenhuma rota de atleta o modifica.
  O UC04 (editar perfil) ignora `role` se for enviado.
- Rotas `/api/admin/**` exigem `ADMIN`: sem token → `UNAUTHORIZED`;
  autenticado sem ser admin → `FORBIDDEN` (403). O papel é lido do banco a cada
  requisição admin (o JWT não carrega o papel): rebaixar um admin nega o acesso já na
  requisição seguinte. Admin não ganha acesso a dados de atletas (ADR-0019).

## Definição de pronto
- [x] Teste cobrindo o fluxo principal
- [x] Teste cobrindo o fluxo alternativo (1a)
- [x] Perfil de atleta comum retorna `role=USER`
- [x] Perfil de admin retorna `role=ADMIN`
- [x] Enviar `role` no UC04 não altera o papel
- [x] Rota admin: atleta comum → `FORBIDDEN`; admin → sucesso; sem token → `UNAUTHORIZED`
- [x] Admin rebaixado no banco perde o acesso na requisição seguinte

## Referências
- ADR-0001 (autenticação JWT)
- ADR-0021 (papel de administrador)