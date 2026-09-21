# ADR-0013: Série — registro incompleto permitido, resultados com 3 estados

## Contexto
Série é a menor unidade de registro do ShotTrack — um exercício/tentativa
dentro de um treino. O documento de domínio é explícito: o sistema não
deve obrigar o atleta a preencher tudo no momento do registro
("registrar primeiro, organizar depois"), e precisa distinguir resultado
não preenchido, resultado igual a zero, e resultado não aplicável.

## Decisão
**Série**: pertence a um Treino. Nenhum campo é obrigatório:
- `armaId` (opcional, referência ao acervo)
- `municaoId` (opcional, referência ao acervo)
- `distanciaMetros` (opcional, numérico)
- `alvo` (opcional, **texto livre** — ver evolução futura abaixo)
- `quantidadeDisparos` (opcional, numérico)
- `observacoes` (opcional, texto)

**Resultados**: relação separada entre Série e Tipo de Resultado (não
campos fixos na série), com **três estados possíveis**:
- Sem registro pra aquele tipo → **não preenchido** (padrão, nenhuma ação
  necessária)
- Registro com um valor → **valor de verdade**, inclusive `"0"`
- Registro marcado explicitamente como não aplicável (flag separada do
  valor) → **não aplicável**

Um resultado só pode ser registrado pra um tipo que esteja configurado no
Perfil de Modalidade (UC30) da modalidade do treino daquela série.

**Formato do valor**: armazenado como texto (`string`), independente do
tipo de resultado. A interpretação (número pra pontuação/tempo/
agrupamento/acertos/erros/penalidades/fator de desempenho, sim/não pra
exercício concluído, texto livre pra anotação livre) é responsabilidade
da camada de validação, baseada no tipo — não há uma coluna por formato.

## Evolução futura — Alvo como entidade própria
O atleta pretende, no futuro, ter um cadastro de alvos com foto anexada,
permitindo análise automática por IA (leitura de imagem, identificação de
impactos, cálculo de agrupamento — já previsto no roadmap de produto,
seção "Assistente Inteligente"). Hoje `alvo` é só texto livre porque essa
funcionalidade ainda não existe. Quando existir, o campo evolui de texto
pra uma referência a uma entidade `Alvo` (provavelmente com foto) — mesmo
raciocínio já aplicado a Local de Treino (ADR-0010): simples agora,
estrutura pronta pra crescer, decisão de extensão fica pra um ADR próprio
quando a funcionalidade for concreta.

## Alternativas consideradas
- Exigir arma e quantidade de disparos no momento do registro → rejeitado:
  contraria diretamente o princípio central do produto.
- `null` representando tanto "não preenchido" quanto "não aplicável" →
  rejeitado: o documento pede explicitamente que os dois sejam
  distinguíveis; usar o mesmo `null` pra ambos perde essa distinção.
- Colunas tipadas por formato de resultado (numérica, texto, booleana,
  todas nullable) → rejeitado por ora: mais complexo de manter, e o
  volume de tipos de resultado é pequeno o bastante pra texto-com-
  validação ser suficiente; pode ser revisitado se surgir necessidade real
  de query/agregação por valor numérico direto no banco.

## Consequências
- Nenhum campo obrigatório em Série — validação de negócio quase
  inexistente no cadastro em si.
- Resultado de série é sub-recurso próprio (criar/remover), não parte do
  payload de criação da série — mesmo padrão já usado em modalidades
  praticadas (UC12) e perfil de modalidade (UC30).
- Exclusão de série: sem bloqueio (ADR-0006 não se aplica aqui — nada
  ainda referencia uma série pra "estar em uso").

## Referências
- ADR-0006 (não se aplica à exclusão de série — nota explícita acima)
- ADR-0010 (mesmo padrão de "simples agora, estrutura pronta pro futuro")
- UC12, UC30 (mesmo padrão de sub-recurso pra relação)