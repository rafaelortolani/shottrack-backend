package com.shottrack.backend.application.habituality.usecase;

import com.shottrack.backend.application.habituality.dto.HabitualityRuleRequest;
import com.shottrack.backend.application.habituality.dto.HabitualityRuleResponse;
import com.shottrack.backend.application.habituality.gateway.HabitualityRuleGateway;
import com.shottrack.backend.application.habituality.mapper.HabitualityMapper;
import com.shottrack.backend.application.habituality.model.HabitualityRule;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * UC54: regras nunca são excluídas — desativar tira a regra da escolha de novos atletas.
 */
@Service
@RequiredArgsConstructor
public class HabitualityRuleAdminService {

    private final HabitualityRuleGateway ruleGateway;
    private final HabitualityMapper mapper;

    public List<HabitualityRuleResponse> list() {
        return ruleGateway.findAll().stream().map(mapper::toRuleResponse).toList();
    }

    public HabitualityRuleResponse create(HabitualityRuleRequest request) {
        String name = request.name().trim();
        if (ruleGateway.existsByName(name)) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        HabitualityRule rule = HabitualityRule.builder()
                .name(name)
                .legalReference(request.legalReference())
                .active(request.active())
                .build();
        return mapper.toRuleResponse(ruleGateway.save(rule));
    }

    public HabitualityRuleResponse update(UUID ruleId, HabitualityRuleRequest request) {
        HabitualityRule rule = findOrThrow(ruleId);
        String name = request.name().trim();
        if (ruleGateway.existsByNameExcluding(name, ruleId)) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        rule.setName(name);
        rule.setLegalReference(request.legalReference());
        if (request.active() != null) {
            rule.setActive(request.active());
        }
        return mapper.toRuleResponse(ruleGateway.save(rule));
    }

    private HabitualityRule findOrThrow(UUID ruleId) {
        return ruleGateway.findById(ruleId)
                .orElseThrow(() -> new BusinessException("HABITUALITY_RULE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
