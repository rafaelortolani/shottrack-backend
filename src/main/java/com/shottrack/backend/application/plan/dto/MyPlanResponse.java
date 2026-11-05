package com.shottrack.backend.application.plan.dto;

/** UC66: o plano do próprio atleta, seus limites efetivos e o uso atual. */
public record MyPlanResponse(
        PlanRef plan,
        Limits limits,
        Usage usage
) {

    public record PlanRef(String code, String name) {
    }

    public record Limits(long photosPerSeries, long photosPerAthlete, long storageBytesPerAthlete, long uploadsPerHour) {
    }

    public record Usage(long photos, long storageBytes) {
    }
}
