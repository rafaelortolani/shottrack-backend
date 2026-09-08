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

## Referências
- Nenhuma