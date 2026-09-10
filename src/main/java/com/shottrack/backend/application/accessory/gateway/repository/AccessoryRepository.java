package com.shottrack.backend.application.accessory.gateway.repository;

import com.shottrack.backend.application.accessory.model.Accessory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccessoryRepository extends JpaRepository<Accessory, UUID> {
}
