# ShotTrack Backend

## Rodando localmente

1. Suba o Postgres:
   ```
   docker compose up -d
   ```
2. Rode a aplicação:
   ```
   ./mvnw spring-boot:run
   ```
3. Teste o cadastro de usuário:
   ```
   curl -X POST http://localhost:8080/api/users \
     -H "Content-Type: application/json" \
     -d '{"name":"Atleta Teste","email":"atleta@shottrack.com","password":"senha12345"}'
   ```

## Rodando os testes
```
./mvnw test
```
(precisa do `docker compose up -d` rodando antes)

## Próximos passos sugeridos
- UC02: login (autenticação, geração de token)
- Restringir `SecurityConfig` conforme os endpoints forem exigindo login
# shottrack-backend
