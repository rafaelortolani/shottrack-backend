package com.shottrack.backend.application.visit.model;

/**
 * ADR-0023: âmbito da competição. A ordem dos valores é a ordem de grandeza
 * (ESTADUAL &lt; NACIONAL &lt; INTERNACIONAL) usada na comparação com o âmbito
 * mínimo de uma exigência.
 */
public enum CompetitionScope {
    ESTADUAL,
    NACIONAL,
    INTERNACIONAL;

    public boolean atLeast(CompetitionScope minimum) {
        return compareTo(minimum) >= 0;
    }
}
