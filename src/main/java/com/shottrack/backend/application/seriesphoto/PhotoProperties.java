package com.shottrack.backend.application.seriesphoto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * ADR-0026: proteções do servidor para as fotos do alvo (UC60) e da rotina de arquivos órfãos (UC52/ADR-0019).
 * Os limites de quantidade, de espaço e de envios por hora vêm do plano do atleta (ADR-0027), não daqui.
 */
@ConfigurationProperties("app.photos")
public record PhotoProperties(
        @DefaultValue("10485760") long maxBytes,
        @DefaultValue("40") long maxMegapixels,
        @DefaultValue("2") int maxConcurrentProcessing,
        @DefaultValue("60m") Duration orphanMinAge
) {
}
