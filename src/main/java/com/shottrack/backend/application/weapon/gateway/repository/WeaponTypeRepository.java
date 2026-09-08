package com.shottrack.backend.application.weapon.gateway.repository;

import com.shottrack.backend.application.weapon.model.WeaponType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WeaponTypeRepository extends JpaRepository<WeaponType, UUID> {
}
