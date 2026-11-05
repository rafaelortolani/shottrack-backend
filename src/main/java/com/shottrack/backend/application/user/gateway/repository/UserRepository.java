package com.shottrack.backend.application.user.gateway.repository;

import com.shottrack.backend.application.user.model.Role;
import com.shottrack.backend.application.user.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u.role from User u where u.id = :id")
    Optional<Role> findRoleById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    List<User> findAllByIdIn(Collection<UUID> ids);

    /** O fragmento chega com `!`, `%` e `_` já escapados com `!` (ver AdminUserService). */
    @Query("select u from User u where lower(u.email) like lower(concat('%', :fragment, '%')) escape '!' order by u.email")
    List<User> searchByEmailFragment(@Param("fragment") String fragment, Pageable pageable);

    @Query("select u.planId, count(u) from User u group by u.planId")
    List<Object[]> countGroupedByPlan();

    long countByPlanId(UUID planId);

    @Query("select u.planId from User u where u.id = :id")
    Optional<UUID> findPlanIdById(@Param("id") UUID id);
}
