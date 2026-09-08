# ADR-0003: Não armazenar número de registro da arma

## Contexto
O cadastro de armas (UC06) precisa identificar cada arma do atleta. No Brasil,
armas de fogo têm um número de registro vinculado ao Exército/SIGMA
(Certificado de Registro), que é um dado sensível: liga a arma à identidade
civil do dono e a um número de controle federal.

## Decisão
O cadastro de armas não armazena número de registro (CR/SIGMA) nem número de
série. Os dados coletados são apenas **tipo, marca, modelo e calibre** —
suficientes pra identificar a arma dentro do app (treinos, competições,
estatísticas) sem guardar dado sensível vinculado a registro federal.

## Alternativas consideradas
- Armazenar o número de registro mascarado (ex: só os últimos dígitos) →
  rejeitado por ora: complexidade de mascaramento/criptografia sem ganho
  claro de produto no estágio atual.
- Armazenar como campo opcional → rejeitado: uma vez que o campo existe, cedo
  ou tarde alguém preenche; mais simples e seguro não coletar.

## Consequências
- Não há como diferenciar duas armas do mesmo atleta com tipo/marca/modelo/
  calibre idênticos (ex: dois revólveres iguais do mesmo modelo) — aceitável
  no escopo atual.
- Se no futuro for necessário coletar esse dado (ex: pra emitir alguma
  declaração formal), essa decisão precisa ser revisitada com um novo ADR,
  já que passa a envolver dado sensível de verdade.

## Referências
- Nenhuma
