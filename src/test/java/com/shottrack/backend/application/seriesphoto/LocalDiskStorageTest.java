package com.shottrack.backend.application.seriesphoto;

import com.shottrack.backend.application.seriesphoto.storage.StorageException;
import com.shottrack.backend.application.seriesphoto.storage.StorageProperties;
import com.shottrack.backend.application.seriesphoto.storage.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** ADR-0026: armazenamento em disco — gravar/ler/apagar, path traversal e a exigência do diretório em produção. */
class LocalDiskStorageTest {

    @TempDir
    Path tempDir;

    private StoragePort storage(Path dir, boolean requireExisting) throws Exception {
        Class<?> type = Class.forName("com.shottrack.backend.application.seriesphoto.storage.LocalDiskStorage");
        Constructor<?> constructor = type.getDeclaredConstructor(StorageProperties.class);
        constructor.setAccessible(true);
        try {
            return (StoragePort) constructor.newInstance(new StorageProperties(dir.toString(), requireExisting, new StorageProperties.OrphanCleanup(false, false, 20, 10)));
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void shouldPutGetAndDeleteAFile() throws Exception {
        StoragePort storage = storage(tempDir, false);
        storage.put("athletes/a/series/s/p.jpg", new byte[]{1, 2, 3});

        assertThat(storage.get("athletes/a/series/s/p.jpg")).contains(new byte[]{1, 2, 3});
        assertThat(storage.list()).extracting("key").containsExactly("athletes/a/series/s/p.jpg");

        storage.delete("athletes/a/series/s/p.jpg");
        storage.delete("athletes/a/series/s/p.jpg"); // idempotente

        assertThat(storage.get("athletes/a/series/s/p.jpg")).isEmpty();
        assertThat(storage.list()).isEmpty();
    }

    @Test
    void shouldNotLeaveTemporaryFilesAfterWriting() throws Exception {
        StoragePort storage = storage(tempDir, false);
        storage.put("x/y.jpg", new byte[]{9});

        try (Stream<Path> files = Files.walk(tempDir)) {
            assertThat(files.filter(Files::isRegularFile).map(p -> p.getFileName().toString()))
                    .containsExactlyInAnyOrder("y.jpg", ".shottrack-storage");
        }
    }

    @Test
    void shouldRefuseKeysThatEscapeTheBaseDirectory() throws Exception {
        Path base = Files.createDirectory(tempDir.resolve("uploads"));
        StoragePort storage = storage(base, false);
        Path outside = tempDir.resolve("fora.txt");
        Files.writeString(outside, "segredo");

        for (String key : new String[]{"../fora.txt", "a/../../fora.txt", "/etc/passwd", "..", ".", "", "a\0b"}) {
            assertThatThrownBy(() -> storage.put(key, new byte[]{1})).as("put %s", key).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> storage.get(key)).as("get %s", key).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> storage.delete(key)).as("delete %s", key).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(Files.readString(outside)).isEqualTo("segredo");
    }

    @Test
    void shouldCreateTheDirectoryWhenNotRequiredToExist() throws Exception {
        Path novo = tempDir.resolve("a/b/uploads");

        storage(novo, false);

        assertThat(novo).isDirectory();
    }

    @Test
    void shouldFailAtStartupWhenTheDirectoryIsRequiredButMissing() {
        Path ausente = tempDir.resolve("nao-existe");

        assertThatThrownBy(() -> storage(ausente, true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_STORAGE_LOCAL_DIR");
        assertThat(ausente).doesNotExist();
    }

    @Test
    void shouldFailAtStartupWhenTheDirectoryIsNotWritable() throws Exception {
        Path readOnly = Files.createDirectory(tempDir.resolve("somente-leitura"));
        assertThat(readOnly.toFile().setWritable(false)).isTrue();
        try {
            if (Files.isWritable(readOnly)) {
                return; // rodando como root: o sistema ignora a permissão, nada a provar
            }
            assertThatThrownBy(() -> storage(readOnly, true)).isInstanceOf(IllegalStateException.class).hasMessageContaining("gravável");
        } finally {
            readOnly.toFile().setWritable(true);
        }
    }

    @Test
    void shouldReturnEmptyForAMissingFile() throws Exception {
        assertThat(storage(tempDir, false).get("nao/existe.jpg")).isEqualTo(Optional.empty());
    }

    // ---- marcador .shottrack-storage (rotina de órfãos) ----

    @Test
    void shouldWriteTheMarkerWhenTheDirectoryIsEmptyOrMissing() throws Exception {
        Path vazio = Files.createDirectory(tempDir.resolve("vazio"));
        Path ausente = tempDir.resolve("novo/uploads");

        assertThat(storage(vazio, false).hasOwnershipMarker()).isTrue();
        assertThat(storage(ausente, false).hasOwnershipMarker()).isTrue();
        assertThat(vazio.resolve(".shottrack-storage")).isRegularFile();
    }

    @Test
    void shouldNotWriteTheMarkerInANonEmptyDirectoryThatDoesNotHaveIt() throws Exception {
        Path alheio = Files.createDirectory(tempDir.resolve("alheio"));
        Files.writeString(alheio.resolve("de-outro-uso.txt"), "x");

        StoragePort storage = storage(alheio, false);

        assertThat(storage.hasOwnershipMarker()).isFalse();
        assertThat(alheio.resolve(".shottrack-storage")).doesNotExist();
        assertThat(alheio.resolve("de-outro-uso.txt")).exists();
    }

    @Test
    void shouldKeepAnExistingMarkerAndStartAgainWithoutError() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("reinicio"));
        storage(dir, false).put("athletes/x/y.jpg", new byte[]{1});
        String marker = Files.readString(dir.resolve(".shottrack-storage"));

        StoragePort again = storage(dir, true);

        assertThat(again.hasOwnershipMarker()).isTrue();
        assertThat(Files.readString(dir.resolve(".shottrack-storage"))).isEqualTo(marker);
    }

    // ---- falha de E/S: só operação e classe da causa, sem caminho ----

    @Test
    void shouldReportStorageFailuresWithOnlyTheOperationAndTheCauseClass() throws Exception {
        StoragePort storage = storage(tempDir, false);
        Files.writeString(tempDir.resolve("arquivo"), "x");

        assertThatThrownBy(() -> storage.put("arquivo/dentro.jpg", new byte[]{1}))
                .isInstanceOf(StorageException.class)
                .hasMessage("Falha de armazenamento (gravar): FileAlreadyExistsException")
                .hasNoCause();
    }
}
