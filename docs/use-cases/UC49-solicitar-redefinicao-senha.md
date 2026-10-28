# UC49 - Solicitar redefinição de senha

## Ator
Atleta não autenticado.

## Fluxo principal
1. Atleta informa o e-mail da conta
2. Sistema responde sucesso (mesma resposta exista a conta ou não)
3. Se a conta existe: invalida qualquer token de redefinição anterior ainda
   válido, gera um novo (1 hora, guardado como hash) e envia e-mail com o
   link `{frontend-url}/redefinir-senha?token=...` em linha própria

## Fluxos alternativos
- 1a. E-mail em formato inválido → erro de validação (`VALIDATION_ERROR`)
- 2a. E-mail não cadastrado → mesma resposta de sucesso, nada é enviado
- 2b. Limite de pedidos excedido → `TOO_MANY_REQUESTS` (429, `Retry-After`)

## Observações
- Resposta: `202 Accepted` com `{"data": null}`, igual para conta existente ou não.
- Um novo pedido apaga os tokens ainda não usados do usuário (o link antigo
  passa a responder `PASSWORD_RESET_TOKEN_INVALID`).
- O envio do e-mail é síncrono: uma conta existente leva um pouco mais para
  responder (tempo do SMTP). Aceito por ora — o rate limit (5/hora por e-mail,
  20/hora por IP) limita a exploração. Falha no SMTP é só logada, sem mudar a
  resposta, para não revelar que a conta existe.
- E-mail em pt/en conforme `Accept-Language`.

## Definição de pronto
- [x] Teste do fluxo principal (e-mail enviado, token guardado como hash)
- [x] Teste: e-mail não cadastrado devolve a mesma resposta e não envia e-mail
- [x] Teste: novo pedido invalida o token anterior
- [x] Teste: e-mail inválido (formato e ausente)
- [x] Teste: rate limit (429 com Retry-After)

## Referências
- ADR-0018, ADR-0009
