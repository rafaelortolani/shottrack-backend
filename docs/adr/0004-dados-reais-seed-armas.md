# Dados reais para o seed do catálogo de armas (ADR-0004, Revisão 2)

Pesquisado em fontes públicas (fabricantes, Wikipedia, lojas especializadas).
Cada modelo já vem com tipo e calibre(s) reais e coerentes — é exatamente
o que a nova modelagem (weapon_models.weapon_type_id +
weapon_model_calibers) precisa pra impedir a combinação errada que você
encontrou.

## Tipos
Pistola, Revólver, Carabina, Espingarda

## Glock (Áustria) — só Pistola
| Modelo | Calibre(s) |
|---|---|
| G17 | 9mm |
| G19 | 9mm |
| G22 | .40 S&W |
| G21 | .45 ACP |
| G26 | 9mm |

## Taurus (Brasil)
| Modelo | Tipo | Calibre(s) |
|---|---|---|
| G2C | Pistola | 9mm |
| G3 | Pistola | 9mm |
| TX22 | Pistola | .22 LR |
| 605 | Revólver | .357 Magnum, .38 Special |
| 692 | Revólver | .357 Magnum, .38 Special, 9mm |
| 856 | Revólver | .38 Special |
| Judge | Revólver | .45 Colt |

## Imbel (Brasil) — Carabina
| Modelo | Calibre |
|---|---|
| IA2 | 5.56mm NATO |
| MD2 | 5.56mm NATO |

Nota: a IA2 também existe em variante 7.62mm NATO, e a MD2 é a base da
família FAL nacional — se quiser incluir essas variantes, dá pra adicionar
depois; mantive uma linha por modelo pra simplicidade inicial do seed.

## CBC (Brasil) — Espingarda
| Modelo | Calibre(s) |
|---|---|
| Momentum | 12, 20 |
| Military 3.0 | 12 |
| Montenegro | 12, 20, 28, .410 |

## Smith & Wesson (EUA)
| Modelo | Tipo | Calibre(s) |
|---|---|---|
| Model 686 | Revólver | .357 Magnum, .38 Special |
| Model 10 | Revólver | .38 Special |
| M&P9 | Pistola | 9mm |
| M&P Shield | Pistola | 9mm |

## Calibres únicos necessários no catálogo
9mm, .40 S&W, .45 ACP, .22 LR, .357 Magnum, .38 Special, .45 Colt,
5.56mm NATO, 12, 20, 28, .410