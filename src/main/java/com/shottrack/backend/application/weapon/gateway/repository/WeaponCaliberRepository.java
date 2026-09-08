package com.shottrack.backend.application.weapon.gateway.repository;

import com.shottrack.backend.application.weapon.model.WeaponCaliber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WeaponCaliberRepository extends JpaRepository<WeaponCaliber, UUID> {
}
