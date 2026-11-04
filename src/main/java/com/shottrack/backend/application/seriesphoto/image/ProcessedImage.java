package com.shottrack.backend.application.seriesphoto.image;

/** Resultado do UC60: JPEG sem metadados (até 2000 px) e miniatura (até 400 px). */
public record ProcessedImage(byte[] image, int widthPx, int heightPx, byte[] thumbnail) {
}
