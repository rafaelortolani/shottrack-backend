package com.shottrack.backend.application.habituality.usecase;

import com.shottrack.backend.application.habituality.dto.HabitualityProgressResponse;
import com.shottrack.backend.application.habituality.dto.ProgressRequirementResponse;
import com.shottrack.backend.application.habituality.dto.ProgressRuleResponse;
import com.shottrack.backend.application.habituality.dto.UpcomingVersionResponse;
import com.shottrack.backend.application.habituality.gateway.AthleteHabitualityGateway;
import com.shottrack.backend.application.habituality.gateway.HabitualityRuleGateway;
import com.shottrack.backend.application.habituality.model.AthleteHabitualityTracking;
import com.shottrack.backend.application.habituality.model.HabitualityRequirement;
import com.shottrack.backend.application.habituality.model.HabitualityRule;
import com.shottrack.backend.application.habituality.model.HabitualityRuleVersion;
import com.shottrack.backend.application.visit.gateway.ClosedVisitFact;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.model.VisitType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * UC57: apuração sob demanda (ADR-0016, sem fila) numa janela móvel dos últimos
 * windowMonths meses até hoje (inclusive os dois extremos, por dia de início da visita,
 * em UTC). Só visitas ENCERRADAS contam.
 */
@Service
@RequiredArgsConstructor
public class HabitualityProgressService {

    private final AthleteHabitualityGateway athleteGateway;
    private final HabitualityRuleGateway ruleGateway;
    private final HabitualityVersionResolver resolver;
    private final HabitualityContentReader contentReader;
    private final PersonalGoalService personalGoalService;
    private final VisitGateway visitGateway;
    private final MessageSource messageSource;

    public HabitualityProgressResponse getProgress(UUID userId) {
        var goal = personalGoalService.find(userId);
        Optional<AthleteHabitualityTracking> tracking = athleteGateway.findTrackingByUserId(userId);
        if (tracking.isEmpty()) {
            return new HabitualityProgressResponse(null, null, null, null, null, null, null, null, false, false, goal);
        }

        HabitualityRule rule = ruleGateway.findById(tracking.get().getRuleId()).orElseThrow();
        ProgressRuleResponse ruleResponse = new ProgressRuleResponse(rule.getId(), rule.getName(), rule.getLegalReference());
        String levelName = tracking.get().getLevelName();
        if (!rule.isActive()) {
            return new HabitualityProgressResponse(ruleResponse, levelName, null, null, null, null, null, null,
                    false, true, goal);
        }

        LocalDate today = HabitualityVersionResolver.today();
        UpcomingVersionResponse upcoming = resolver.upcoming(rule.getId(), today)
                .map(next -> new UpcomingVersionResponse(next.getValidFrom()))
                .orElse(null);

        Optional<HabitualityRuleVersion> current = resolver.current(rule.getId(), today);
        Optional<HabitualityContentReader.LevelContent> level = current.flatMap(version ->
                contentReader.content(version.getId()).stream()
                        .filter(content -> content.level().getName().equalsIgnoreCase(levelName))
                        .findFirst());
        if (level.isEmpty()) {
            // Nível ausente na versão em vigor: o atleta precisa escolher de novo.
            return new HabitualityProgressResponse(ruleResponse, levelName,
                    current.map(HabitualityRuleVersion::getWindowMonths).orElse(null), null, null, null, null,
                    upcoming, true, false, goal);
        }

        int windowMonths = current.get().getWindowMonths();
        LocalDate periodStart = today.minusMonths(windowMonths);
        List<ClosedVisitFact> facts = visitGateway.findClosedFactsByUserIdBetween(userId,
                periodStart.atStartOfDay().toInstant(ZoneOffset.UTC),
                today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Locale locale = LocaleContextHolder.getLocale();
        List<ProgressRequirementResponse> requirements = level.get().requirements().stream()
                .map(requirement -> evaluate(requirement, facts, locale))
                .toList();
        boolean compliant = requirements.stream().allMatch(ProgressRequirementResponse::met);
        return new HabitualityProgressResponse(ruleResponse, level.get().level().getName(), windowMonths,
                periodStart, today, compliant, requirements, upcoming, false, false, goal);
    }

    private ProgressRequirementResponse evaluate(HabitualityRequirement requirement, List<ClosedVisitFact> facts,
                                                 Locale locale) {
        List<ClosedVisitFact> matching = facts.stream().filter(fact -> counts(requirement, fact)).toList();
        long done = requirement.isDistinctEvents()
                ? matching.stream().map(fact -> new Event(day(fact.startedAt()), fact.trainingLocationId())).distinct().count()
                : matching.size();
        long missing = Math.max(0, requirement.getMinQuantity() - done);
        return new ProgressRequirementResponse(describe(requirement, locale), requirement.getActivityType(),
                requirement.isOnlyInClub(), requirement.isDistinctEvents(), requirement.getMinScope(),
                requirement.getMinQuantity(), done, missing, missing == 0);
    }

    private boolean counts(HabitualityRequirement requirement, ClosedVisitFact fact) {
        boolean typeMatches = switch (requirement.getActivityType()) {
            case TREINO -> fact.visitType() == VisitType.TREINO;
            case COMPETICAO -> fact.visitType() == VisitType.COMPETICAO;
            case QUALQUER -> true;
        };
        if (!typeMatches) {
            return false;
        }
        if (requirement.isOnlyInClub() && !fact.club()) {
            return false;
        }
        // Âmbito mínimo: visita sem âmbito (treino, ou competição sem âmbito) nunca conta.
        return requirement.getMinScope() == null
                || (fact.competitionScope() != null && fact.competitionScope().atLeast(requirement.getMinScope()));
    }

    private static LocalDate day(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    /** "Eventos distintos" = pares distintos (data, local), ADR-0023. */
    private record Event(LocalDate day, UUID trainingLocationId) {
    }

    private String describe(HabitualityRequirement requirement, Locale locale) {
        String activity = messageSource.getMessage("HABITUALITY_ACTIVITY_" + requirement.getActivityType().name(), null, locale);
        String base = messageSource.getMessage("HABITUALITY_REQUIREMENT",
                new Object[]{requirement.getMinQuantity(), activity}, locale);
        List<String> filters = new ArrayList<>();
        if (requirement.isOnlyInClub()) {
            filters.add(messageSource.getMessage("HABITUALITY_FILTER_ONLY_IN_CLUB", null, locale));
        }
        if (requirement.isDistinctEvents()) {
            filters.add(messageSource.getMessage("HABITUALITY_FILTER_DISTINCT_EVENTS", null, locale));
        }
        if (requirement.getMinScope() != null) {
            String scope = messageSource.getMessage("HABITUALITY_SCOPE_" + requirement.getMinScope().name(), null, locale);
            filters.add(messageSource.getMessage("HABITUALITY_FILTER_MIN_SCOPE", new Object[]{scope}, locale));
        }
        return filters.isEmpty() ? base : base + " (" + String.join(", ", filters) + ")";
    }
}
