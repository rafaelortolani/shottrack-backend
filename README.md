# ShotTrack Backend

Backend do ShotTrack — aplicação para praticantes de tiro esportivo
acompanharem treinos, competições, resultados, armas, modalidades e
evolução de desempenho.

## Stack

- Java 21
- Spring Boot 3.3 (Web, Data JPA, Security, Validation)
- Maven
- PostgreSQL (schema versionado via Flyway)
- Mailpit (SMTP fake para desenvolvimento local)
- JWT (autenticação stateless, com refresh token)
- springdoc-openapi (documentação/Swagger UI)

## Arquitetura

Cada funcionalidade é organizada por domínio, com camadas internas:

```
application/
└── <dominio>/
    ├── <Dominio>Controller.java
    ├── dto/         # records de entrada/saída da API
    ├── model/       # entidade JPA
    ├── mapper/      # model → DTO (MapStruct)
    ├── usecase/     # regra de negócio
    └── gateway/     # port + adapter de persistência
```

As convenções completas (DTOs, erros, Lombok, testes) estão documentadas em
[`.claude/skills/convencoes-backend/SKILL.md`](.claude/skills/convencoes-backend/SKILL.md).

## Pré-requisitos

- JDK 21
- Docker e Docker Compose

## Rodando localmente

1. Suba as dependências (Postgres + Mailpit):
   ```bash
   docker compose up -d
   ```
2. Rode a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
3. A API sobe em `http://localhost:8080`. O Mailpit (visualização de emails
   enviados em dev) fica em `http://localhost:8025`.

Pra testar os endpoints protegidos, cadastre um usuário (`POST /api/users`),
faça login (`POST /api/auth/login`) e use o `accessToken` retornado — direto
pelo Swagger (`http://localhost:8080/swagger-ui/index.html`, botão
"Authorize") ou via curl (exemplos abaixo).

## Rodando os testes

Os testes de integração (`@SpringBootTest`) usam **Postgres e RabbitMQ reais**, mas num banco e num
vhost **dedicados** — nunca o banco de desenvolvimento (`shottrack`) nem o do E2E do front
(`shottrack_test`). Uma trava (`TestDatabaseGuard`) recusa subir qualquer teste apontando para eles.

1. Suba as dependências:
   ```bash
   docker compose up -d
   ```
2. Crie o banco `shottrack_unit_test` e o vhost `shottrack_unit_test` do RabbitMQ (uma vez; é idempotente):
   ```bash
   ./scripts/criar-ambiente-de-teste.sh
   ```
3. Rode a suíte (o `JWT_SECRET` é obrigatório porque o contexto Spring completo sobe):
   ```bash
   export JWT_SECRET=$(openssl rand -base64 32)
   ./mvnw test
   ```

O Flyway aplica as migrations no `shottrack_unit_test` na primeira execução (ele já nasce com a regra de
habitualidade da V52). Os testes de integração desfazem o que criam (`@Transactional`); um teste que precise
commitar de verdade (ex.: `AccountDeletionRollbackTest`) apaga os próprios dados no fim. Para usar outro banco/vhost,
defina `TEST_DB_URL` e `TEST_RABBIT_VHOST` — a trava continua valendo. Para zerar o banco de testes:
`docker compose exec postgres sh -c "PGUSER=shottrack psql -d postgres -c 'DROP DATABASE shottrack_unit_test'"` e rode o
script do passo 2 de novo.

## Endpoints disponíveis

| Método | Rota | Descrição | Use case |
|---|---|---|---|
| POST | `/api/users` | Cadastro de usuário | [UC01](docs/use-cases/UC01-cadastro-usuario.md) |
| POST | `/api/auth/login` | Login (retorna access + refresh token) | [UC02](docs/use-cases/UC02-login.md) |
| POST | `/api/auth/refresh` | Renova o access token (rotação do refresh token) | [UC02](docs/use-cases/UC02-login.md) |
| GET | `/api/users/me` | Consulta o perfil do usuário autenticado | [UC03](docs/use-cases/UC03-consultar-perfil.md) |
| PATCH | `/api/users/me` | Edita o nome do usuário autenticado | [UC04](docs/use-cases/UC04-editar-perfil.md) |
| POST | `/api/users/me/email` | Inicia a troca de email (envia código de verificação) | [UC04](docs/use-cases/UC04-editar-perfil.md) |
| POST | `/api/users/me/email/confirmation` | Confirma o código e efetiva a troca de email | [UC04](docs/use-cases/UC04-editar-perfil.md) |
| PATCH | `/api/users/me/password` | Altera a senha do usuário autenticado | [UC05](docs/use-cases/UC05-alterar-senha.md) |

A partir de `/swagger-ui/index.html` (app rodando localmente) dá pra explorar e
testar todos os endpoints, inclusive os protegidos por JWT (botão
"Authorize").

### Exemplo — cadastro

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Atleta Teste","email":"atleta@shottrack.com","password":"senha12345"}'
```

### Exemplo — login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"atleta@shottrack.com","password":"senha12345"}'
```

## Documentação do projeto

- [`CLAUDE.md`](CLAUDE.md) — contexto lido automaticamente pelo Claude Code
- [`docs/use-cases/`](docs/use-cases) — comportamento formalizado de cada funcionalidade
- [`docs/adr/`](docs/adr) — decisões técnicas e o porquê de cada uma
- [`.claude/skills/`](.claude/skills) — convenções de código aplicadas automaticamente

## Roadmap

- [x] UC01 — Cadastro de usuário
- [x] UC02 — Login (JWT + refresh token com rotação)
- [x] UC03 — Consultar perfil
- [x] UC04 — Editar perfil (incluindo troca de email verificada)
- [x] UC05 — Alterar senha
- [ ] Verificação de email no cadastro (adiado — [ADR-0002](docs/adr/0002-verificacao-email-adiada.md))
