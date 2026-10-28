package com.shottrack.backend.application.habituality.usecase;

import org.springframework.transaction.annotation.Transactional;
import com.shottrack.backend.application.habituality.dto.PersonalGoalRequest;
import com.shottrack.backend.application.habituality.dto.PersonalGoalResponse;
import com.shottrack.backend.application.habituality.gateway.AthleteHabitualityGateway;
import com.shottrack.backend.application.habituality.model.AthletePersonalGoal;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.model.VisitType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * UC58: meta de treinos por ano civil. Conta visitas TREINO encerradas, pelo dia de
 * início (UTC), entre 1º/jan e 31/dez do ano corrente — vira o ano, a contagem recomeça.
 */
@Service
@RequiredArgsConstructor
public class PersonalGoalService {

    private final AthleteHabitualityGateway athleteGateway;
    private final VisitGateway visitGateway;

    public PersonalGoalResponse set(UUID userId, PersonalGoalRequest request) {
        AthletePersonalGoal goal = athleteGateway.findGoalByUserId(userId)
                .map(existing -> {
                    existing.setYearlyTrainings(request.yearlyTrainings());
                    return existing;
                })
                .orElseGet(() -> AthletePersonalGoal.builder()
                        .userId(userId)
                        .yearlyTrainings(request.yearlyTrainings())
                        .build());
        return toResponse(userId, athleteGateway.saveGoal(goal));
    }

    @Transactional
    public void remove(UUID userId) {
        athleteGateway.deleteGoalByUserId(userId);
    }

    /** Usado pelo progresso (UC57); null quando o atleta não definiu meta. */
    PersonalGoalResponse find(UUID userId) {
        return athleteGateway.findGoalByUserId(userId).map(goal -> toResponse(userId, goal)).orElse(null);
    }

    private PersonalGoalResponse toResponse(UUID userId, AthletePersonalGoal goal) {
        int year = HabitualityVersionResolver.today().getYear();
        long done = visitGateway.countClosedByUserIdAndTypeBetween(userId, VisitType.TREINO,
                LocalDate.of(year, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
                LocalDate.of(year + 1, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        return new PersonalGoalResponse(goal.getYearlyTrainings(), done);
    }
}
