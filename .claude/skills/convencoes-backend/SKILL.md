---
name: convencoes-backend
description: Convenções de código do backend do ShotTrack — estrutura de camadas, DTOs, erros e resposta da API. Usar sempre que criar ou alterar controllers, services, repositories ou DTOs.
---

# Convenções de Backend — ShotTrack

Baseado no módulo `user`, implementado como referência (UC01 - cadastro de usuário).

## Estrutura de pacotes
Cada funcionalidade é um pacote por domínio (ex: `user`), não por camada técnica:
```
user/
├── User.java              (entidade JPA)
├── UserRepository.java
├── UserService.java       (regra de negócio)
├── UserController.java
└── dto/
    ├── UserRegisterRequest.java
    └── UserResponse.java
```

## Camadas
- Controller: só recebe request (`@Valid @RequestBody`), chama o service, monta o `ResponseEntity`. Nunca tem regra de negócio.
- Service: contém a regra de negócio e lança `BusinessException` quando algo viola uma regra.
- Repository: interface Spring Data JPA pura, sem lógica.

## DTOs
- Records do Java. Nunca expor a entidade JPA direto na API.
- `<Entidade>RegisterRequest` / `<Entidade>Request` para entrada.
- `<Entidade>Response` para saída, com factory `from(entidade)`.
- Nunca incluir campos sensíveis (senha, hash) no Response.

## Erros
- Erro de negócio: lançar `BusinessException(code, message, httpStatus)` — código estável em MAIÚSCULAS_COM_UNDERSCORE.
- Validação de campo: usar Bean Validation (`@NotBlank`, `@Email`, etc.) no próprio DTO — o `ApiExceptionHandler` já traduz pra resposta padrão.
- Nunca deixar stack trace vazar pro cliente.

## Resposta da API
- Sucesso: `{ "data": {...} }`
- Erro: `{ "error": { "code": "...", "message": "..." } }`
- Sempre usando `ApiResponse<T>` (`common/web/ApiResponse.java`).

## Lombok
- Getters/setters são sempre gerados via Lombok (`@Getter`, `@Setter` quando necessário) — nunca declarar manualmente.
- Entidade JPA: `@Getter` na classe (setters só se a mutabilidade for realmente necessária).
- Construtor de injeção de dependência (campos `final` em `@Service`/`@RestController`/etc.): `@RequiredArgsConstructor` — nunca declarar o construtor manualmente.
- Construtor sem argumentos exigido pelo JPA: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- Um construtor manual só continua existindo quando tem lógica própria além de atribuir campos (ex: `User(name, email, passwordHash)` calcula `createdAt = Instant.now()`).
- DTOs continuam sendo records (não usam Lombok).

## Banco de dados
- Toda mudança de schema é uma migration nova em `db/migration` (`V<numero>__descricao.sql`).
- Nunca alterar uma migration já aplicada.
- `ddl-auto: validate` — o Hibernate nunca gera schema sozinho.

## Segurança
- Endpoints públicos (sem login necessário) são liberados explicitamente em `SecurityConfig`.
- Senha sempre via `PasswordEncoder` (BCrypt) — nunca texto plano, nunca no log.

## Testes
- Todo use case novo ganha um teste de integração com `@SpringBootTest` + `MockMvc`,
  cobrindo: caso de sucesso, e pelo menos uma regra de negócio violada.
- Como os testes rodam contra o Postgres real (`docker-compose.yml`), a classe de teste leva
  `@Transactional` pra isolar cada `@Test`: o Spring dá rollback ao final de cada método,
  então dados de uma execução nunca vazam pra outra.
