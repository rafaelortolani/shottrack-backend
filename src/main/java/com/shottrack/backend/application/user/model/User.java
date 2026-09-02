package com.shottrack.backend.application.user.model;

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
@Table(name = "users")
public class User extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private final String name;

    @Column(nullable = false, unique = true)
    private final String email;

    @Column(name = "password_hash", nullable = false)
    private final String passwordHash;
}
