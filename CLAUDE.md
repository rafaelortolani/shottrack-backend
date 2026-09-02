# ShotTrack Backend

App para atletas de tiro esportivo acompanharem treinos, competições, resultados,
armas, modalidades e evolução de desempenho.

## Stack
- Java 21, Spring Boot 3.3, Maven
- Postgres via Flyway (schema nunca gerado automaticamente pelo Hibernate)
- Sem front-end ainda — foco atual é o backend

## Como rodar
1. `docker compose up -d`
2. `./mvnw spring-boot:run`
3. API em `http://localhost:8080`

## Onde estão as coisas
- `docs/use-cases/` — cada funcionalidade formalizada como use case antes de implementar
- `docs/adr/` — decisões técnicas difíceis de reverter (criar só quando necessário)
- `.claude/skills/convencoes-backend/` — convenções de código, atualizada conforme o projeto evolui

## Regra de ouro
Não adicionar documentação especulativa. Documentação nasce do código que já existe,
não o contrário.
