package com.shottrack.backend.application.seriesphoto.storage;

/**
 * Falha de E/S no armazenamento de fotos. A mensagem leva só a operação e o NOME DA CLASSE da causa: a causa em si
 * (ex.: AccessDeniedException) cita o caminho do arquivo, que tem o UUID do atleta/série/foto — dado que não pode ir
 * para o log nem para o Sentry. Por isso a causa não é anexada (nem o stack trace dela).
 */
public class StorageException extends RuntimeException {

    public StorageException(String operation, Throwable cause) {
        super("Falha de armazenamento (" + operation + "): " + cause.getClass().getSimpleName());
    }
}
