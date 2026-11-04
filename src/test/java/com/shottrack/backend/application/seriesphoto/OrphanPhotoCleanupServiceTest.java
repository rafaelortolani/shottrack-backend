package com.shottrack.backend.application.seriesphoto;

import com.shottrack.backend.application.seriesphoto.gateway.SeriesPhotoGateway;
import com.shottrack.backend.application.seriesphoto.storage.StorageException;
import com.shottrack.backend.application.seriesphoto.storage.StoragePort;
import com.shottrack.backend.application.seriesphoto.storage.StorageProperties;
import com.shottrack.backend.application.seriesphoto.storage.StoredObject;
import com.shottrack.backend.application.seriesphoto.usecase.OrphanPhotoCleanupScheduler;
import com.shottrack.backend.application.seriesphoto.usecase.OrphanPhotoCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

/**
 * ADR-0026: salvaguardas da rotina de órfãos, com armazenamento e banco simulados (o disco de verdade está em
 * {@link OrphanPhotoCleanupTest}). Apagar o arquivo errado é irreversível, então cada trava tem seu teste.
 */
@ExtendWith(OutputCaptureExtension.class)
class OrphanPhotoCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");

    private final StoragePort storage = mock(StoragePort.class);
    private final SeriesPhotoGateway gateway = mock(SeriesPhotoGateway.class);
    private final List<StoredObject> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(storage.hasOwnershipMarker()).thenReturn(true);
        when(storage.list()).thenAnswer(invocation -> List.copyOf(stored));
        when(gateway.existsByAnyKey(anyString())).thenReturn(false);
    }

    private OrphanPhotoCleanupService service(boolean dryRun, int floor, int percent) {
        return new OrphanPhotoCleanupService(storage, gateway,
                new PhotoProperties(5, 500, 10485760, 40, 2, Duration.ofMinutes(60)),
                new StorageProperties("x", false, new StorageProperties.OrphanCleanup(true, dryRun, floor, percent)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private OrphanPhotoCleanupService service() {
        return service(false, 20, 10);
    }

    private static String photoKey() {
        return "athletes/" + UUID.randomUUID() + "/series/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".jpg";
    }

    private String add(String key, long ageMinutes) {
        stored.add(new StoredObject(key, NOW.minus(Duration.ofMinutes(ageMinutes))));
        return key;
    }

    private List<String> addOrphans(int count, long ageMinutes) {
        return IntStream.range(0, count).mapToObj(i -> add(photoKey(), ageMinutes)).toList();
    }

    @Test
    void shouldIgnoreEveryFileOutsideTheSystemKeyPattern() {
        String uuid = UUID.randomUUID().toString();
        List.of(".shottrack-storage", "lixo.txt", "foto.jpg", "athletes/foto.jpg", "athletes/x/series/y/z.jpg",
                        "athletes/" + uuid + "/series/" + uuid + "/" + uuid + ".png",
                        "athletes/" + uuid + "/series/" + uuid + "/" + uuid + ".jpg.bak",
                        "athletes/" + uuid + "/series/" + uuid + "/" + uuid + ".JPG",
                        "athletes/" + uuid.toUpperCase() + "/series/" + uuid + "/" + uuid + ".jpg",
                        "athletes/" + uuid + "/series/" + uuid + "/extra/" + uuid + ".jpg",
                        "../athletes/" + uuid + "/series/" + uuid + "/" + uuid + ".jpg",
                        "outra-pasta/athletes/" + uuid + "/series/" + uuid + "/" + uuid + ".jpg",
                        "athletes/" + uuid + "/series/" + uuid + "/.tmp-123.part")
                .forEach(key -> add(key, 60 * 24 * 90));

        int removed = service().cleanup();

        assertThat(removed).isZero();
        verify(storage, never()).delete(anyString());
        verify(gateway, never()).existsByAnyKey(anyString());
    }

    @Test
    void shouldRemoveOldOrphansInThePatternIncludingThumbnails() {
        String base = "athletes/" + UUID.randomUUID() + "/series/" + UUID.randomUUID() + "/" + UUID.randomUUID();
        String image = add(base + ".jpg", 61);
        String thumb = add(base + ".thumb.jpg", 61 * 24);

        int removed = service().cleanup();

        assertThat(removed).isEqualTo(2);
        verify(storage).delete(image);
        verify(storage).delete(thumb);
    }

    @Test
    void shouldKeepRecentFilesAndFilesThatStillHaveARow() {
        String recent = add(photoKey(), 59);
        String withRow = add(photoKey(), 60 * 24);
        when(gateway.existsByAnyKey(withRow)).thenReturn(true);

        assertThat(service().cleanup()).isZero();

        verify(storage, never()).delete(recent);
        verify(storage, never()).delete(withRow);
    }

    @Test
    void shouldRefuseToRunWithoutTheOwnershipMarker(CapturedOutput output) {
        when(storage.hasOwnershipMarker()).thenReturn(false);
        addOrphans(3, 60 * 24);

        assertThat(service().cleanup()).isZero();

        verify(storage, never()).list();
        verify(storage, never()).delete(anyString());
        assertThat(output.getAll()).contains("Limpeza de fotos órfãs RECUSADA").contains(".shottrack-storage");
    }

    @Test
    void shouldAbortWithoutDeletingAnythingAboveTheMinimumOf20Candidates(CapturedOutput output) {
        addOrphans(21, 60 * 24);

        assertThat(service().cleanup()).isZero();

        verify(storage, never()).delete(anyString());
        assertThat(output.getAll()).contains("Limpeza de fotos órfãs ABORTADA")
                .contains("21 candidato(s) a órfão de 21 arquivo(s) de foto (limite 20)");
    }

    @Test
    void shouldRunWhenTheCandidatesAreExactlyTheLimit() {
        addOrphans(20, 60 * 24);

        assertThat(service().cleanup()).isEqualTo(20);

        verify(storage, times(20)).delete(anyString());
    }

    @Test
    void shouldUseTenPercentOfAllPhotoFilesWhenThatIsAboveTheMinimum(CapturedOutput output) {
        addOrphans(250, 1); // recentes: contam no total, não são candidatos
        addOrphans(30, 60 * 24); // total 280 → limite 28 → 30 candidatos abortam
        assertThat(service().cleanup()).isZero();
        verify(storage, never()).delete(anyString());
        assertThat(output.getAll()).contains("30 candidato(s) a órfão de 280 arquivo(s) de foto (limite 28)");

        stored.clear();
        addOrphans(250, 1);
        addOrphans(27, 60 * 24); // total 277 → limite 27 → 27 candidatos passam (acima do piso de 20)
        assertThat(service().cleanup()).isEqualTo(27);
    }

    @Test
    void shouldOnlyLogWhatItWouldDeleteInDryRun(CapturedOutput output) {
        addOrphans(3, 60 * 24);
        addOrphans(2, 5);

        assertThat(service(true, 20, 10).cleanup()).isZero();

        verify(storage, never()).delete(anyString());
        assertThat(output.getAll()).contains("(simulação): 3 arquivo(s) seriam removidos de 5 arquivo(s) de foto");
    }

    @Test
    void shouldNotFailWhenListingFailsAndShouldLogOnlyTheCauseClass(CapturedOutput output) {
        when(storage.list()).thenThrow(new StorageException("listar", new IOException("/var/dados/athletes/segredo")));

        assertThat(service().cleanup()).isZero();

        assertThat(output.getAll()).contains("Falha de armazenamento (listar): IOException").doesNotContain("/var/dados");
    }

    @Test
    void shouldKeepGoingWhenOneDeletionFailsAndLogOnlyTheCauseClass(CapturedOutput output) {
        List<String> orphans = addOrphans(3, 60 * 24);
        doThrow(new StorageException("apagar", new IOException("/var/dados/" + orphans.get(0)))).when(storage).delete(orphans.get(0));

        assertThat(service().cleanup()).isEqualTo(2);

        verify(storage).delete(orphans.get(1));
        verify(storage).delete(orphans.get(2));
        assertThat(output.getAll()).contains("Falha de armazenamento (apagar): IOException")
                .doesNotContain("/var/dados").doesNotContain(orphans.get(0));
    }

    // ---- desligada por padrão ----

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(OrphanPhotoCleanupScheduler.class)
            .withBean(OrphanPhotoCleanupService.class, () -> mock(OrphanPhotoCleanupService.class));

    @Test
    void shouldNotCreateTheSchedulerUnlessTheCleanupIsExplicitlyEnabled() {
        runner.run(context -> assertThat(context).doesNotHaveBean(OrphanPhotoCleanupScheduler.class));
        runner.withPropertyValues("app.storage.orphan-cleanup.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OrphanPhotoCleanupScheduler.class));
        runner.withPropertyValues("app.storage.orphan-cleanup.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(OrphanPhotoCleanupScheduler.class));
    }
}
