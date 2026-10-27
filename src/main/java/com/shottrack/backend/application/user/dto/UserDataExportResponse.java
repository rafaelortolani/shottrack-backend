package com.shottrack.backend.application.user.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * UC53/ADR-0019: tudo o que o sistema guarda sobre o atleta, em nomes legíveis
 * (sem ids internos, sem hash de senha, sem tokens). As referências entre
 * registros (ex: série → arma) vão embutidas, não por id.
 */
public record UserDataExportResponse(
        Instant exportedAt,
        ProfileExport profile,
        List<PracticedModalityExport> practicedModalities,
        List<WeaponExport> weapons,
        List<AmmunitionExport> ammunitions,
        List<AccessoryExport> accessories,
        List<TrainingLocationExport> trainingLocations,
        List<VisitExport> visits
) {

    public record ProfileExport(
            String name,
            String email,
            String experienceLevel,
            Instant registeredAt,
            Instant termsAcceptedAt,
            String termsVersion
    ) {
    }

    public record PracticedModalityExport(String modality, List<String> resultTypes) {
    }

    public record WeaponExport(String type, String brand, String model, String caliber, String nickname) {
    }

    public record AmmunitionExport(
            String manufacturer,
            String caliber,
            String nickname,
            BigDecimal projectileWeightGrains,
            BigDecimal powderCharge,
            String projectileType,
            String lot,
            String notes
    ) {
    }

    public record AccessoryExport(String name, String type, String notes, List<WeaponExport> weapons) {
    }

    public record TrainingLocationExport(String name, String city, String state) {
    }

    public record VisitExport(
            String trainingLocation,
            Instant startedAt,
            Instant endedAt,
            String status,
            String observations,
            List<TrainingExport> trainings
    ) {
    }

    public record TrainingExport(
            String modality,
            Instant startedAt,
            Instant endedAt,
            String status,
            List<SeriesExport> series
    ) {
    }

    public record SeriesExport(
            WeaponExport weapon,
            AmmunitionExport ammunition,
            BigDecimal distanceMeters,
            String target,
            Integer shotCount,
            String notes,
            List<ResultExport> results
    ) {
    }

    public record ResultExport(String resultType, String value, boolean notApplicable) {
    }
}
