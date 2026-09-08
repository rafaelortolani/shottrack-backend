package com.shottrack.backend.application.weapon.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@RequiredArgsConstructor
@Entity
@Table(name = "weapons")
public class Weapon extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Column(name = "type_id", nullable = false)
    private final UUID typeId;

    @Column(name = "brand_id", nullable = false)
    private final UUID brandId;

    @Column(name = "model_id", nullable = false)
    private final UUID modelId;

    @Column(name = "caliber_id", nullable = false)
    private final UUID caliberId;
}
