# ShotTrack Backend

App para atletas de tiro esportivo acompanharem treinos, competições, resultados,
armas, modalidades e evolução de desempenho.

## Stack
- Java 21, Spring Boot 3.3, Maven
- Postgres via Flyway (schema nunca gerado automaticamente pelo Hibernate)
- Sem front-end ainda — foco atual é o backend

## Como rodar
1. `docker compose up -d`
2. `export JWT_SECRET=$(openssl rand -base64 32)` (obrigatório — também precisa
   estar setado pra rodar os testes, já que sobem o contexto Spring completo)
3. `./mvnw spring-boot:run`
4. API em `http://localhost:8080`

## Onde estão as coisas
- `docs/use-cases/` — cada funcionalidade formalizada como use case antes de implementar
- `docs/adr/` — decisões técnicas difíceis de reverter (criar só quando necessário)
- `.claude/skills/convencoes-backend/` — convenções de código, atualizada conforme o projeto evolui

## Regra de ouro
Não adicionar documentação especulativa. Documentação nasce do código que já existe,
não o contrário.

## Definição de pronto
Nenhuma funcionalidade é considerada concluída sem teste automatizado cobrindo
o caso de sucesso e TODOS os fluxos alternativos/erro descritos no use case.
Sem teste, a tarefa não está terminada — independente de "funcionar manualmente".