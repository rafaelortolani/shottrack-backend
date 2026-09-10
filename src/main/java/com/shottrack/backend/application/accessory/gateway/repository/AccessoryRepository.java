package com.shottrack.backend.application.accessory.gateway.repository;

import com.shottrack.backend.application.accessory.model.Accessory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccessoryRepository extends JpaRepository<Accessory, UUID> {

    List<Accessory> findAllByUserId(UUID userId);
}
