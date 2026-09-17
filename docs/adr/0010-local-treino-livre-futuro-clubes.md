# ADR-0010: Local de treino livre, com estrutura pronta pra clubes parceiros futuros

## Contexto
O produto pretende, no futuro, firmar parcerias com clubes de tiro,
permitindo ao atleta selecionar um clube de uma lista filtrável por
cidade/estado, em vez de cadastrar o local manualmente. Esse catálogo de
clubes parceiros não existe hoje, e construir isso agora seria
especulativo — não há parceria firmada, nem requisito concreto do que um
"clube parceiro" precisa ter além do nome/localização.

## Decisão
Por agora, Local de Treino é **cadastro livre do próprio atleta** — sem
catálogo, sem vínculo com nenhuma entidade de clube:
- Nome (obrigatório)
- Cidade (obrigatório)
- Estado (obrigatório, sigla — ex: SP, RJ)

Cidade e estado são capturados desde já como campos estruturados (não
embutidos livremente no nome), mesmo sendo texto livre agora — isso deixa
o dado pronto pra ser usado em filtro/busca no futuro, sem precisar migrar
dados antigos quando o catálogo de clubes existir.

## Extensão futura (não implementada agora)
Quando o catálogo de clubes parceiros existir, a extensão natural seria:
Local de Treino ganha um campo opcional `clubeParceiroId`, e o cadastro
passa a oferecer duas formas de preencher — buscar um clube parceiro da
lista (filtrada por estado/cidade) ou continuar cadastrando manualmente.
Os dois modos convivem; usar o catálogo nunca é obrigatório. Essa extensão
é decisão de um ADR próprio, quando a parceria for concreta.

## Alternativas consideradas
- Criar a tabela de clube parceiro vazia desde já, sem uso real →
  rejeitado: especulativo, mesmo raciocínio já aplicado a outras decisões
  adiadas do projeto (ex: modalidade personalizada, ADR-0005).
- Deixar cidade/estado de fora agora, só adicionar quando o catálogo
  existir → rejeitado: exigiria migração de dados existentes depois;
  capturar como campo estruturado agora é barato e não trava nada.

## Consequências
- Cadastro de local hoje é simples: nome, cidade, estado, tudo obrigatório.
- Nenhum endpoint de catálogo de clubes agora.
- Exclusão de local segue o mesmo padrão do ADR-0006 (bloqueada se já
  usado em alguma visita — sem efeito prático ainda, já que Visita não
  existe).

## Referências
- ADR-0006 (exclusão bloqueada, não arquivamento)