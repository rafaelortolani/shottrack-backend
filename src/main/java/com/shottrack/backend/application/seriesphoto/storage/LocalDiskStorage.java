package com.shottrack.backend.application.seriesphoto.storage;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * ADR-0026: armazenamento em disco. Toda chave é resolvida dentro do diretório base: o caminho final normalizado
 * precisa continuar sob a base (path traversal). A gravação é atômica (arquivo temporário + rename), então uma
 * leitura nunca vê arquivo pela metade.
 */
@Component
class LocalDiskStorage implements StoragePort {

    private static final String TEMP_PREFIX = ".tmp-";

    private final Path baseDir;

    LocalDiskStorage(StorageProperties properties) {
        this.baseDir = Path.of(properties.localDir()).toAbsolutePath().normalize();
        try {
            if (properties.requireExistingDir() && !Files.isDirectory(baseDir)) {
                throw new IllegalStateException("O diretório de fotos '" + baseDir
                        + "' não existe (app.storage.local-dir / APP_STORAGE_LOCAL_DIR) — crie-o e dê permissão de escrita");
            }
            Files.createDirectories(baseDir);
            Path probe = Files.createTempFile(baseDir, TEMP_PREFIX, ".probe");
            Files.delete(probe);
        } catch (IOException e) {
            throw new IllegalStateException("O diretório de fotos '" + baseDir + "' não é gravável por este usuário", e);
        }
    }

    @Override
    public void put(String key, byte[] content) {
        Path target = resolve(key);
        Path temp = null;
        try {
            Files.createDirectories(target.getParent());
            temp = Files.createTempFile(target.getParent(), TEMP_PREFIX, ".part");
            Files.write(temp, content);
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // a rotina de órfãos limpa o temporário
                }
            }
            throw new UncheckedIOException("Falha ao gravar arquivo de foto", e);
        }
    }

    @Override
    public Optional<byte[]> get(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(path));
        } catch (java.nio.file.NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler arquivo de foto", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao apagar arquivo de foto", e);
        }
    }

    @Override
    public List<StoredObject> list() {
        try (Stream<Path> files = Files.walk(baseDir)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> {
                        try {
                            return new StoredObject(baseDir.relativize(path).toString().replace('\\', '/'),
                                    Files.getLastModifiedTime(path).toInstant());
                        } catch (IOException e) {
                            return new StoredObject(baseDir.relativize(path).toString().replace('\\', '/'), Instant.now());
                        }
                    })
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao listar arquivos de foto", e);
        }
    }

    private Path resolve(String key) {
        if (key == null || key.isBlank() || key.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Chave de armazenamento inválida");
        }
        Path path = baseDir.resolve(key).normalize();
        if (!path.startsWith(baseDir) || path.equals(baseDir)) {
            throw new IllegalArgumentException("Chave de armazenamento fora do diretório base");
        }
        return path;
    }
}
