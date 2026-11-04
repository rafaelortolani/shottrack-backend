package com.shottrack.backend.application.seriesphoto.usecase;

import com.shottrack.backend.application.seriesphoto.PhotoProperties;
import com.shottrack.backend.application.seriesphoto.gateway.SeriesPhotoGateway;
import com.shottrack.backend.application.seriesphoto.storage.StorageException;
import com.shottrack.backend.application.seriesphoto.storage.StoragePort;
import com.shottrack.backend.application.seriesphoto.storage.StorageProperties;
import com.shottrack.backend.application.seriesphoto.storage.StoredObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ADR-0026: remove do armazenamento os arquivos de foto sem linha no banco (falha ao apagar após um commit, falha entre
 * gravar o arquivo e a linha). Salvaguardas, porque apagar arquivo errado é irreversível:
 * <ul>
 *   <li>só considera caminhos que batem EXATAMENTE com o formato de chave gerado pelo sistema
 *       (athletes/{uuid}/series/{uuid}/{uuid}.jpg e .thumb.jpg); qualquer outro arquivo ou pasta é ignorado;</li>
 *   <li>recusa rodar se o diretório não tem o marcador `.shottrack-storage` (diretório que não é nosso);</li>
 *   <li>aborta, sem apagar nada, se achar candidatos demais (mais de max(piso, % dos arquivos de foto));</li>
 *   <li>nunca toca em arquivo com menos de `app.photos.orphan-min-age` (60 min): pode ser upload em andamento;</li>
 *   <li>`dry-run` só registra quantos apagaria.</li>
 * </ul>
 * O log traz só contagens: caminhos têm o UUID do atleta/série/foto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanPhotoCleanupService {

    private static final String UUID_RE = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final Pattern PHOTO_KEY = Pattern.compile(
            "^athletes/" + UUID_RE + "/series/" + UUID_RE + "/" + UUID_RE + "(\\.thumb)?\\.jpg$");

    private final StoragePort storage;
    private final SeriesPhotoGateway photoGateway;
    private final PhotoProperties properties;
    private final StorageProperties storageProperties;
    private final Clock clock;

    /** @return quantos arquivos foram apagados (0 na simulação, na recusa e no aborto) */
    public int cleanup() {
        if (!storage.hasOwnershipMarker()) {
            log.error("Limpeza de fotos órfãs RECUSADA: o diretório de armazenamento não tem o marcador "
                    + "'.shottrack-storage' (não foi criado por esta aplicação). Confira app.storage.local-dir; se o diretório "
                    + "for mesmo das fotos, crie o arquivo marcador à mão.");
            return 0;
        }
        List<StoredObject> photoFiles;
        try {
            photoFiles = storage.list().stream().filter(o -> PHOTO_KEY.matcher(o.key()).matches()).toList();
        } catch (StorageException e) {
            log.warn("Limpeza de fotos órfãs não executada. {}", e.getMessage());
            return 0;
        }

        Instant limit = clock.instant().minus(properties.orphanMinAge());
        List<String> candidates = photoFiles.stream()
                .filter(o -> !o.lastModified().isAfter(limit))
                .filter(o -> !photoGateway.existsByAnyKey(o.key()))
                .map(StoredObject::key)
                .toList();

        StorageProperties.OrphanCleanup settings = storageProperties.orphanCleanup();
        long maxCandidates = Math.max(settings.abortMinCandidates(), (long) photoFiles.size() * settings.abortPercent() / 100);
        if (candidates.size() > maxCandidates) {
            log.error("Limpeza de fotos órfãs ABORTADA, nada foi apagado: {} candidato(s) a órfão de {} arquivo(s) de foto "
                    + "(limite {}). Pode ser diretório errado ou banco inconsistente; confira antes de rodar de novo.",
                    candidates.size(), photoFiles.size(), maxCandidates);
            return 0;
        }
        if (settings.dryRun()) {
            log.info("Limpeza de fotos órfãs (simulação): {} arquivo(s) seriam removidos de {} arquivo(s) de foto",
                    candidates.size(), photoFiles.size());
            return 0;
        }

        int removed = 0;
        for (String key : candidates) {
            try {
                storage.delete(key);
                removed++;
            } catch (StorageException e) {
                log.warn("Limpeza de fotos órfãs. {}", e.getMessage());
            }
        }
        log.info("Limpeza de fotos órfãs: {} arquivo(s) removido(s)", removed);
        return removed;
    }
}
