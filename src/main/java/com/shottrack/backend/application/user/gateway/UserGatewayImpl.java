package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.application.user.model.Role;
import com.shottrack.backend.application.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class UserGatewayImpl implements UserGateway {

    private final UserRepository userRepository;

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public User saveAndFlush(User user) {
        return userRepository.saveAndFlush(user);
    }

    @Override
    public Optional<User> findByIdForUpdate(UUID id) {
        return userRepository.findByIdForUpdate(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return userRepository.existsById(id);
    }

    @Override
    public Optional<Role> findRoleById(UUID id) {
        return userRepository.findRoleById(id);
    }

    @Override
    public void deleteById(UUID id) {
        userRepository.deleteById(id);
    }

    @Override
    public List<User> findAllByIdIn(Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : userRepository.findAllByIdIn(ids);
    }

    @Override
    public List<User> searchByEmailFragment(String fragment, int limit) {
        return userRepository.searchByEmailFragment(fragment, PageRequest.of(0, limit));
    }

    @Override
    public Map<UUID, Long> countByPlanIds() {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : userRepository.countGroupedByPlan()) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public long countByPlanId(UUID planId) {
        return userRepository.countByPlanId(planId);
    }

    @Override
    public Optional<UUID> findPlanIdById(UUID id) {
        return userRepository.findPlanIdById(id);
    }
}
