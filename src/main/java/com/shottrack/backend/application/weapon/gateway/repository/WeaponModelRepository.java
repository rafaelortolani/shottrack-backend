package com.shottrack.backend.application.weapon.gateway.repository;

import com.shottrack.backend.application.weapon.model.WeaponModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeaponModelRepository extends JpaRepository<WeaponModel, UUID> {

    List<WeaponModel> findAllByBrandId(UUID brandId);
}
