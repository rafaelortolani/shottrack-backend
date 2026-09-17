package com.shottrack.backend.application.traininglocation.usecase;

import com.shottrack.backend.application.traininglocation.dto.TrainingLocationRegisterRequest;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationResponse;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationUpdateRequest;
import com.shottrack.backend.application.traininglocation.gateway.TrainingLocationGateway;
import com.shottrack.backend.application.traininglocation.mapper.TrainingLocationMapper;
import com.shottrack.backend.application.traininglocation.model.TrainingLocation;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainingLocationService {

    private final TrainingLocationGateway trainingLocationGateway;
    private final TrainingLocationMapper trainingLocationMapper;

    public TrainingLocationResponse register(UUID userId, TrainingLocationRegisterRequest request) {
        TrainingLocation trainingLocation = TrainingLocation.builder()
                .userId(userId)
                .name(request.name())
                .city(request.city())
                .state(request.state())
                .build();

        TrainingLocation saved = trainingLocationGateway.save(trainingLocation);
        return trainingLocationMapper.toResponse(saved);
    }

    public List<TrainingLocationResponse> listByUser(UUID userId) {
        return trainingLocationGateway.findAllByUserId(userId).stream()
                .map(trainingLocationMapper::toResponse)
                .toList();
    }

    /**
     * UC26: edição parcial — só os campos enviados (não nulos) são
     * atualizados. Qualquer um dos três, se enviado em branco, rejeita antes
     * de tocar a entidade (mesmo padrão de Acessório, UC20).
     */
    public TrainingLocationResponse update(UUID userId, UUID trainingLocationId, TrainingLocationUpdateRequest request) {
        TrainingLocation trainingLocation = findOwnedTrainingLocationOrThrow(userId, trainingLocationId);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BusinessException("TRAINING_LOCATION_FIELD_REQUIRED", HttpStatus.BAD_REQUEST);
            }
            trainingLocation.setName(request.name());
        }
        if (request.city() != null) {
            if (request.city().isBlank()) {
                throw new BusinessException("TRAINING_LOCATION_FIELD_REQUIRED", HttpStatus.BAD_REQUEST);
            }
            trainingLocation.setCity(request.city());
        }
        if (request.state() != null) {
            if (request.state().isBlank()) {
                throw new BusinessException("TRAINING_LOCATION_FIELD_REQUIRED", HttpStatus.BAD_REQUEST);
            }
            trainingLocation.setState(request.state());
        }

        TrainingLocation saved = trainingLocationGateway.save(trainingLocation);
        return trainingLocationMapper.toResponse(saved);
    }

    /**
     * UC27/ADR-0006: bloqueia a exclusão (nunca arquiva) se o local já foi
     * usado em alguma visita.
     */
    public void delete(UUID userId, UUID trainingLocationId) {
        TrainingLocation trainingLocation = findOwnedTrainingLocationOrThrow(userId, trainingLocationId);

        if (isUsedInAnyVisit(trainingLocation)) {
            throw new BusinessException("TRAINING_LOCATION_IN_USE", HttpStatus.CONFLICT);
        }

        trainingLocationGateway.delete(trainingLocation);
    }

    /**
     * O domínio de Visita ainda não existe, então nenhum local pode estar
     * "em uso" — sempre retorna false até lá. Quando Visita existir,
     * troca-se este método pela consulta real, sem reescrever o restante de
     * delete().
     */
    private boolean isUsedInAnyVisit(TrainingLocation trainingLocation) {
        return false;
    }

    private TrainingLocation findOwnedTrainingLocationOrThrow(UUID userId, UUID trainingLocationId) {
        return trainingLocationGateway.findById(trainingLocationId)
                .filter(location -> location.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException("TRAINING_LOCATION_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
