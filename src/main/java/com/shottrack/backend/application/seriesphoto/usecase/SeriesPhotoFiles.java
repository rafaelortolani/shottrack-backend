package com.shottrack.backend.application.seriesphoto.usecase;

import com.shottrack.backend.application.seriesphoto.gateway.SeriesPhotoGateway;
import com.shottrack.backend.application.seriesphoto.model.SeriesPhoto;
import com.shottrack.backend.application.seriesphoto.storage.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * ADR-0026: apagar série, treino, visita ou conta remove as linhas das fotos (cascata) e, DEPOIS do commit, os
 * arquivos. Quem exclui coleta as chaves antes de apagar (`keysOf...`) e entrega a lista a `deleteAfterCommit`.
 * Falha ao apagar um arquivo só vira aviso (sem dado pessoal): a rotina semanal de órfãos limpa o que sobrar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeriesPhotoFiles {

    private final SeriesPhotoGateway photoGateway;
    private final StoragePort storage;

    public List<String> keysOfSeries(UUID seriesId) {
        return keysOf(photoGateway.findAllBySeriesId(seriesId));
    }

    public List<String> keysOfTraining(UUID trainingId) {
        return keysOf(photoGateway.findAllByTrainingId(trainingId));
    }

    public List<String> keysOfVisit(UUID visitId) {
        return keysOf(photoGateway.findAllByVisitId(visitId));
    }

    public List<String> keysOfAthlete(UUID athleteId) {
        return keysOf(photoGateway.findAllByAthleteId(athleteId));
    }

    public void deleteAfterCommit(Collection<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        List<String> toDelete = List.copyOf(keys);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteQuietly(toDelete);
                }
            });
        } else {
            deleteQuietly(toDelete);
        }
    }

    /** Também usado quando o registro não chegou a ser gravado (UC60): apaga já, sem esperar commit. */
    public void deleteQuietly(Collection<String> keys) {
        for (String key : keys) {
            try {
                storage.delete(key);
            } catch (RuntimeException e) {
                log.warn("Falha ao apagar arquivo de foto ({})", e.getClass().getSimpleName());
            }
        }
    }

    private static List<String> keysOf(List<SeriesPhoto> photos) {
        List<String> keys = new ArrayList<>();
        for (SeriesPhoto photo : photos) {
            keys.add(photo.getStorageKey());
            keys.add(photo.getThumbKey());
        }
        return keys;
    }
}
