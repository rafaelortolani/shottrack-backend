package com.shottrack.backend.application.weapon.gateway.repository;

import com.shottrack.backend.application.weapon.model.WeaponBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WeaponBrandRepository extends JpaRepository<WeaponBrand, UUID> {
}
