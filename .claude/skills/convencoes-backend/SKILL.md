---
name: convencoes-backend
description: Convenções de código do backend do ShotTrack — estrutura de camadas, DTOs, erros e resposta da API. Usar sempre que criar ou alterar controllers, services, repositories ou DTOs.
---

# Convenções de Backend — ShotTrack

Baseado no módulo `user`, implementado como referência (UC01 - cadastro de usuário).

## Estrutura de pacotes
Cada funcionalidade é um módulo dentro de `application`, um pacote por domínio (ex: `user`), com
subpacotes por camada dentro do módulo:
```
application/
└── user/
    ├── UserController.java
    ├── dto/
    │   ├── UserRegisterRequest.java
    │   └── UserResponse.java
    ├── model/
    │   └── User.java                (entidade JPA)
    ├── mapper/
    │   └── UserMapper.java          (model → DTO de saída)
    ├── usecase/
    │   └── UserService.java         (regra de negócio)
    └── gateway/
        ├── UserGateway.java         (port — só isso o usecase enxerga)
        ├── UserGatewayImpl.java     (adapter, package-private)
        └── repository/
            └── UserRepository.java  (Spring Data JPA, só usado pelo Impl)
```

## Camadas
- Controller: só recebe request (`@Valid @RequestBody`), chama o usecase, monta o `ResponseEntity`. Nunca tem regra de negócio.
- Usecase (ex: `UserService`): contém a regra de negócio, fala com `Gateway` (nunca com o `Repository` direto), e lança `BusinessException` quando algo viola uma regra.
- Mapper: converte `model` (entidade) para DTO de saída. Fica fora do usecase pra não misturar regra de negócio com serialização.
  Implementado com MapStruct (`@Mapper(componentModel = "spring")` numa interface, sem corpo — nunca escrever o mapeamento à mão).
  MapStruct gera a implementação (`<Nome>MapperImpl`) como `@Component`, injetada normalmente via `@RequiredArgsConstructor`.
- Gateway: a interface (`UserGateway`) é o único contrato que o usecase conhece — é o port. `UserGatewayImpl` é o adapter, `package-private` (não é acessado fora do pacote `gateway`), e é o único que enxerga o `Repository`.
- Repository: interface Spring Data JPA pura, sem lógica, vive dentro de `gateway/repository` e só é usada pelo `*GatewayImpl`.

## DTOs
- Records do Java. Nunca expor a entidade JPA direto na API.
- `<Entidade>RegisterRequest` / `<Entidade>Request` para entrada.
- `<Entidade>Response` para saída — conversão feita pelo `<Entidade>Mapper`, nunca por factory estática no DTO.
- Nunca incluir campos sensíveis (senha, hash) no Response.

## Erros
- Erro de negócio: lançar `BusinessException(code, httpStatus)` — código estável em MAIÚSCULAS_COM_UNDERSCORE.
- A mensagem nunca é passada na hora de lançar a exceção: o `code` dobra como chave de tradução em
  `messages.properties` (padrão, pt) / `messages_en.properties` (en). O `ApiExceptionHandler` resolve a
  mensagem certa via `MessageSource`, usando o locale da requisição (`Accept-Language`, default `pt`).
- Toda `BusinessException` nova precisa da entrada correspondente nos dois arquivos de mensagens —
  se faltar, `MessageSource` lança `NoSuchMessageException` e a resposta vira 500 (falha alto e cedo,
  de propósito).
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
- Campos obrigatórios setados só na criação (ex: `name`, `email`, `passwordHash`): declarar como `final` e usar `@RequiredArgsConstructor` — nunca declarar o construtor manualmente. O Lombok gera o construtor público só com esses campos.
- Campo gerado pelo banco (`id`) ou preenchido por auditoria (`createdAt`, `updatedAt`, etc. — ver seção Auditoria): não é `final`, fica de fora do `@RequiredArgsConstructor` automaticamente.
- Construtor sem argumentos do JPA em entidade com campos `final`: `@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)` — o Hibernate popula os campos `final` via reflection ao carregar do banco.
- DTOs continuam sendo records (não usam Lombok).

## Auditoria de entidades
- Toda entidade JPA estende `AbstractBaseEntity` (`common/jpa/AbstractBaseEntity.java`), que traz `createdAt`,
  `updatedAt`, `createdBy` e `updatedBy` via Spring Data JPA Auditing (`@CreatedDate`, `@LastModifiedDate`,
  `@CreatedBy`, `@LastModifiedBy` + `@EntityListeners(AuditingEntityListener.class)`).
- `@EnableJpaAuditing` fica em `config/JpaAuditingConfig.java`.
- `createdBy`/`updatedBy` ficam `null` até existir um bean `AuditorAware` — depende do login (UC02), que ainda
  não existe. Não é erro: Spring Data simplesmente não preenche esses campos sem um `AuditorAware` registrado.
- Nunca declarar `createdAt`/`updatedAt` na própria entidade — eles vêm da base.

## Banco de dados
- Toda mudança de schema é uma migration nova em `db/migration` (`V<numero>__descricao.sql`).
- Nunca alterar uma migration já aplicada.
- `ddl-auto: validate` — o Hibernate nunca gera schema sozinho.

## Segurança
- Endpoints públicos (sem login necessário) são liberados explicitamente em `SecurityConfig`.
- Senha sempre via `PasswordEncoder` (BCrypt) — nunca texto plano, nunca no log.

## Testes (obrigatório)
Toda funcionalidade nova é entregue com teste automatizado — sem exceção,
mesmo em protótipo. Nunca considerar um use case implementado sem isso.

- `@SpringBootTest` + `MockMvc`, cobrindo: o fluxo principal E cada um dos
  fluxos alternativos/erro listados no use case correspondente — não apenas
  um exemplo representativo.
- Como os testes rodam contra o Postgres real (`docker-compose.yml`), a classe
  de teste leva `@Transactional` pra isolar cada `@Test` (rollback automático).
