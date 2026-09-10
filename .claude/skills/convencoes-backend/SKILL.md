---
name: convencoes-backend
description: Convenções de código do backend do ShotTrack — estrutura de camadas, DTOs, erros e resposta da API. Usar sempre que criar ou alterar controllers, services, repositories ou DTOs.
---

# Convenções de Backend — ShotTrack

Baseado no módulo `user`, implementado como referência (UC01 - cadastro de usuário).

## Idioma
- Identificadores de código (classes, métodos, variáveis, campos) sempre em inglês —
  isso inclui nomes de método de teste. Nunca `deveRejeitarCodigoExpirado`, sempre
  `shouldRejectExpiredCode`.
- Convenção de nomes de teste: `should<comportamento esperado>` (ex:
  `shouldRegisterUserWithValidData`, `shouldRejectDuplicateEmail`,
  `shouldRejectRequestWithoutToken`).
- Português continua em: texto voltado ao usuário/API consumer (`messages.properties`,
  mensagens de `@NotBlank`/`@Email` etc.), comentários que explicam o *porquê* de uma
  decisão (referenciando UC/ADR), e toda a documentação (`docs/use-cases`, `docs/adr`).

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
- Construtor de injeção de dependência (campos `final` em `@Service`/`@RestController`/etc.): `@RequiredArgsConstructor` — nunca declarar o construtor manualmente. Isso vale só pra essas classes de infraestrutura, não pra entidades (ver abaixo).
- Construtor sem argumentos exigido pelo JPA: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- Construtor sem argumentos do JPA em entidade com campos `final`: `@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)` — o Hibernate popula os campos `final` via reflection ao carregar do banco.
- DTOs continuam sendo records (não usam Lombok).

### Instanciação de entidade JPA: sempre via `@Builder`
Regra única pra toda entidade, sem exceção por "não valer a pena" (nem catálogo
de campo único como `WeaponBrand`) — mais fácil de entender tendo só um jeito
de instanciar em vez de decidir caso a caso.

- Nunca `new Entidade(...)` fora da própria classe e nunca `@RequiredArgsConstructor` numa entidade — em vez disso, um construtor **`private`** anotado com `@Builder`, listando exatamente os campos que fazem sentido na criação:
  ```java
  @Builder
  private Weapon(UUID userId, @NonNull UUID typeId, @NonNull UUID brandId, @NonNull UUID modelId,
                 @NonNull UUID caliberId, String nickname) {
      this.userId = userId;
      this.typeId = typeId;
      this.brandId = brandId;
      this.modelId = modelId;
      this.caliberId = caliberId;
      this.nickname = nickname;
  }
  ```
  Uso: `Weapon.builder().userId(userId).typeId(typeId)....build()`.
- `id` nunca entra no builder (gerado pelo banco). Campo de transição de estado com
  método de domínio próprio também fica de fora (ex: `revoked` em `RefreshToken`, só
  muda via `revoke()`; `used` em `EmailVerificationCode`, só via `markUsed()`) — expor
  esses campos no builder permitiria criar um registro "pré-revogado"/"pré-usado" do
  nada, quebrando o invariante.
- Campo obrigatório: `final` (se nunca muda depois de criado) ou `@Setter` + `@NonNull`
  (se pode ser editado depois — ex: `Weapon.typeId` no UC10). De qualquer forma, é
  parâmetro do construtor `@Builder`; quando o campo é `@NonNull`, repetir `@NonNull`
  no parâmetro do construtor também, pra manter a validação em tempo de criação (o
  Lombok não propaga a anotação do campo pra um construtor escrito à mão).
- Campo opcional: `@Setter`, sem `@NonNull`, também é parâmetro do builder.
- Campo com valor padrão (ex: `User.experienceLevel = ExperienceLevel.BEGINNER`):
  mantém o inicializador no campo e o construtor só sobrescreve quando o parâmetro
  vier preenchido (`if (experienceLevel != null) { this.experienceLevel = experienceLevel; }`)
  — **não** usar `@Builder.Default` (nesse projeto, combinado com `@Builder` num
  construtor escrito à mão, ele gera uma chamada a um método `$default$campo()` que
  o Lombok não chega a criar, e a compilação quebra).
- Builder não substitui os setters existentes: edição de uma entidade já carregada do
  banco (ex: `WeaponService.update`) continua via setter — o builder só participa da
  criação.

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
- Nomes de método de teste em inglês, padrão `should<comportamento esperado>`
  (ver seção Idioma).
