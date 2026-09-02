# UC01 - Cadastro de usuário

## Ator
Visitante (ainda não possui conta)

## Pré-condição
Nenhuma.

## Fluxo principal
1. Visitante informa nome, email e senha
2. Sistema valida os dados (nome obrigatório, email em formato válido,
   senha com no mínimo 8 caracteres)
3. Sistema verifica que o email ainda não está cadastrado
4. Sistema gera o hash da senha (BCrypt) — nunca armazena senha em texto plano
5. Sistema cria o usuário e retorna seus dados (sem a senha/hash)

## Fluxos alternativos
- 2a. Algum campo inválido → erro de validação, informando o campo específico
- 3a. Email já cadastrado → erro `EMAIL_ALREADY_REGISTERED`

## Referências
- Nenhuma (não depende de nenhuma decisão registrada em ADR)

## Definição de pronto
- [ ] Teste cobrindo o fluxo principal
- [ ] Um teste para CADA fluxo alternativo listado acima