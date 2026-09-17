package com.shottrack.backend.application.accessory.gateway.repository;

import com.shottrack.backend.application.accessory.model.AccessoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccessoryTypeRepository extends JpaRepository<AccessoryType, UUID> {
}
