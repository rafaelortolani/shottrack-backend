package com.shottrack.backend.application.seriesphoto;

import com.shottrack.backend.application.seriesphoto.dto.SeriesPhotoResponse;
import com.shottrack.backend.application.seriesphoto.usecase.SeriesPhotoService;
import com.shottrack.backend.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** UC60/UC61/UC62 (ADR-0026). Nenhuma rota é pública: a imagem só sai por aqui, autenticada. */
@RestController
@RequestMapping("/api/series/{seriesId}/photos")
@RequiredArgsConstructor
public class SeriesPhotoController {

    private static final MediaType JPEG = MediaType.IMAGE_JPEG;

    private final SeriesPhotoService photoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SeriesPhotoResponse>> attach(Authentication authentication,
                                                                   @PathVariable UUID seriesId,
                                                                   @RequestPart("file") MultipartFile file) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(photoService.attach(userId, seriesId, file)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SeriesPhotoResponse>>> list(Authentication authentication,
                                                                       @PathVariable UUID seriesId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(photoService.list(userId, seriesId)));
    }

    @GetMapping("/{photoId}/content")
    public ResponseEntity<byte[]> content(Authentication authentication, @PathVariable UUID seriesId,
                                          @PathVariable UUID photoId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return image(photoService.content(userId, seriesId, photoId, false));
    }

    @GetMapping("/{photoId}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(Authentication authentication, @PathVariable UUID seriesId,
                                            @PathVariable UUID photoId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return image(photoService.content(userId, seriesId, photoId, true));
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID seriesId,
                                       @PathVariable UUID photoId) {
        UUID userId = (UUID) authentication.getPrincipal();
        photoService.delete(userId, seriesId, photoId);
        return ResponseEntity.noContent().build();
    }

    /** Content-Type fixo, sem sniffing e sem cache compartilhado: a foto é dado pessoal. */
    private static ResponseEntity<byte[]> image(byte[] bytes) {
        return ResponseEntity.ok()
                .contentType(JPEG)
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().cachePrivate().getHeaderValue())
                .body(bytes);
    }
}
