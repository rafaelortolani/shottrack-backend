# ADR-0004: Catálogo de tipo, marca, modelo e calibre via seed fixo

## Contexto
UC06 (cadastro de arma) precisa que o atleta escolha tipo, marca, modelo e
calibre de forma consistente — texto livre permitiria duplicidade e erro de
digitação (ex: "Taurus", "taurus", "TAURUS" tratados como coisas diferentes,
ou "pistola" vs "Pistola" vs "PISTOLA"), dificultando qualquer agregação
futura.

## Decisão
Tipo, marca, modelo e calibre passam a ser catálogos fixos, populados via
migration (Flyway), sem endpoint de cadastro/edição pelo app:
- **Tipo**: lista simples (ex: Pistola, Revólver, Carabina, Espingarda...).
- **Marca**: lista simples (ex: Taurus, Glock, Imbel, CBC...).
- **Modelo**: pertence a uma marca (FK obrigatória) — ex: "G17" só existe
  dentro de "Glock", nunca solto.
- **Calibre**: lista simples, independente de marca/modelo (ex: .38, 9mm,
  .40, 12 gauge...).

O atleta escolhe entre os itens existentes (tipoId, marcaId, modeloId,
calibreId) no cadastro da arma (UC06), consultáveis via UC09. Não existe
endpoint pra criar novos itens do catálogo pelo app — adicionar um item novo
é uma migration nova.

## Alternativas consideradas
- Texto livre nos quatro campos → rejeitado: sem normalização, permite erro
  de digitação e impede agregação futura (ex: "quantos atletas têm Glock?"
  ou "quantas pistolas foram cadastradas?").
- Tipo como enum fixo no código (Java `enum`), diferente do tratamento dado
  a marca/modelo/calibre → rejeitado: trataria uma mesma categoria de
  problema (lista fechada de opções) de dois jeitos diferentes sem motivo
  técnico real; manter os quatro catálogos no mesmo padrão simplifica o
  UC09 (uma única forma de consulta) e a evolução futura (se um dia virar
  CRUD de admin, os quatro seguem juntos).
- Atleta cadastra um novo item se não encontrar o seu → rejeitado por ora:
  sem moderação, o catálogo rapidamente acumula duplicatas/erros de
  digitação; exigiria um fluxo de revisão que não existe no projeto ainda.
- CRUD completo de catálogo por um admin → rejeitado por ora: o projeto não
  tem conceito de admin/role ainda — escopo maior do que o necessário agora.

## Consequências
- UC06 passa a exigir tipoId + marcaId + modeloId (validado como pertencente
  à marca) + calibreId, em vez de tipo em texto livre.
- Precisa de endpoints de consulta somente leitura pro catálogo completo
  (UC09, incluindo tipos), pro cliente popular os dropdowns.
- Adicionar um tipo/marca/modelo/calibre novo exige uma migration e um
  deploy — aceitável no volume atual; pode precisar ser revisitado (ex:
  virar CRUD de admin) se a demanda por novos itens for frequente.

## Revisão pós-QA — cadastro rápido confirmado como desnecessário
A documentação de produto (visão/domínio do ShotTrack) sugeria um "cadastro
rápido" de arma, com campos opcionais e complementação posterior, como
alternativa ao catálogo fechado obrigatório desta decisão — esse conflito
ficou registrado como pendência, sem resolver.

Após testar manualmente o fluxo completo do atleta (cadastro, login, perfil,
modalidades, armas — checklist de QA), o cadastro rígido via catálogo fechado
se mostrou suficiente: a ausência de um cadastro rápido/parcial não fez falta
no uso real. **Decisão confirmada**: mantém-se o catálogo fechado obrigatório
como está; o cadastro rápido não será implementado, salvo se um caso de uso
real e concreto justificar revisitar isso no futuro.

## Revisão 2 — tipo e calibre amarrados ao modelo (bug de integridade)
Gap encontrado em uso real: `tipo`, `marca`, `modelo` e `calibre` eram
escolhas **independentes** (só validava modelo pertencer à marca) — nada
impedia cadastrar um modelo real de pistola marcado como "Revólver", ou um
calibre que aquele modelo nunca usaria de verdade.

**Decisão**: o catálogo é reestruturado pra amarrar tipo e calibres
válidos ao modelo, não mais como campos soltos:

```sql
CREATE TABLE weapon_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);

CREATE TABLE weapon_brands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);

CREATE TABLE weapon_calibers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);

CREATE TABLE weapon_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brand_id UUID NOT NULL REFERENCES weapon_brands(id),
    weapon_type_id UUID NOT NULL REFERENCES weapon_types(id),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    UNIQUE (brand_id, name)
);

CREATE TABLE weapon_model_calibers (
    weapon_model_id UUID NOT NULL REFERENCES weapon_models(id),
    weapon_caliber_id UUID NOT NULL REFERENCES weapon_calibers(id),
    PRIMARY KEY (weapon_model_id, weapon_caliber_id)
);
```

- `weapon_models` ganha `weapon_type_id` — o tipo passa a ser **inerente
  ao modelo**, não mais escolha livre do atleta no cadastro.
- `weapon_model_calibers` é a relação N:N entre modelo e calibres
  válidos — o atleta só pode escolher um calibre que essa tabela permite
  pra aquele modelo.

**Novo fluxo de cadastro (UC06/UC10)**: Marca → Modelo (filtrado pela
marca; tipo já vem junto, exibido mas não editável) → Calibre (filtrado
pelos permitidos daquele modelo). `WeaponRegisterRequest`/
`WeaponUpdateRequest` passam a exigir só `modelId` + `caliberId` (+
apelido opcional na edição) — `typeId` e `brandId` deixam de ser enviados
pelo cliente, são derivados do modelo no servidor.

## Alternativas consideradas (Revisão 2)
- Validar a combinação em código (regra de negócio "se modelo X, calibre
  precisa ser Y ou Z") em vez de tabela → rejeitado: hardcoded no código
  é mais frágil e mais difícil de manter que dado estruturado; qualquer
  novo modelo exigiria alterar código, não só popular uma tabela.
- Manter tipo como campo solto, só adicionar validação cruzada (tipo
  precisa bater com o tipo real do modelo, comparando os dois) →
  rejeitado: mais complexo que simplesmente eliminar a redundância —
  se o tipo já vem do modelo, não faz sentido pedir de novo ao atleta.

## Consequências
- Migration reestruturando `weapon_models` (nova coluna
  `weapon_type_id`) e criando `weapon_model_calibers`.
- Dados legados: para armas já cadastradas, a migration precisa (a)
  corrigir o `type_id` salvo em cada arma pra bater com o do modelo dela,
  e (b) inserir em `weapon_model_calibers` qualquer combinação
  modelo+calibre já usada por arma existente, pra não invalidar cadastro
  já feito.
- `WeaponRegisterRequest`/`WeaponUpdateRequest` mudam de shape (remove
  `typeId`), e o erro `WEAPON_MODEL_BRAND_MISMATCH` deixa de existir
  (não é mais possível enviar marca e modelo divergentes, já que só se
  envia `modelId`) — substituído por `WEAPON_CALIBER_NOT_ALLOWED_FOR_MODEL`.
- Catálogo (UC09) ganha um novo endpoint de consulta: calibres válidos
  por modelo.

## Referências
- Nenhuma