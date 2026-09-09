package com.shottrack.backend.application.weapon.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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

    @Setter
    @NonNull
    @Column(name = "type_id", nullable = false)
    private UUID typeId;

    @Setter
    @NonNull
    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Setter
    @NonNull
    @Column(name = "model_id", nullable = false)
    private UUID modelId;

    @Setter
    @NonNull
    @Column(name = "caliber_id", nullable = false)
    private UUID caliberId;

    /**
     * Texto livre opcional (UC06/UC10) — diferencia armas com o mesmo tipo/
     * marca/modelo/calibre. Ao contrário dos ids acima, pode ser alterado
     * mesmo depois da arma já ter sido usada em treino/resultado (UC10).
     */
    @Setter
    @Column
    private String nickname;
}
