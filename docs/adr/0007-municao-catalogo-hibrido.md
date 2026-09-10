# ADR-0007: Munição — catálogo híbrido com cadastro parcial

## Contexto
Ao contrário de Arma (ADR-0004, catálogo fechado obrigatório), o documento
de domínio pede explicitamente que Munição aceite cadastro parcial:
*"quando o atleta não souber todos os detalhes, pode selecionar uma munição
parcialmente cadastrada e complementar os dados depois."*

Mas nem todo campo de munição tem a mesma natureza: fabricante é um valor
estável e reaproveitável (mesmo risco de inconsistência do ADR-0004 — "CBC"
vs "cbc"), enquanto peso do projétil e quantidade de pólvora são medidas
numéricas que variam por lote e não fazem sentido como lista fechada.

## Decisão
Modelo híbrido:
- **Fabricante**: catálogo fechado, mesmo padrão do ADR-0004 (seed via
  migration, consulta somente leitura).
- **Calibre**: reaproveita o catálogo de calibre já existente para armas
  (`/api/weapon-catalog/calibers`) — mesmo conceito, evita duplicar catálogo
  e mantém consistência entre arma e munição do mesmo calibre.
- **Peso do projétil, quantidade de pólvora, tipo de projétil, lote**: campos
  livres (numéricos ou texto conforme o caso), todos opcionais.
- **Apelido/identificação**: campo livre, usado pelo atleta pra reconhecer a
  munição na lista.

Cadastro parcial permitido: **nenhum campo é obrigatório isoladamente**,
mas pelo menos um entre `fabricanteId` e `apelido` precisa estar preenchido
— o suficiente pra o atleta conseguir reconhecer o item depois, sem exigir
que ele saiba calibre, peso ou lote no momento do registro.

## Alternativas consideradas
- Catálogo fechado igual arma (todos os campos via catálogo) → rejeitado:
  contraria explicitamente o que o documento pede pra este domínio
  especificamente; lote e identificação não fazem sentido como catálogo.
- Totalmente livre, nada obrigatório → rejeitado: sem nenhum campo mínimo, o
  atleta poderia salvar um registro sem forma nenhuma de diferenciá-lo de
  outro depois — pelo menos um identificador é necessário.
- Catálogo de calibre próprio pra munição, separado do de arma → rejeitado:
  duplicaria um conceito que já existe, sem ganho — o mesmo calibre serve
  pra ambos.

## Consequências
- Precisa de migration com seed de fabricantes comuns de munição.
- Reaproveita o endpoint de calibre já existente — nenhuma rota nova pra isso.
- Validação de "fabricanteId OU apelido" é uma regra de negócio no usecase,
  não uma validação simples de Bean Validation (não dá pra expressar
  "um OU outro" com `@NotBlank` de forma direta).
- Diferente de Arma (edição por substituição completa, ADR-0004), a edição
  de munição (UC15) é parcial — só os campos enviados são atualizados,
  coerente com "completar depois".

## Referências
- ADR-0004 (catálogo de armas — modelo de referência, com as diferenças
  explicadas acima)