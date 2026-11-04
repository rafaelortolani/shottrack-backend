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

### Subir uma instância (não a suíte) para testar à mão

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `RABBITMQ_*`, `SMTP_*` e `FRONTEND_URL` **só valem no perfil `prod`**
(`application-prod.yml`). No perfil padrão a configuração aponta para o banco de desenvolvimento e essas variáveis são
ignoradas em silêncio — uma instância "de teste" subida com elas grava no banco de dev. Para uma instância de teste use os
nomes do Spring e marque-a com `APP_TEST_INSTANCE=true`:

```bash
APP_TEST_INSTANCE=true SERVER_PORT=8098 \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/shottrack_unit_test \
SPRING_RABBITMQ_VIRTUAL_HOST=shottrack_unit_test \
SPRING_MAIL_PORT=1025 APP_FRONTEND_URL=http://localhost:3000 \
java -jar target/backend-0.1.0.jar
```

Com `APP_TEST_INSTANCE=true` o app **recusa subir** (`TestInstanceGuard`) se o banco for `shottrack` ou `shottrack_test`
ou se o vhost do RabbitMQ for o padrão `/`. Sem a variável a trava não age.

## Fotos do alvo (ADR-0026)

As fotos enviadas ficam em disco, atrás de `StoragePort`. Em desenvolvimento o diretório é `./data/uploads` (ignorado pelo
git); nos testes, um diretório temporário. **Em produção** defina `APP_STORAGE_LOCAL_DIR` (volume dedicado): o diretório
precisa existir e ser gravável pelo usuário do contêiner — senão a aplicação não sobe. Limites ajustáveis em `app.photos.*`
(5 por série, 500 por atleta, 10 MB, 40 megapixels). A rotina semanal de arquivos órfãos roda com `app.cleanup.enabled=true`.

## Verificação de vulnerabilidades (CVEs)

- **Dependabot** (`.github/dependabot.yml`): abre PRs semanais com atualizações do Maven e das imagens do `docker-compose`
  (inclusive as de segurança), com as de patch/minor do Spring agrupadas em um PR só.
- **OWASP Dependency-Check**, num profile fora do build normal. Peça uma chave gratuita da NVD em
  https://nvd.nist.gov/developers/request-an-api-key e passe-a só pela variável de ambiente (nunca no repositório):
  ```bash
  export NVD_API_KEY=<sua chave>
  ./mvnw -Pdependency-check verify -DskipTests
  ```
  Sem a chave, o download da base da NVD fica muito lento e costuma falhar por limite de requisições. O build falha se
  achar CVE com CVSS >= 7; o relatório fica em `target/dependency-check-report.html` (e `.json`).

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
