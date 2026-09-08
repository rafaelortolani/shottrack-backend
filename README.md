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

## Rodando os testes

```bash
./mvnw test
```
> Precisa do `docker compose up -d` rodando antes — os testes de integração
> usam o Postgres real, não um banco em memória.

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
