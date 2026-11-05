package com.shottrack.backend.application.plan.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.UUID;

/** ADR-0027: plano. O `code` nunca muda; planos nunca são excluídos, só desativados. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "plans")
public class Plan extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, updatable = false)
    private final String code;

    @Setter
    @NonNull
    @Column(nullable = false)
    private String name;

    @Setter
    @Column(nullable = false)
    private boolean active = true;

    @Setter
    @Column(name = "is_default", nullable = false)
    private boolean defaultPlan;

    @Builder
    private Plan(@NonNull String code, @NonNull String name) {
        this.code = code;
        this.name = name;
    }
}
