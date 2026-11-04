package com.shottrack.backend.application.seriesphoto;

import com.shottrack.backend.application.seriesphoto.storage.StoragePort;
import com.shottrack.backend.application.seriesphoto.storage.StorageProperties;
import com.shottrack.backend.application.seriesphoto.usecase.OrphanPhotoCleanupScheduler;
import com.shottrack.backend.application.seriesphoto.usecase.OrphanPhotoCleanupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-0026: a rotina semanal contra o disco de verdade e o banco: remove órfão antigo no formato de chave do sistema,
 * mas nunca toca em arquivo recente, em arquivo com linha, em arquivo fora do formato nem em diretório sem o marcador.
 * Diretório próprio: não mistura com os outros testes. A rotina fica DESLIGADA por padrão (só o perfil prod liga).
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "app.storage.local-dir=${java.io.tmpdir}/shottrack-uploads-orphans-test-v2")
class OrphanPhotoCleanupTest {

    @Autowired
    private StoragePort storage;
    @Autowired
    private StorageProperties storageProperties;
    @Autowired
    private OrphanPhotoCleanupService cleanup;
    @Autowired
    private ApplicationContext context;

    private final List<String> created = new ArrayList<>();

    @AfterEach
    void removeLeftovers() throws IOException {
        created.forEach(storage::delete);
        created.clear();
        Path marker = baseDir().resolve(".shottrack-storage");
        if (!Files.exists(marker)) {
            Files.writeString(marker, "restaurado pelo teste\n");
        }
    }

    private Path baseDir() {
        return Path.of(storageProperties.localDir()).toAbsolutePath().normalize();
    }

    private static String photoKey(String suffix) {
        return "athletes/" + UUID.randomUUID() + "/series/" + UUID.randomUUID() + "/" + UUID.randomUUID() + suffix;
    }

    private String file(String key, long ageMinutes) {
        storage.put(key, new byte[]{1, 2, 3});
        created.add(key);
        assertThat(new File(baseDir().resolve(key).toString())
                .setLastModified(Instant.now().minusSeconds(ageMinutes * 60).toEpochMilli())).isTrue();
        return key;
    }

    @Test
    void shouldRemoveOldFilesWithoutARowAndKeepRecentOnes(CapturedOutput output) {
        String old = file(photoKey(".jpg"), 61 * 24);
        String justOverTheLimit = file(photoKey(".thumb.jpg"), 61);
        String recent = file(photoKey(".jpg"), 59);
        String brandNew = file(photoKey(".jpg"), 0);

        int removed = cleanup.cleanup();

        assertThat(removed).isEqualTo(2);
        assertThat(storage.get(old)).isEmpty();
        assertThat(storage.get(justOverTheLimit)).isEmpty();
        assertThat(storage.get(recent)).as("menos de 60 minutos: pode ser upload em andamento").isPresent();
        assertThat(storage.get(brandNew)).isPresent();
        assertThat(output.getAll()).contains("Limpeza de fotos órfãs: 2 arquivo(s) removido(s)");
    }

    @Test
    void shouldNeverDeleteAFileOutsideTheSystemKeyPatternEvenWhenOld() {
        String foreign = file("outro-uso/relatorio.pdf", 60 * 24 * 30);
        String wrongName = file("athletes/" + UUID.randomUUID() + "/foto.jpg", 60 * 24 * 30);
        String png = file(photoKey(".png"), 60 * 24 * 30);
        String orphan = file(photoKey(".jpg"), 60 * 24 * 30);

        int removed = cleanup.cleanup();

        assertThat(removed).isEqualTo(1);
        assertThat(storage.get(foreign)).isPresent();
        assertThat(storage.get(wrongName)).isPresent();
        assertThat(storage.get(png)).isPresent();
        assertThat(storage.get(orphan)).isEmpty();
        assertThat(baseDir().resolve(".shottrack-storage")).as("o marcador nunca é apagado").exists();
    }

    @Test
    void shouldRefuseToRunWhenTheDirectoryHasNoMarker(CapturedOutput output) throws IOException {
        String orphan = file(photoKey(".jpg"), 60 * 24 * 30);
        Files.delete(baseDir().resolve(".shottrack-storage"));

        int removed = cleanup.cleanup();

        assertThat(removed).isZero();
        assertThat(storage.get(orphan)).as("sem marcador nada é apagado").isPresent();
        assertThat(output.getAll()).contains("Limpeza de fotos órfãs RECUSADA");
    }

    @Test
    void shouldRemoveNothingWhenThereAreNoOrphans(CapturedOutput output) {
        assertThat(cleanup.cleanup()).isZero();
        assertThat(output.getAll()).contains("Limpeza de fotos órfãs: 0 arquivo(s) removido(s)");
    }

    @Test
    void shouldHaveTheScheduledJobDisabledByDefault() {
        assertThat(storageProperties.orphanCleanup().enabled()).isFalse();
        assertThat(storageProperties.orphanCleanup().dryRun()).isFalse();
        assertThat(context.getBeansOfType(OrphanPhotoCleanupScheduler.class)).isEmpty();
    }
}
