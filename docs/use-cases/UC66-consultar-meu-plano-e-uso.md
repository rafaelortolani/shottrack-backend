# UC66 - Consultar meu plano e uso

## Ator
Atleta autenticado.

## Pré-condição
Usuário logado (UC02) com access token válido.

## Rota
`GET /api/users/me/plan`

## Fluxo principal
1. Atleta solicita seu plano
2. Sistema devolve `plan` (`code`, `name`), os `limits` efetivos (`photosPerSeries`, `photosPerAthlete`,
   `storageBytesPerAthlete`, `uploadsPerHour`) e o `usage` atual (`photos`, `storageBytes`)

## Regras
- Só os dados do próprio atleta. Não expõe outros planos nem configuração interna.
- `Cache-Control: no-store`.
- O uso é calculado a partir de `series_photos` do atleta (contagem e soma de `size_bytes`), com índice por atleta.

## Fluxos alternativos
- 1a. Token ausente ou inválido → `UNAUTHORIZED`

## Definição de pronto
- [x] Atleta novo (uso zero) e atleta com fotos (contagem e soma corretas)
- [x] Reflete mudança de plano e de limite feita pelo administrador
- [x] Um teste para o fluxo alternativo; isolamento (cada atleta vê só o seu)

## Referências
- ADR-0027, ADR-0026
- UC60, UC64, UC65
