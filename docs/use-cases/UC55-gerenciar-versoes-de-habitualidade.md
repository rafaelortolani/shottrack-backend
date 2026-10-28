# UC55 — Gerenciar versões, níveis e exigências (admin)

## Ator
Administrador.

## Rotas
- `GET /api/admin/habituality/rules/{id}/versions` (versão vigente marcada)
- `POST /api/admin/habituality/rules/{id}/versions`
- `PUT /api/admin/habituality/versions/{versionId}`
- `DELETE /api/admin/habituality/versions/{versionId}`

Corpo (POST/PUT):
```json
{
  "validFrom": "2027-01-01",
  "windowMonths": 12,
  "levels": [
    { "name": "Iniciante", "position": 1,
      "requirements": [
        { "activityType": "TREINO", "minQuantity": 8,
          "onlyInClub": false, "distinctEvents": false, "minScope": null } ] },
    { "name": "Intermediário", "position": 2,
      "requirements": [
        { "activityType": "TREINO", "minQuantity": 12, "onlyInClub": false,
          "distinctEvents": false, "minScope": null },
        { "activityType": "COMPETICAO", "minQuantity": 2, "onlyInClub": false,
          "distinctEvents": true, "minScope": "ESTADUAL" } ] }
  ]
}
```
(Valores do exemplo são ilustrativos; os reais vêm da norma.)

## Fluxo principal
Admin cadastra uma versão completa (níveis + exigências) para a regra. A versão vale a
partir de `validFrom`. O PUT substitui níveis e exigências da versão.

## Regras
- Mudança de norma = nova versão com `validFrom` futuro. Versão futura: editável e
  removível. Versão vigente/passada: só editável (nunca removível) enquanto nenhum atleta
  acompanha a regra.
- Primeira versão da regra pode ter data passada ou de hoje; as seguintes devem ser
  futuras. Datas duplicadas na mesma regra não são permitidas.
- Pelo menos 1 nível; níveis com nomes únicos na versão; cada nível com pelo menos 1
  exigência; `minQuantity` ≥ 1; `windowMonths` ≥ 1; `minScope` só com atividade
  `COMPETICAO` ou `QUALQUER`.

## Erros
`UNAUTHORIZED`, `FORBIDDEN`, `HABITUALITY_RULE_NOT_FOUND`, `HABITUALITY_VERSION_NOT_FOUND`,
`HABITUALITY_VERSION_NOT_EDITABLE`, `HABITUALITY_VERSION_DATE_INVALID`, `VALIDATION_ERROR`.

## Definição de pronto
- [x] Criar com 2 níveis; editar futura; remover futura
- [x] Editar vigente com e sem atleta acompanhando; remover vigente negado
- [x] Datas inválidas/duplicadas; níveis duplicados; exigência inválida
- [x] 401/403; regra/versão inexistente

## Decisões de implementação
- Primeira versão: qualquer data. Seguintes: data futura (`400 HABITUALITY_VERSION_DATE_INVALID`);
  data repetida na regra: `409 HABITUALITY_VERSION_DATE_INVALID`.
- Mudar a data no PUT: pode manter a atual, ir para o futuro ou, na versão mais antiga da regra,
  ficar no passado/hoje.
- Nomes de nível únicos sem diferenciar maiúsculas; o resto das validações vem do Bean Validation.
- A resposta traz `current`, `editable` e `removable` por versão.

## Swagger e mensagens
- Rotas anotadas com `@Operation` em `HabitualityAdminController`.
- Mensagens pt/en: `HABITUALITY_RULE_NOT_FOUND`, `HABITUALITY_VERSION_NOT_FOUND`,
  `HABITUALITY_VERSION_NOT_EDITABLE` (409), `HABITUALITY_VERSION_DATE_INVALID`, `VALIDATION_ERROR`.
