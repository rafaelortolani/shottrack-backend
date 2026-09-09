# ADR-0005: Modalidade como catálogo fixo

## Contexto
A Fase 1 do roadmap (Conhecendo o Atleta) exige que o atleta selecione quais
modalidades pratica. O documento de domínio cita exemplos (precisão, IPSC,
Steel Challenge, trap, skeet, saque e tiro) e menciona "modalidade
personalizada" como possibilidade — mas essa segunda parte ainda não tem
nenhuma decisão de produto ou técnica por trás.

## Decisão
Modalidade é um catálogo fixo, populado via migration (Flyway), seguindo o
mesmo padrão já usado para tipo/marca/modelo/calibre de arma (ADR-0004):
sem endpoint de criação/edição pelo app, consultável via UC11.

**"Modalidade personalizada"** (citada no documento de domínio) fica fora de
escopo por agora — mesmo tratamento dado a outras funcionalidades futuras do
documento (ex: verificação de email no cadastro, ADR-0002). Quando for
implementada, provavelmente vai exigir revisar esta decisão.

## Alternativas consideradas
- Atleta cria modalidade livremente → rejeitado por ora: mesmo risco de
  duplicidade/inconsistência já discutido no ADR-0004, e o documento não
  detalha o suficiente sobre "modalidade personalizada" pra implementar com
  segurança agora.
- Catálogo + opção de personalizada desde já → rejeitado por ora: aumenta o
  escopo do primeiro corte sem necessidade imediata (a Fase 1 só precisa que
  o atleta consiga selecionar modalidades existentes).

## Consequências
- Precisa de migration com seed inicial de modalidades comuns.
- Precisa de endpoint de consulta somente leitura (UC11).
- "Modalidade personalizada" fica registrada como pendência — não deve ser
  esquecida quando o produto avançar pra Fase 3 (Treinos), já que lá
  modalidade passa a ser referenciada por todo treino registrado.

## Referências
- ADR-0004 (mesmo padrão aplicado a tipo/marca/modelo/calibre de arma)