package com.shottrack.backend.application.plan.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.shottrack.backend.application.plan.dto.CreatePlanRequest;
import com.shottrack.backend.application.plan.dto.EditPlanLimitsRequest;
import com.shottrack.backend.application.plan.dto.PlanLimitResponse;
import com.shottrack.backend.application.plan.dto.PlanResponse;
import com.shottrack.backend.application.plan.dto.UpdatePlanRequest;
import com.shottrack.backend.application.plan.gateway.PlanGateway;
import com.shottrack.backend.application.plan.gateway.PlanLimitGateway;
import com.shottrack.backend.application.plan.mapper.PlanMapper;
import com.shottrack.backend.application.plan.model.Plan;
import com.shottrack.backend.application.plan.model.PlanLimit;
import com.shottrack.backend.application.plan.model.PlanLimitId;
import com.shottrack.backend.application.plan.model.PlanLimitValue;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * UC63/UC64/UC68/UC69 (ADR-0027): administração de planos e limites. Toda alteração roda numa transação que começa
 * travando a administração de planos (lock consultivo): requisições simultâneas se enfileiram, então nunca há plano
 * acima do teto, dois padrões ou nenhum. Planos nunca são excluídos.
 */
@Service
@RequiredArgsConstructor
public class PlanAdminService {

    static final int MAX_PLANS = 20;
    private static final Pattern CODE_FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");
    private static final int NAME_MIN = 2;
    private static final int NAME_MAX = 60;

    private final PlanGateway planGateway;
    private final PlanLimitGateway planLimitGateway;
    private final UserGateway userGateway;
    private final PlanLimitService planLimitService;
    private final PlanAuditRecorder auditRecorder;
    private final PlanMapper planMapper;

    /** UC63: planos com limites e quantidade de atletas, em 3 consultas (planos, limites, contagens), sem N+1. */
    @Transactional(readOnly = true)
    public List<PlanResponse> list() {
        List<Plan> plans = planGateway.findAll();
        Map<UUID, Long> athletes = userGateway.countByPlanIds();
        Map<UUID, Map<String, Long>> stored = planLimitGateway.findAllByPlanIds(plans.stream().map(Plan::getId).toList()).stream()
                .collect(Collectors.groupingBy(v -> v.getId().planId(),
                        Collectors.toMap(v -> v.getId().limitKey(), PlanLimitValue::getValue)));
        return plans.stream()
                .map(plan -> planMapper.toResponse(plan, athletes.getOrDefault(plan.getId(), 0L),
                        limitResponses(stored.getOrDefault(plan.getId(), Map.of()))))
                .toList();
    }

    /** UC64: edição parcial e atômica — valida tudo antes de gravar; qualquer erro desfaz tudo. */
    @Transactional
    public PlanResponse editLimits(UUID actorId, String code, EditPlanLimitsRequest request) {
        planGateway.lockPlanAdministration();
        Plan plan = findPlanOrThrow(code);

        Map<PlanLimit, Long> requested = new LinkedHashMap<>();
        request.limits().forEach((key, node) -> {
            PlanLimit limit = PlanLimit.fromKey(key).filter(PlanLimit::isEnforced)
                    .orElseThrow(() -> new BusinessException("PLAN_LIMIT_INVALID_KEY", HttpStatus.BAD_REQUEST));
            requested.put(limit, validValue(limit, node));
        });

        Map<String, PlanLimitValue> stored = planLimitGateway.findAllByPlanId(plan.getId()).stream()
                .collect(Collectors.toMap(v -> v.getId().limitKey(), v -> v));
        requested.forEach((limit, value) -> {
            PlanLimitValue current = stored.get(limit.name());
            long oldValue = current != null ? current.getValue() : limit.getDefaultValue();
            if (oldValue == value) {
                return;
            }
            if (current != null) {
                current.setValue(value);
                planLimitGateway.save(current);
            } else {
                planLimitGateway.save(PlanLimitValue.builder().id(new PlanLimitId(plan.getId(), limit.name())).value(value).build());
            }
            auditRecorder.limitChanged(actorId, plan.getCode(), limit.name(), oldValue, value);
        });
        planLimitService.invalidate();
        return response(plan);
    }

    /** UC68. */
    @Transactional
    public PlanResponse create(UUID actorId, CreatePlanRequest request) {
        planGateway.lockPlanAdministration();

        String code = request.code();
        if (code == null || !CODE_FORMAT.matcher(code).matches()) {
            throw new BusinessException("PLAN_CODE_INVALID", HttpStatus.BAD_REQUEST);
        }
        String name = validName(request.name());
        Plan source = request.copyFromCode() == null || request.copyFromCode().isBlank() ? null : findPlanOrThrow(request.copyFromCode().trim());
        if (planGateway.count() >= MAX_PLANS) {
            throw new BusinessException("PLAN_COUNT_LIMIT_REACHED", HttpStatus.CONFLICT);
        }
        if (planGateway.existsByCode(code)) {
            throw new BusinessException("PLAN_CODE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
        if (planGateway.existsByName(name)) {
            throw new BusinessException("PLAN_NAME_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        Plan plan = planGateway.saveAndFlush(Plan.builder().code(code).name(name).build());
        if (source != null) {
            planLimitGateway.findAllByPlanId(source.getId()).forEach(value -> planLimitGateway.save(
                    PlanLimitValue.builder().id(new PlanLimitId(plan.getId(), value.getId().limitKey())).value(value.getValue()).build()));
        }
        auditRecorder.planCreated(actorId, plan.getCode(), plan.getName());
        return response(plan);
    }

    /** UC69: edição parcial (nome, ativo, padrão); só audita o que mudou. */
    @Transactional
    public PlanResponse update(UUID actorId, String code, UpdatePlanRequest request) {
        planGateway.lockPlanAdministration();
        Plan plan = findPlanOrThrow(code);

        if (request.name() == null && request.active() == null && request.defaultPlan() == null) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        String newName = request.name() == null ? plan.getName() : validName(request.name());
        if (request.name() != null && !newName.equalsIgnoreCase(plan.getName()) && planGateway.existsByNameExcluding(newName, plan.getId())) {
            throw new BusinessException("PLAN_NAME_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
        boolean newActive = request.active() == null ? plan.isActive() : request.active();
        boolean newDefault = request.defaultPlan() == null ? plan.isDefaultPlan() : request.defaultPlan();

        if (plan.isDefaultPlan() && !newDefault) {
            throw new BusinessException("PLAN_DEFAULT_REQUIRED", HttpStatus.CONFLICT);
        }
        if (newDefault && !newActive) {
            // definir como padrão um plano inativo (ou desativar o padrão)
            throw new BusinessException(plan.isDefaultPlan() ? "PLAN_DEFAULT_CANNOT_BE_DEACTIVATED" : "PLAN_NOT_ACTIVE", HttpStatus.CONFLICT);
        }

        if (!newName.equals(plan.getName())) {
            auditRecorder.planUpdated(actorId, plan.getCode(), "name", plan.getName(), newName);
            plan.setName(newName);
        }
        if (newActive != plan.isActive()) {
            auditRecorder.planUpdated(actorId, plan.getCode(), "active", plan.isActive(), newActive);
            plan.setActive(newActive);
        }
        if (newDefault && !plan.isDefaultPlan()) {
            // o anterior deixa de ser padrão primeiro (e é gravado), para o índice único parcial nunca ver dois
            for (Plan previous : planGateway.findDefaults()) {
                previous.setDefaultPlan(false);
                planGateway.saveAndFlush(previous);
                auditRecorder.planUpdated(actorId, previous.getCode(), "default", true, false);
            }
            plan.setDefaultPlan(true);
            auditRecorder.planUpdated(actorId, plan.getCode(), "default", false, true);
        }
        planGateway.saveAndFlush(plan);
        return response(plan);
    }

    private PlanResponse response(Plan plan) {
        Map<String, Long> stored = planLimitGateway.findAllByPlanId(plan.getId()).stream()
                .collect(Collectors.toMap(v -> v.getId().limitKey(), PlanLimitValue::getValue));
        return planMapper.toResponse(plan, userGateway.countByPlanId(plan.getId()), limitResponses(stored));
    }

    private List<PlanLimitResponse> limitResponses(Map<String, Long> stored) {
        return Arrays.stream(PlanLimit.values())
                .map(limit -> planMapper.toLimitResponse(limit, stored.getOrDefault(limit.name(), limit.getDefaultValue())))
                .toList();
    }

    private Plan findPlanOrThrow(String code) {
        return planGateway.findByCode(code).orElseThrow(() -> new BusinessException("PLAN_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private static String validName(String raw) {
        String name = raw == null ? "" : raw.strip();
        if (name.length() < NAME_MIN || name.length() > NAME_MAX) {
            throw new BusinessException("PLAN_NAME_INVALID", HttpStatus.BAD_REQUEST);
        }
        return name;
    }

    /** Só inteiro JSON dentro da faixa: texto, decimal, nulo, booleano e número gigante dão PLAN_LIMIT_OUT_OF_RANGE. */
    private static long validValue(PlanLimit limit, JsonNode node) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToLong() || !limit.accepts(node.longValue())) {
            throw new BusinessException("PLAN_LIMIT_OUT_OF_RANGE", HttpStatus.BAD_REQUEST);
        }
        return node.longValue();
    }
}
