package com.shottrack.backend.application.weapon.gateway.repository;

import com.shottrack.backend.application.weapon.model.Weapon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeaponRepository extends JpaRepository<Weapon, UUID> {

    List<Weapon> findAllByUserId(UUID userId);
}
