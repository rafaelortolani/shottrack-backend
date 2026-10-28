package com.shottrack.backend.application.habituality.gateway;

import com.shottrack.backend.application.habituality.gateway.repository.HabitualityRuleRepository;
import com.shottrack.backend.application.habituality.model.HabitualityRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class HabitualityRuleGatewayImpl implements HabitualityRuleGateway {

    private final HabitualityRuleRepository repository;

    @Override
    public HabitualityRule save(HabitualityRule rule) {
        return repository.save(rule);
    }

    @Override
    public Optional<HabitualityRule> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<HabitualityRule> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Override
    public List<HabitualityRule> findAllActive() {
        return repository.findAllByActiveTrueOrderByNameAsc();
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameExcluding(String name, UUID excludedId) {
        return repository.existsByNameIgnoreCaseAndIdNot(name, excludedId);
    }
}
