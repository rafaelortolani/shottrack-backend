package com.shottrack.backend.application.seriesphoto.storage;

import java.util.List;
import java.util.Optional;

/**
 * ADR-0026: porta de armazenamento dos arquivos de foto (padrão Gateway do projeto). A implementação inicial é o
 * disco local; uma de Object Storage (S3) entra depois sem mudar o restante. As chaves são geradas pelo sistema.
 */
public interface StoragePort {

    void put(String key, byte[] content);

    Optional<byte[]> get(String key);

    /** Idempotente: apagar o que não existe não é erro. */
    void delete(String key);

    /** Todos os arquivos guardados (para a rotina de órfãos). */
    List<StoredObject> list();

    /**
     * O armazenamento foi criado por esta aplicação (arquivo marcador `.shottrack-storage`, gravado na subida quando o
     * diretório estava vazio ou ausente). Sem ele, a rotina de órfãos não apaga nada: o diretório pode ser de outro uso.
     */
    boolean hasOwnershipMarker();
}
