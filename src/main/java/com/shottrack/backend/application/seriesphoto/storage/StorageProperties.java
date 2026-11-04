package com.shottrack.backend.application.seriesphoto.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * ADR-0026: onde ficam os arquivos. `requireExistingDir` (ligado no perfil prod) faz o app falhar na subida se o
 * diretório não existir ou não for gravável pelo usuário do contêiner, em vez de falhar no primeiro upload.
 */
@ConfigurationProperties("app.storage")
public record StorageProperties(
        @DefaultValue("./data/uploads") String localDir,
        @DefaultValue("false") boolean requireExistingDir
) {
}
