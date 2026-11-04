package com.shottrack.backend.application.seriesphoto;

import com.shottrack.backend.application.seriesphoto.storage.StoragePort;
import com.shottrack.backend.application.seriesphoto.usecase.OrphanPhotoCleanupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-0026: a rotina semanal remove arquivo sem linha no banco, mas nunca toca em arquivo com menos de 60 minutos
 * (pode ser um upload em andamento) nem em arquivo que tem linha. Diretório próprio: não mistura com os outros testes.
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "app.storage.local-dir=${java.io.tmpdir}/shottrack-uploads-orphans-test")
class OrphanPhotoCleanupTest {

    @Autowired
    private StoragePort storage;
    @Autowired
    private OrphanPhotoCleanupService cleanup;

    private final java.util.List<String> created = new java.util.ArrayList<>();

    @AfterEach
    void removeLeftovers() {
        created.forEach(storage::delete);
    }

    private String file(String label, long ageMinutes) {
        String key = "athletes/" + UUID.randomUUID() + "/series/" + UUID.randomUUID() + "/" + label + ".jpg";
        storage.put(key, new byte[]{1, 2, 3});
        created.add(key);
        Path path = Path.of(System.getProperty("java.io.tmpdir"), "shottrack-uploads-orphans-test", key);
        assertThat(new File(path.toString()).setLastModified(Instant.now().minusSeconds(ageMinutes * 60).toEpochMilli())).isTrue();
        return key;
    }

    @Test
    void shouldRemoveOldFilesWithoutARowAndKeepRecentOnes(CapturedOutput output) {
        String old = file("antigo", 61 * 24);
        String justOverTheLimit = file("quase", 61);
        String recent = file("recente", 59);
        String brandNew = file("novo", 0);

        int removed = cleanup.cleanup();

        assertThat(removed).isEqualTo(2);
        assertThat(storage.get(old)).isEmpty();
        assertThat(storage.get(justOverTheLimit)).isEmpty();
        assertThat(storage.get(recent)).as("menos de 60 minutos: pode ser upload em andamento").isPresent();
        assertThat(storage.get(brandNew)).isPresent();
        assertThat(output.getAll()).contains("Limpeza de fotos órfãs: 2 arquivo(s) removido(s)");
    }

    @Test
    void shouldRemoveNothingWhenThereAreNoOrphans(CapturedOutput output) {
        assertThat(cleanup.cleanup()).isZero();
        assertThat(output.getAll()).contains("Limpeza de fotos órfãs: 0 arquivo(s) removido(s)");
    }
}
